/*******************************************************************************
 * Copyright (c) 2014, 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *     Alexander Nyßen (itemis AG) - refactorings
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.fx.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.gef4.fx.gestures.AbstractMouseDragGesture;
import org.eclipse.gef4.fx.nodes.InfiniteCanvas;
import org.eclipse.gef4.geometry.planar.Dimension;
import org.eclipse.gef4.mvc.fx.domain.FXDomain;
import org.eclipse.gef4.mvc.fx.parts.FXPartUtils;
import org.eclipse.gef4.mvc.fx.policies.IFXOnClickPolicy;
import org.eclipse.gef4.mvc.fx.policies.IFXOnDragPolicy;
import org.eclipse.gef4.mvc.fx.viewer.FXViewer;
import org.eclipse.gef4.mvc.parts.IVisualPart;
import org.eclipse.gef4.mvc.policies.IPolicy;
import org.eclipse.gef4.mvc.tools.AbstractTool;
import org.eclipse.gef4.mvc.tools.ITool;
import org.eclipse.gef4.mvc.viewer.IViewer;

import com.google.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * An {@link ITool} to handle click/drag interaction gestures.
 * <p>
 * As click and drag are 'overlapping' gestures (a click is part of each drag,
 * which is composed out of click, drag, and release), these are handled
 * together here, even while distinct interaction policies will be queried to
 * handle the respective gesture parts.
 * <p>
 * During each click/drag interaction, the tool identifies respective
 * {@link IVisualPart}s that serve as interaction targets for click and drag
 * respectively. They are identified via hit-testing on the visuals and the
 * availability of a corresponding {@link IFXOnClickPolicy} or
 * {@link IFXOnDragPolicy}.
 * <p>
 * The {@link FXClickDragTool} handles the opening and closing of an transaction
 * operation via the {@link FXDomain}, to which it is adapted. It controls that
 * a single transaction operation is used for the complete interaction
 * (including the click and potential drag part), so all interaction results can
 * be undone in a single undo step.
 *
 * @author mwienand
 * @author anyssen
 *
 */
public class FXClickDragTool extends AbstractTool<Node> {

	/**
	 * The typeKey used to retrieve those policies that are able to handle the
	 * click part of the click/drag interaction gesture.
	 */
	public static final Class<IFXOnClickPolicy> ON_CLICK_POLICY_KEY = IFXOnClickPolicy.class;

	/**
	 * The typeKey used to retrieve those policies that are able to handle the
	 * drag part of the click/drag interaction gesture.
	 */
	public static final Class<IFXOnDragPolicy> ON_DRAG_POLICY_KEY = IFXOnDragPolicy.class;

	@Inject
	private ITargetPolicyResolver targetPolicyResolver;

	private final Map<Scene, AbstractMouseDragGesture> gestures = new HashMap<>();
	private final Map<IViewer<Node>, ChangeListener<Boolean>> viewerFocusChangeListeners = new HashMap<>();
	private final Map<Scene, EventHandler<MouseEvent>> cursorMouseMoveFilters = new HashMap<>();
	private final Map<Scene, EventHandler<KeyEvent>> cursorKeyFilters = new HashMap<>();

	private IViewer<Node> activeViewer;

	@SuppressWarnings("unchecked")
	@Override
	public List<IFXOnDragPolicy> getActivePolicies(IViewer<Node> viewer) {
		return (List<IFXOnDragPolicy>) super.getActivePolicies(viewer);
	}

	@Override
	protected void registerListeners() {
		super.registerListeners();
		for (final IViewer<Node> viewer : getDomain().getViewers().values()) {
			// register a viewer focus change listener
			ChangeListener<Boolean> viewerFocusChangeListener = new ChangeListener<Boolean>() {
				@Override
				public void changed(
						ObservableValue<? extends Boolean> observable,
						Boolean oldValue, Boolean newValue) {
					// cannot abort if no activeViewer
					if (activeViewer == null) {
						return;
					}
					// check if any viewer is focused
					for (IViewer<Node> v : getDomain().getViewers().values()) {
						if (v.isViewerFocused()) {
							return;
						}
					}
					// no viewer is focused => abort
					// cancel target policies
					for (IPolicy<Node> policy : getActivePolicies(
							activeViewer)) {
						if (policy instanceof IFXOnDragPolicy) {
							((IFXOnDragPolicy) policy).dragAborted();
						}
					}
					// clear active policies
					clearActivePolicies(activeViewer);
					activeViewer = null;
					// close execution transaction
					getDomain().closeExecutionTransaction(FXClickDragTool.this);
				}
			};
			viewer.viewerFocusedProperty()
					.addListener(viewerFocusChangeListener);
			viewerFocusChangeListeners.put(viewer, viewerFocusChangeListener);

			Scene scene = ((FXViewer) viewer).getScene();
			if (gestures.containsKey(scene)) {
				// already registered for this scene
				continue;
			}

			final IFXOnDragPolicy indicationCursorPolicy[] = new IFXOnDragPolicy[] {
					null };
			@SuppressWarnings("unchecked")
			final List<IFXOnDragPolicy> possibleDragPolicies[] = new ArrayList[] {
					null };

			// register mouse move filter for forwarding events to drag policies
			// that can show a mouse cursor to indicate their action
			final EventHandler<MouseEvent> indicationCursorMouseMoveFilter = new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if (indicationCursorPolicy[0] != null) {
						indicationCursorPolicy[0].hideIndicationCursor();
						indicationCursorPolicy[0] = null;
					}

					EventTarget eventTarget = event.getTarget();
					if (eventTarget instanceof Node) {
						// determine all drag policies that can be
						// notified about events
						Node target = (Node) eventTarget;
						possibleDragPolicies[0] = new ArrayList<>(
								targetPolicyResolver.getTargetPolicies(
										FXClickDragTool.this, target,
										ON_DRAG_POLICY_KEY));

						// search drag policies in reverse order first,
						// so that the policy closest to the target part
						// is the first policy to provide an indication
						// cursor
						ListIterator<? extends IFXOnDragPolicy> dragIterator = possibleDragPolicies[0]
								.listIterator(possibleDragPolicies[0].size());
						while (dragIterator.hasPrevious()) {
							IFXOnDragPolicy policy = dragIterator.previous();
							if (policy.showIndicationCursor(event)) {
								indicationCursorPolicy[0] = policy;
								break;
							}
						}
					}
				}
			};
			scene.addEventFilter(MouseEvent.MOUSE_MOVED,
					indicationCursorMouseMoveFilter);
			cursorMouseMoveFilters.put(scene, indicationCursorMouseMoveFilter);

			// register key event filter for forwarding events to drag policies
			// that can show a mouse cursor to indicate their action
			final EventHandler<KeyEvent> indicationCursorKeyFilter = new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (indicationCursorPolicy[0] != null) {
						indicationCursorPolicy[0].hideIndicationCursor();
						indicationCursorPolicy[0] = null;
					}

					if (possibleDragPolicies[0] == null
							|| possibleDragPolicies[0].isEmpty()) {
						return;
					}

					// search drag policies in reverse order first,
					// so that the policy closest to the target part
					// is the first policy to provide an indication
					// cursor
					ListIterator<? extends IFXOnDragPolicy> dragIterator = possibleDragPolicies[0]
							.listIterator(possibleDragPolicies[0].size());
					while (dragIterator.hasPrevious()) {
						IFXOnDragPolicy policy = dragIterator.previous();
						if (policy.showIndicationCursor(event)) {
							indicationCursorPolicy[0] = policy;
							break;
						}
					}
				}
			};
			scene.addEventFilter(KeyEvent.ANY, indicationCursorKeyFilter);
			cursorKeyFilters.put(scene, indicationCursorKeyFilter);

			AbstractMouseDragGesture gesture = new AbstractMouseDragGesture() {
				@Override
				protected void drag(Node target, MouseEvent e, double dx,
						double dy) {
					// abort processing of this gesture if no policies could be
					// found that can process it
					if (getActivePolicies(activeViewer).isEmpty()) {
						return;
					}

					for (IFXOnDragPolicy policy : getActivePolicies(
							activeViewer)) {
						policy.drag(e, new Dimension(dx, dy));
					}
				}

				@Override
				protected void press(Node target, MouseEvent e) {
					if (viewer instanceof FXViewer) {
						InfiniteCanvas canvas = ((FXViewer) viewer).getCanvas();
						// if any node in the target hierarchy is a scrollbar,
						// do not process the event
						if (e.getTarget() instanceof Node) {
							Node targetNode = (Node) e.getTarget();
							while (targetNode != null) {
								if (targetNode == canvas
										.getHorizontalScrollBar()
										|| targetNode == canvas
												.getVerticalScrollBar()) {
									return;
								}
								targetNode = targetNode.getParent();
							}
						}
					}

					// show indication cursor on press so that the indication
					// cursor is shown even when no mouse move event was
					// previously fired
					indicationCursorMouseMoveFilter.handle(e);

					// disable indication cursor event filters within
					// press-drag-release gesture
					Scene scene = viewer.getRootPart().getVisual().getScene();
					scene.removeEventFilter(MouseEvent.MOUSE_MOVED,
							indicationCursorMouseMoveFilter);
					scene.removeEventFilter(KeyEvent.ANY,
							indicationCursorKeyFilter);

					// determine click policies
					boolean opened = false;
					List<? extends IFXOnClickPolicy> clickPolicies = targetPolicyResolver
							.getTargetPolicies(FXClickDragTool.this, target,
									ON_CLICK_POLICY_KEY);

					// process click first
					if (clickPolicies != null && !clickPolicies.isEmpty()) {
						opened = true;
						getDomain()
								.openExecutionTransaction(FXClickDragTool.this);
						for (IFXOnClickPolicy clickPolicy : clickPolicies) {
							clickPolicy.click(e);
						}
					}

					// determine viewer that contains the given target part
					activeViewer = FXPartUtils.retrieveViewer(getDomain(),
							target);

					// determine drag policies
					List<? extends IFXOnDragPolicy> policies = null;
					if (activeViewer != null) {
						// XXX: A click policy could have changed the visual
						// hierarchy so that the viewer cannot be determined for
						// the target node anymore. If that is the case, no drag
						// policies should be notified about the event.
						policies = targetPolicyResolver.getTargetPolicies(
								FXClickDragTool.this, target, activeViewer,
								ON_DRAG_POLICY_KEY);
					}

					// abort processing of this gesture if no drag policies
					// could be found
					if (policies == null || policies.isEmpty()) {
						// remove this tool from the domain's execution
						// transaction if previously opened
						if (opened) {
							getDomain().closeExecutionTransaction(
									FXClickDragTool.this);
						}
						policies = null;
						return;
					}

					// add this tool to the execution transaction of the domain
					// if not yet opened
					if (!opened) {
						getDomain()
								.openExecutionTransaction(FXClickDragTool.this);
					}

					// mark the drag policies as active
					setActivePolicies(activeViewer, policies);

					// send press() to all drag policies
					for (IFXOnDragPolicy policy : policies) {
						policy.press(e);
					}
				}

				@Override
				protected void release(Node target, MouseEvent e, double dx,
						double dy) {
					// enable indication cursor event filters outside of
					// press-drag-release gesture
					Scene scene = viewer.getRootPart().getVisual().getScene();
					scene.addEventFilter(MouseEvent.MOUSE_MOVED,
							indicationCursorMouseMoveFilter);
					scene.addEventFilter(KeyEvent.ANY,
							indicationCursorKeyFilter);

					// abort processing of this gesture if no policies could be
					// found that can process it
					if (getActivePolicies(activeViewer).isEmpty()) {
						activeViewer = null;
						return;
					}

					// send release() to all drag policies
					for (IFXOnDragPolicy policy : getActivePolicies(
							activeViewer)) {
						policy.release(e, new Dimension(dx, dy));
					}

					// clear active policies before processing release
					clearActivePolicies(activeViewer);
					activeViewer = null;

					// remove this tool from the domain's execution transaction
					getDomain().closeExecutionTransaction(FXClickDragTool.this);

					// hide indication cursor
					if (indicationCursorPolicy[0] != null) {
						indicationCursorPolicy[0].hideIndicationCursor();
						indicationCursorPolicy[0] = null;
					}
				}
			};

			gesture.setScene(scene);
			gestures.put(scene, gesture);
		}
	}

	@Override
	protected void unregisterListeners() {
		for (Scene scene : new ArrayList<>(gestures.keySet())) {
			gestures.remove(scene).setScene(null);
			scene.removeEventFilter(MouseEvent.MOUSE_MOVED,
					cursorMouseMoveFilters.remove(scene));
			scene.removeEventFilter(KeyEvent.ANY,
					cursorKeyFilters.remove(scene));
		}
		for (IViewer<Node> viewer : new ArrayList<>(
				viewerFocusChangeListeners.keySet())) {
			viewer.viewerFocusedProperty()
					.removeListener(viewerFocusChangeListeners.remove(viewer));
		}
		super.unregisterListeners();
	}

}
