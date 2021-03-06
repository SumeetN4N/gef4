/*******************************************************************************
 * Copyright (c) 2014, 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.examples.logo.ui.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef4.common.adapt.AdapterKey;
import org.eclipse.gef4.fx.anchors.DynamicAnchor;
import org.eclipse.gef4.fx.nodes.Connection;
import org.eclipse.gef4.geometry.planar.Point;
import org.eclipse.gef4.mvc.examples.logo.MvcLogoExample;
import org.eclipse.gef4.mvc.examples.logo.MvcLogoExampleModule;
import org.eclipse.gef4.mvc.examples.logo.MvcLogoExampleViewersComposite;
import org.eclipse.gef4.mvc.examples.logo.model.FXGeometricCurve;
import org.eclipse.gef4.mvc.examples.logo.ui.MvcLogoExampleUiModule;
import org.eclipse.gef4.mvc.examples.logo.ui.properties.FXCurvePropertySource;
import org.eclipse.gef4.mvc.fx.ui.parts.AbstractFXView;
import org.eclipse.gef4.mvc.fx.viewer.FXViewer;
import org.eclipse.gef4.mvc.models.ContentModel;
import org.eclipse.gef4.mvc.operations.AbstractCompositeOperation;
import org.eclipse.gef4.mvc.operations.DeselectOperation;
import org.eclipse.gef4.mvc.operations.ForwardUndoCompositeOperation;
import org.eclipse.gef4.mvc.operations.ITransactionalOperation;
import org.eclipse.gef4.mvc.operations.SelectOperation;
import org.eclipse.gef4.mvc.parts.IContentPart;
import org.eclipse.gef4.mvc.ui.properties.SetPropertyValueOperation;
import org.eclipse.gef4.mvc.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.gef4.mvc.ui.properties.UndoablePropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import com.google.inject.Guice;
import com.google.inject.util.Modules;

import javafx.scene.Node;
import javafx.scene.Scene;

public class MvcLogoExampleView extends AbstractFXView {

	public static final class ChangeWayPointsOperation extends AbstractOperation
			implements ITransactionalOperation {

		private final FXGeometricCurve curve;
		private final List<Point> newWayPoints;
		private final List<Point> oldWayPoints;

		public ChangeWayPointsOperation(String label, FXGeometricCurve curve,
				List<Point> oldWayPoints, List<Point> newWayPoints) {
			super(label);
			this.curve = curve;
			this.oldWayPoints = oldWayPoints;
			this.newWayPoints = newWayPoints;
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
			curve.setWayPoints(newWayPoints.toArray(new Point[] {}));
			return Status.OK_STATUS;
		}

		@Override
		public boolean isContentRelevant() {
			return true;
		}

		@Override
		public boolean isNoOp() {
			return oldWayPoints == newWayPoints || (oldWayPoints != null
					&& oldWayPoints.equals(newWayPoints));
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
			return execute(monitor, info);
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
			curve.setWayPoints(oldWayPoints.toArray(new Point[] {}));
			return Status.OK_STATUS;
		}
	}

	private UndoablePropertySheetEntry rootEntry;

	// TODO: create AbstractFXView via an executable extension factory
	// (obtaining the injector via the bundle)
	public MvcLogoExampleView() {
		super(Guice.createInjector(Modules.override(new MvcLogoExampleModule())
				.with(new MvcLogoExampleUiModule())));
		// set default contents (GEF logo)
		FXViewer viewer = getContentViewer();
		ContentModel contentModel = viewer.getAdapter(ContentModel.class);
		contentModel.getContents()
				.setAll(MvcLogoExample.createDefaultContents());
		// set palette contents
		FXViewer paletteViewer = getPaletteViewer();
		ContentModel paletteContentModel = paletteViewer
				.getAdapter(ContentModel.class);
		paletteContentModel.getContents()
				.setAll(MvcLogoExample.createPaletteContents());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class key) {
		if (IPropertySheetPage.class.equals(key)) {
			// use another UndoablePropertySheetEntry, which chains undo of
			// waypoint removal
			UndoablePropertySheetPage propertySheetPage = (UndoablePropertySheetPage) super.getAdapter(
					key);
			if (rootEntry == null) {
				rootEntry = new UndoablePropertySheetEntry(
						(IOperationHistory) getAdapter(IOperationHistory.class),
						(IUndoContext) getAdapter(IUndoContext.class)) {
					// FIXME: Code copied from FXBendConnectionPolicy (see
					// #494752)
					private Point computeEndHint(Connection connection) {
						if (connection.getEndAnchor() instanceof DynamicAnchor
								&& connection.getPointsUnmodifiable()
										.size() > 1) {
							Point endPoint = connection.getEndPoint();
							Point neighbor = connection.getPoint(
									connection.getPointsUnmodifiable().size()
											- 2);
							Point translated = endPoint.getTranslated(endPoint
									.getDifference(neighbor).getScaled(0.5));
							return translated;
						}
						return null;
					}

					// FIXME: Code copied from FXBendConnectionPolicy (see
					// #494752)
					private Point computeStartHint(Connection connection) {
						if (connection.getStartAnchor() instanceof DynamicAnchor
								&& connection.getPointsUnmodifiable()
										.size() > 1) {
							Point startPoint = connection.getStartPoint();
							Point neighbor = connection.getPoint(1);
							Point translated = startPoint.getTranslated(
									startPoint.getDifference(neighbor)
											.getScaled(0.5));
							return translated;
						}
						return null;
					}

					@Override
					public void setValues(Object[] objects) {
						if (objects == null || objects.length == 0) {
							// TODO: test
							objects = new Object[] { getContentViewer()
									.getAdapter(ContentModel.class)
									.getContents().get(0) };
						}
						super.setValues(objects);
					}

					@Override
					protected void valueChanged(
							UndoablePropertySheetEntry child,
							ITransactionalOperation operation) {
						// in case routing style is changed, clear the
						// waypoints (chain into a composite operation)
						if (operation instanceof SetPropertyValueOperation) {
							SetPropertyValueOperation changeRoutingStyleOperation = (SetPropertyValueOperation) operation;
							if (changeRoutingStyleOperation
									.getPropertySource() instanceof FXCurvePropertySource
									&& FXCurvePropertySource.ROUTING_STYLE_PROPERTY
											.getId()
											.equals(changeRoutingStyleOperation
													.getPropertyId())) {
								// clear way anchors using bend policy
								FXCurvePropertySource ps = (FXCurvePropertySource) changeRoutingStyleOperation
										.getPropertySource();
								IContentPart<Node, ? extends Node> contentPart = getContentViewer()
										.getContentPartMap().get(ps.getCurve());

								// preserve first and last waypoint, but clear
								// all intermediate points
								List<Point> newWaypoints = new ArrayList<>();
								List<Point> currentWaypoints = ps.getCurve()
										.getWayPointsCopy();
								// FIXME: Code copied from
								// FXBendConnectionPolicy (see #494752)
								newWaypoints.add(computeStartHint(
										(Connection) contentPart.getVisual()));
								newWaypoints.add(computeEndHint(
										(Connection) contentPart.getVisual()));

								ChangeWayPointsOperation clearWaypointsOperation = new ChangeWayPointsOperation(
										"Clear waypoints", ps.getCurve(),
										currentWaypoints, newWaypoints);
								AbstractCompositeOperation c = new ForwardUndoCompositeOperation(
										"Change routing style");
								c.add(changeRoutingStyleOperation);
								c.add(clearWaypointsOperation);
								// reselect
								IUndoableOperation deselectOperation = new DeselectOperation<>(
										getContentViewer(),
										Collections.singletonList(contentPart));
								IUndoableOperation selectOperation = new SelectOperation<>(
										getContentViewer(),
										Collections.singletonList(contentPart));
								c.add(deselectOperation);
								c.add(selectOperation);
								super.valueChanged(child, c);
							} else {
								super.valueChanged(child, operation);
							}
						} else {
							super.valueChanged(child, operation);
						}
					}
				};
				propertySheetPage.setRootEntry(rootEntry);
			}
			return propertySheetPage;
		}
		return super.getAdapter(key);
	}

	protected FXViewer getPaletteViewer() {
		return getDomain().getAdapter(AdapterKey.get(FXViewer.class,
				MvcLogoExampleModule.PALETTE_VIEWER_ROLE));
	}

	@Override
	protected void hookViewers() {
		// build viewers composite
		MvcLogoExampleViewersComposite viewersComposite = new MvcLogoExampleViewersComposite(
				getContentViewer(), getPaletteViewer());
		// create scene and populate canvas
		getCanvas().setScene(new Scene(viewersComposite.getComposite()));
	}

}
