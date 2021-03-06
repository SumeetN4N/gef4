/*******************************************************************************
 * Copyright (c) 2014, 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.fx.parts;

import javafx.scene.shape.StrokeType;

/**
 * An {@link AbstractFXSegmentHandlePart} with a quadratic
 * {@link javafx.scene.shape.Rectangle} visual.
 *
 * @author mwienand
 *
 */
public class FXSquareSegmentHandlePart
		extends AbstractFXSegmentHandlePart<javafx.scene.shape.Rectangle> {

	/**
	 * The default size for this part's visualization.
	 */
	public static final double DEFAULT_SIZE = 4;

	@Override
	protected javafx.scene.shape.Rectangle createVisual() {
		javafx.scene.shape.Rectangle visual = new javafx.scene.shape.Rectangle();
		visual.setTranslateX(-DEFAULT_SIZE / 2);
		visual.setTranslateY(-DEFAULT_SIZE / 2);
		visual.setFill(getMoveFill());
		visual.setStroke(getStroke());
		visual.setWidth(DEFAULT_SIZE);
		visual.setHeight(DEFAULT_SIZE);
		visual.setStrokeWidth(1);
		visual.setStrokeType(StrokeType.OUTSIDE);
		return visual;
	}

}
