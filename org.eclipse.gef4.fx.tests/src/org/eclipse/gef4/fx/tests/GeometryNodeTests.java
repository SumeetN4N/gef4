/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.fx.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.gef4.fx.nodes.GeometryNode;
import org.eclipse.gef4.geometry.planar.RoundedRectangle;
import org.junit.Test;

import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;

public class GeometryNodeTests {

	/**
	 * Ensures setting/resizing the geometry will resize the visuals
	 */
	@Test
	public void resizeOnGeometryChange() {
		GeometryNode<RoundedRectangle> n = new GeometryNode<>();
		n.setFill(Color.RED);
		n.setStrokeWidth(5);
		n.setStrokeType(StrokeType.OUTSIDE);
		
		n.setGeometry(new RoundedRectangle(50, 50, 30, 40, 20, 20));
		assertEquals(n.getGeometry().getBounds().getWidth(), 30, 0);
		assertEquals(n.getGeometry().getBounds().getHeight(), 40, 0);
		assertEquals(40.0, n.getWidth(), 0);
		assertEquals(50.0, n.getHeight(), 0);
		
		n.resizeGeometry(50, 60);
		assertEquals(n.getGeometry().getBounds().getWidth(), 50, 0);
		assertEquals(n.getGeometry().getBounds().getHeight(), 60, 0);
		assertEquals(60.0, n.getWidth(), 0);
		assertEquals(70.0, n.getHeight(), 0);
	}

	public void resizeOnStrokeWidthAndTypeChange() {

	}
}
