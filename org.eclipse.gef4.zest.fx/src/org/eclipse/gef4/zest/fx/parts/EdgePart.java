/*******************************************************************************
 * Copyright (c) 2014, 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.zest.fx.parts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.gef4.fx.anchors.IAnchor;
import org.eclipse.gef4.fx.nodes.Connection;
import org.eclipse.gef4.fx.nodes.IConnectionInterpolator;
import org.eclipse.gef4.fx.nodes.IConnectionRouter;
import org.eclipse.gef4.geometry.planar.AffineTransform;
import org.eclipse.gef4.geometry.planar.Point;
import org.eclipse.gef4.graph.Edge;
import org.eclipse.gef4.mvc.fx.parts.AbstractFXContentPart;
import org.eclipse.gef4.mvc.fx.providers.IAnchorProvider;
import org.eclipse.gef4.mvc.parts.IBendableContentPart;
import org.eclipse.gef4.mvc.parts.ITransformableContentPart;
import org.eclipse.gef4.mvc.parts.IVisualPart;
import org.eclipse.gef4.zest.fx.ZestProperties;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import javafx.collections.MapChangeListener;
import javafx.scene.Node;

/**
 * The {@link EdgePart} is the controller for an {@link Edge} content object. It
 * uses {@link Connection} for the visualization.
 *
 * @author mwienand
 *
 */
public class EdgePart extends AbstractFXContentPart<Connection>
		implements ITransformableContentPart<Node, Connection>, IBendableContentPart<Node, Connection> {

	/**
	 * The role used for attaching to the source node.
	 */
	protected static final String SOURCE_ROLE = "SOURCE";
	/**
	 * The role used for attaching to the target node.
	 */
	protected static final String TARGET_ROLE = "TARGET";

	/**
	 * The CSS class that is assigned to the visual of this {@link EdgePart}.
	 */
	public static final String CSS_CLASS = "edge";

	/**
	 * The CSS class that is assigned to the {@link Connection#getCurve() curve
	 * node} of the {@link Connection} of this {@link EdgePart}.
	 */
	public static final String CSS_CLASS_CURVE = "curve";

	/**
	 * CSS class assigned to the decorations.
	 */
	public static final String CSS_CLASS_DECORATION = "decoration";

	private MapChangeListener<String, Object> edgeAttributesObserver = new MapChangeListener<String, Object>() {

		@Override
		public void onChanged(MapChangeListener.Change<? extends String, ? extends Object> change) {
			if (ZestProperties.ROUTER__E.equals(change.getKey())) {
				// if the router changed, re-attach the visual (so we attach to
				// a different anchor)
				for (Entry<IVisualPart<Node, ? extends Node>, String> anchoragesByRole : getAnchoragesUnmodifiable()
						.entries()) {
					detachFromAnchorageVisual(anchoragesByRole.getKey(), anchoragesByRole.getValue());
					attachToAnchorageVisual(anchoragesByRole.getKey(), anchoragesByRole.getValue());
				}
			}
			refreshVisual();
		}

	};

	@Override
	protected void addChildVisual(IVisualPart<Node, ? extends Node> child, int index) {
		if (!getVisual().getChildren().contains(child.getVisual())) {
			getVisual().getChildren().add(child.getVisual());
		}
	}

	@Override
	protected void attachToAnchorageVisual(IVisualPart<Node, ? extends Node> anchorage, String role) {
		IAnchor anchor = anchorage.getAdapter(IAnchorProvider.class).get(this, role);
		if (role.equals(SOURCE_ROLE)) {
			getVisual().setStartAnchor(anchor);
		} else if (role.equals(TARGET_ROLE)) {
			getVisual().setEndAnchor(anchor);
		} else {
			throw new IllegalArgumentException("Cannot attach to anchor with role <" + role + ">.");
		}
	}

	@Override
	public void bendContent(List<org.eclipse.gef4.mvc.parts.IBendableContentPart.BendPoint> bendPoints) {
		List<Point> positions = new ArrayList<>();
		boolean attachedSource = false;
		boolean attachedTarget = false;
		// collect positions and de-/attach source & target
		for (int i = 0; i < bendPoints.size(); i++) {
			BendPoint bp = bendPoints.get(i);
			if (i == 0) {
				// update source
				org.eclipse.gef4.graph.Node newSource = bp.isAttached()
						? (org.eclipse.gef4.graph.Node) bp.getContentAnchorage() : null;
				org.eclipse.gef4.graph.Node oldSource = getContent().getSource();
				if (oldSource != newSource) {
					if (oldSource != null) {
						detachFromContentAnchorage(oldSource, SOURCE_ROLE);
					}
					if (newSource != null) {
						attachToContentAnchorage(newSource, SOURCE_ROLE);
						attachedSource = true;
					}
				} else if (oldSource != null) {
					attachedSource = true;
				}
			}
			if (i == bendPoints.size() - 1) {
				// update target
				org.eclipse.gef4.graph.Node newTarget = bp.isAttached()
						? (org.eclipse.gef4.graph.Node) bp.getContentAnchorage() : null;
				org.eclipse.gef4.graph.Node oldTarget = getContent().getTarget();
				if (oldTarget != newTarget) {
					if (oldTarget != null) {
						detachFromContentAnchorage(oldTarget, TARGET_ROLE);
					}
					if (newTarget != null) {
						attachToContentAnchorage(newTarget, TARGET_ROLE);
						attachedTarget = true;
					}
				} else if (oldTarget != null) {
					attachedTarget = true;
				}
			}
			if (!bp.isAttached()) {
				positions.add(bp.getPosition());
			}
		}
		// update properties
		if (!attachedSource) {
			if (positions.size() > 0) {
				ZestProperties.setStartPoint(getContent(), positions.remove(0));
			} else {
				throw new IllegalStateException("No start point provided.");
			}
		} else {
			// XXX: Set start hint as Zest start point property so it can be
			// used within doRefreshVisual().
			ZestProperties.setStartPoint(getContent(), getVisual().getStartPointHint());
		}
		if (!attachedTarget) {
			if (positions.size() > 0) {
				ZestProperties.setEndPoint(getContent(), positions.remove(positions.size() - 1));
			} else {
				throw new IllegalStateException("No start point provided.");
			}
		} else {
			// XXX: Set start hint as Zest start point property so it can be
			// used within doRefreshVisual().
			ZestProperties.setEndPoint(getContent(), getVisual().getEndPointHint());
		}
		ZestProperties.setControlPoints(getContent(), positions);
	}

	@Override
	protected Connection createVisual() {
		Connection visual = new Connection();
		visual.getStyleClass().add(CSS_CLASS);

		// initialize style class for (default) curve
		visual.getCurve().getStyleClass().add(CSS_CLASS_CURVE);

		// initialized style class for (default) decorations
		if (visual.getStartDecoration() != null) {
			if (!visual.getStartDecoration().getStyleClass().contains(CSS_CLASS_DECORATION)) {
				visual.getStartDecoration().getStyleClass().add(CSS_CLASS_DECORATION);
			}
		}
		if (visual.getEndDecoration() != null) {
			if (!visual.getEndDecoration().getStyleClass().contains(CSS_CLASS_DECORATION)) {
				visual.getEndDecoration().getStyleClass().add(CSS_CLASS_DECORATION);
			}
		}
		return visual;
	}

	@Override
	protected void detachFromAnchorageVisual(IVisualPart<Node, ? extends Node> anchorage, String role) {
		Connection connection = getVisual();
		if (role.equals(SOURCE_ROLE)) {
			Point startPoint = connection.getStartPoint();
			connection.setStartPoint(startPoint == null ? new Point() : startPoint);
		} else if (role.equals(TARGET_ROLE)) {
			Point endPoint = connection.getEndPoint();
			connection.setEndPoint(endPoint == null ? new Point() : endPoint);
		} else {
			throw new IllegalArgumentException("Cannot detach from anchor with role <" + role + ">.");
		}
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		getContent().attributesProperty().addListener(edgeAttributesObserver);
	}

	@Override
	protected void doAttachToContentAnchorage(Object contentAnchorage, String role) {
		if (SOURCE_ROLE.equals(role)) {
			getContent().setSource((org.eclipse.gef4.graph.Node) contentAnchorage);
		} else if (TARGET_ROLE.equals(role)) {
			getContent().setTarget((org.eclipse.gef4.graph.Node) contentAnchorage);
		} else {
			throw new IllegalArgumentException("Cannot attach to content anchorage with role <" + role + ">.");
		}
	}

	@Override
	protected void doDeactivate() {
		getContent().attributesProperty().removeListener(edgeAttributesObserver);
		super.doDeactivate();
	}

	@Override
	protected void doDetachFromContentAnchorage(Object contentAnchorage, String role) {
		if (SOURCE_ROLE.equals(role)) {
			getContent().setSource(null);
		} else if (TARGET_ROLE.equals(role)) {
			getContent().setTarget(null);
		} else {
			throw new IllegalArgumentException("Cannot detach from content anchorage with role <" + role + ">.");
		}
	}

	@Override
	protected SetMultimap<? extends Object, String> doGetContentAnchorages() {
		SetMultimap<Object, String> anchorages = HashMultimap.create();
		org.eclipse.gef4.graph.Node source = getContent().getSource();
		if (source != null) {
			anchorages.put(source, SOURCE_ROLE);
		}
		org.eclipse.gef4.graph.Node target = getContent().getTarget();
		if (target != null) {
			anchorages.put(target, TARGET_ROLE);
		}
		return anchorages;
	}

	@Override
	protected List<? extends Object> doGetContentChildren() {
		return Collections.emptyList();
	}

	@Override
	protected void doRefreshVisual(Connection visual) {
		Edge edge = getContent();
		Map<String, Object> attrs = edge.attributesProperty();

		refreshCurve();

		// css class
		if (attrs.containsKey(ZestProperties.CSS_CLASS__NE)) {
			String cssClass = ZestProperties.getCssClass(edge);
			if (!visual.getStyleClass().contains(cssClass)) {
				visual.getStyleClass().add(cssClass);
			}
		}

		// css id
		if (attrs.containsKey(ZestProperties.CSS_ID__NE)) {
			String cssId = ZestProperties.getCssId(edge);
			visual.setId(cssId);
		}

		// css style
		String curveCssStyle = ZestProperties.getCurveCssStyle(edge);
		if (attrs.containsKey(ZestProperties.CURVE_CSS_STYLE__E)) {
			visual.getCurve().setStyle(curveCssStyle);
		}

		// custom decoration
		Node sourceDecoration = ZestProperties.getSourceDecoration(edge);
		if (sourceDecoration != null) {
			visual.setStartDecoration(sourceDecoration);
			// apply CSS class
			if (!sourceDecoration.getStyleClass().contains(CSS_CLASS_DECORATION)) {
				sourceDecoration.getStyleClass().add(CSS_CLASS_DECORATION);
			}
			// apply CSS style
			String sourceDecorationCssStyle = ZestProperties.getSourceDecorationCssStyle(edge);
			if (sourceDecorationCssStyle != null) {
				sourceDecoration.setStyle(sourceDecorationCssStyle);
			}
		}
		Node targetDecoration = ZestProperties.getTargetDecoration(edge);
		if (targetDecoration != null) {
			visual.setEndDecoration(targetDecoration);
			// apply CSS class
			if (!targetDecoration.getStyleClass().contains(CSS_CLASS_DECORATION)) {
				targetDecoration.getStyleClass().add(CSS_CLASS_DECORATION);
			}
			// apply CSS style
			String targetDecorationCssStyle = ZestProperties.getTargetDecorationCssStyle(edge);
			if (targetDecorationCssStyle != null) {
				targetDecoration.setStyle(targetDecorationCssStyle);
			}
		}

		// connection router
		IConnectionRouter router = ZestProperties.getRouter(edge);
		if (router != null) {
			visual.setRouter(router);
		}

		// interpolator
		IConnectionInterpolator interpolator = ZestProperties.getInterpolator(edge);
		if (interpolator != null) {
			visual.setInterpolator(interpolator);
		}

		// start point or hint
		Point startPoint = ZestProperties.getStartPoint(edge);
		if (!getContentAnchoragesUnmodifiable().containsValue(SOURCE_ROLE)) {
			if (startPoint != null) {
				visual.setStartPoint(startPoint);
			}
		} else {
			visual.setStartPointHint(startPoint);
		}

		// end point or hint
		Point endPoint = ZestProperties.getEndPoint(edge);
		if (!getContentAnchoragesUnmodifiable().containsValue(TARGET_ROLE)) {
			if (endPoint != null) {
				visual.setEndPoint(endPoint);
			}
		} else {
			visual.setEndPointHint(endPoint);
		}

		// control points
		List<Point> controlPoints = new ArrayList<>(ZestProperties.getControlPoints(edge));
		if (!visual.getControlPoints().equals(controlPoints)) {
			visual.setControlPoints(controlPoints);
		}
	}

	@Override
	public Edge getContent() {
		return (Edge) super.getContent();
	}

	/**
	 * Returns the {@link Node} that displays the edge.
	 *
	 * @return The {@link Node} used to display the edge.
	 */
	public Node getCurve() {
		return getVisual().getCurve();
	}

	private void refreshCurve() {
		Node curve = ZestProperties.getCurve(getContent());
		if (getVisual().getCurve() != curve && curve != null) {
			getVisual().setCurve(curve);
			if (!curve.getStyleClass().contains(CSS_CLASS_CURVE)) {
				curve.getStyleClass().add(CSS_CLASS_CURVE);
			}
		}
	}

	@Override
	protected void removeChildVisual(IVisualPart<Node, ? extends Node> child, int index) {
		getVisual().getChildren().remove(child.getVisual());
	}

	@Override
	public void transformContent(AffineTransform transform) {
		ZestProperties.setControlPoints(getContent(), Arrays.asList(
				transform.getTransformed(ZestProperties.getControlPoints(getContent()).toArray(new Point[] {}))));
	}

}
