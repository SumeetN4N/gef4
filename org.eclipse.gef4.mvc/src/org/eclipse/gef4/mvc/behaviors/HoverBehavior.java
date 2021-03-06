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
package org.eclipse.gef4.mvc.behaviors;

import java.util.Collections;
import java.util.List;

import org.eclipse.gef4.common.adapt.AdapterKey;
import org.eclipse.gef4.common.reflect.Types;
import org.eclipse.gef4.mvc.models.HoverModel;
import org.eclipse.gef4.mvc.parts.IFeedbackPartFactory;
import org.eclipse.gef4.mvc.parts.IHandlePartFactory;
import org.eclipse.gef4.mvc.parts.IVisualPart;
import org.eclipse.gef4.mvc.viewer.IViewer;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * The {@link HoverBehavior} is responsible for creating and removing selection
 * feedback.
 *
 * @author anyssen
 *
 * @param <VR>
 *            The visual root node of the UI toolkit used, e.g.
 *            javafx.scene.Node in case of JavaFX.
 */
public class HoverBehavior<VR> extends AbstractBehavior<VR> {

	/**
	 * The adapter role for the {@link IFeedbackPartFactory} that is used to
	 * generate hover feedback parts.
	 */
	public static final String HOVER_FEEDBACK_PART_FACTORY = "HOVER_FEEDBACK_PART_FACTORY";

	/**
	 * The adapter role for the {@link IHandlePartFactory} that is used to
	 * generate hover handle parts.
	 */
	public static final String HOVER_HANDLE_PART_FACTORY = "HOVER_HANDLE_PART_FACTORY";

	private ChangeListener<IVisualPart<VR, ? extends VR>> hoverObserver = new ChangeListener<IVisualPart<VR, ? extends VR>>() {
		@Override
		public void changed(
				ObservableValue<? extends IVisualPart<VR, ? extends VR>> observable,
				IVisualPart<VR, ? extends VR> oldValue,
				IVisualPart<VR, ? extends VR> newValue) {
			onHoverChange(oldValue, newValue);
		}
	};

	@Override
	protected void doActivate() {
		// register
		HoverModel<VR> hoverModel = getHoverModel();
		hoverModel.hoverProperty().addListener(hoverObserver);

		// create feedback and handles if we are already hovered
		IVisualPart<VR, ? extends VR> hover = hoverModel.getHover();
		if (hover != null) {
			onHoverChange(null, hover);
		}
	}

	@Override
	protected void doDeactivate() {
		HoverModel<VR> hoverModel = getHoverModel();

		// remove any pending feedback and handles
		IVisualPart<VR, ? extends VR> hover = hoverModel.getHover();
		if (hover != null) {
			onHoverChange(hover, null);
		}

		// unregister
		hoverModel.hoverProperty().removeListener(hoverObserver);
	}

	/**
	 * Returns the {@link IFeedbackPartFactory} for hover feedback.
	 *
	 * @return The {@link IFeedbackPartFactory} for hover feedback.
	 */
	@SuppressWarnings("serial")
	protected IFeedbackPartFactory<VR> getFeedbackPartFactory() {
		IViewer<VR> viewer = getHost().getRoot().getViewer();
		return viewer.getAdapter(
				AdapterKey.get(new TypeToken<IFeedbackPartFactory<VR>>() {
				}.where(new TypeParameter<VR>() {
				}, Types.<VR> argumentOf(viewer.getClass())),
						HOVER_FEEDBACK_PART_FACTORY));
	}

	/**
	 * Returns the {@link HoverModel} in the context of the {@link #getHost()
	 * host}.
	 *
	 * @return The {@link HoverModel} in the context of the {@link #getHost()
	 *         host}.
	 */
	@SuppressWarnings("serial")
	protected HoverModel<VR> getHoverModel() {
		IViewer<VR> viewer = getHost().getRoot().getViewer();
		HoverModel<VR> hoverModel = viewer
				.getAdapter(new TypeToken<HoverModel<VR>>() {
				}.where(new TypeParameter<VR>() {
				}, Types.<VR> argumentOf(viewer.getClass())));
		return hoverModel;
	}

	/**
	 * Called when the {@link HoverModel} changes, i.e. a part is unhovered or
	 * hovered. Adds/Removes feedback accordingly.
	 *
	 * @param oldHovered
	 *            The previously hovered part, or <code>null</code>.
	 * @param newHovered
	 *            The newly hovered part, or <code>null</code>.
	 */
	protected void onHoverChange(IVisualPart<VR, ? extends VR> oldHovered,
			IVisualPart<VR, ? extends VR> newHovered) {
		if (getHost() != oldHovered && getHost() == newHovered) {
			switchAdaptableScopes();
			List<IVisualPart<VR, ? extends VR>> targets = Collections
					.<IVisualPart<VR, ? extends VR>> singletonList(getHost());
			addFeedback(targets, getFeedbackPartFactory().createFeedbackParts(
					targets, this, Collections.emptyMap()));
		} else if (getHost() == oldHovered && getHost() != newHovered) {
			removeFeedback(Collections.singletonList(getHost()));
		}
	}

}
