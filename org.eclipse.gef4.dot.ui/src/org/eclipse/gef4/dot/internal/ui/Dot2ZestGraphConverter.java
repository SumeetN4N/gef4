/*******************************************************************************
 * Copyright (c) 2015 itemis AG and others.
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
package org.eclipse.gef4.dot.internal.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gef4.dot.internal.DotAttributes;
import org.eclipse.gef4.geometry.planar.Point;
import org.eclipse.gef4.graph.Edge;
import org.eclipse.gef4.graph.Graph;
import org.eclipse.gef4.graph.Node;
import org.eclipse.gef4.layout.ILayoutAlgorithm;
import org.eclipse.gef4.layout.algorithms.GridLayoutAlgorithm;
import org.eclipse.gef4.layout.algorithms.RadialLayoutAlgorithm;
import org.eclipse.gef4.layout.algorithms.SpringLayoutAlgorithm;
import org.eclipse.gef4.layout.algorithms.TreeLayoutAlgorithm;
import org.eclipse.gef4.zest.fx.ZestProperties;

public class Dot2ZestGraphConverter {

	private Graph dotGraph;
	private Map<Node, Node> dotToZestNodes = new HashMap<Node, Node>();

	public Dot2ZestGraphConverter(Graph dotGraph) {
		this.dotGraph = dotGraph;
	}

	public Graph convert() {
		return convertGraph(dotGraph);
	}

	private Graph convertGraph(Graph dot) {
		Graph zest = new Graph();
		convertGraphAttributes(dot.getAttributes(), zest.getAttributes());
		// convert nodes and store dot to zest mapping, so that source and
		// destination of edges can be found easily later
		for (Node dotNode : dot.getNodes()) {
			Node zestNode = convertNode(dotNode);
			zestNode.setGraph(zest);
			dotToZestNodes.put(dotNode, zestNode);
			zest.getNodes().add(zestNode);
		}
		// convert edges
		for (Edge dotEdge : dot.getEdges()) {
			Edge edge = convertEdge(dotEdge);
			edge.setGraph(zest);
			zest.getEdges().add(edge);
		}
		return zest;
	}

	private Edge convertEdge(Edge dotEdge) {
		// find nodes
		Node zestSource = dotToZestNodes.get(dotEdge.getSource());
		Node zestTarget = dotToZestNodes.get(dotEdge.getTarget());
		// create edge
		Edge zestEdge = new Edge(zestSource, zestTarget);
		convertEdgeAttributes(dotEdge.getAttributes(),
				zestEdge.getAttributes());
		return zestEdge;
	}

	private void convertEdgeAttributes(Map<String, Object> dot,
			Map<String, Object> zest) {
		// convert id and label
		Object dotId = dot.get(DotAttributes.EDGE_ID);
		Object dotLabel = dot.get(DotAttributes.EDGE_LABEL);
		zest.put(ZestProperties.ELEMENT_CSS_ID, dotId);
		zest.put(ZestProperties.ELEMENT_LABEL, dotLabel);

		// convert edge style
		Object dotStyle = dot.get(DotAttributes.EDGE_STYLE);
		String curveCssStyle = null;
		if (DotAttributes.EDGE_STYLE_DASHED.equals(dotStyle)) {
			curveCssStyle = "-fx-stroke-dash-array: 7 7;"; //$NON-NLS-1$
		} else if (DotAttributes.EDGE_STYLE_DOTTED.equals(dotStyle)) {
			curveCssStyle = "-fx-stroke-dash-array: 1 7;"; //$NON-NLS-1$
		} else if (DotAttributes.EDGE_STYLE_BOLD.equals(dotStyle)) {
			curveCssStyle = "-fx-stroke-width: 2;"; //$NON-NLS-1$
		}
		// TODO: handle tapered edges

		if (curveCssStyle != null) {
			zest.put(ZestProperties.EDGE_CURVE_CSS_STYLE, curveCssStyle);
		}
	}

	private Node convertNode(Node dotNode) {
		Node zestNode = new Node();
		convertNodeAttributes(dotNode.getAttributes(),
				zestNode.getAttributes());
		// convert nested graph
		if (dotNode.getNestedGraph() != null) {
			Graph nested = convertGraph(dotNode.getNestedGraph());
			zestNode.setNestedGraph(nested);
		}
		return zestNode;
	}

	private void convertNodeAttributes(Map<String, Object> dot,
			Map<String, Object> zest) {
		// convert id and label
		Object dotId = dot.get(DotAttributes.NODE_ID);
		zest.put(ZestProperties.ELEMENT_CSS_ID, dotId);
		Object dotLabel = dot.get(DotAttributes.NODE_LABEL);
		if (dotLabel != null && dotLabel.equals("\\N")) { //$NON-NLS-1$
			// The node default label '\N' is used to indicate that a node's
			// name or ID becomes its label.
			dotLabel = dotId;
		}
		zest.put(ZestProperties.ELEMENT_LABEL, dotLabel);
		Object dotPos = dot.get(DotAttributes.NODE_POS);
		if (dotPos != null) {
			String posString = (String) dotPos;
			// handle update sign
			if (posString.contains("!")) { //$NON-NLS-1$
				posString = posString.substring(0, posString.indexOf("!")); //$NON-NLS-1$
			}
			double x = Double.parseDouble(
					posString.substring(0, posString.indexOf(","))); //$NON-NLS-1$
			double y = Double.parseDouble(
					posString.substring(posString.indexOf(",") + 1)); //$NON-NLS-1$
			zest.put(ZestProperties.NODE_POSITION, new Point(x, y));
			// if a position is specified in the DOT input, ensure Zest does not
			// alter it
			zest.put(ZestProperties.ELEMENT_LAYOUT_IRRELEVANT, Boolean.TRUE);
		}
	}

	private void convertGraphAttributes(Map<String, Object> dot,
			Map<String, Object> zest) {
		// convert layout and rankdir to LayoutAlgorithm
		Object dotLayout = dot.get(DotAttributes.GRAPH_LAYOUT);
		Object dotRankdir = dot.get(DotAttributes.GRAPH_RANKDIR);
		ILayoutAlgorithm algo = null;
		if (DotAttributes.GRAPH_LAYOUT_CIRCO.equals(dotLayout)
				|| DotAttributes.GRAPH_LAYOUT_NEATO.equals(dotLayout)
				|| DotAttributes.GRAPH_LAYOUT_TWOPI.equals(dotLayout)) {
			algo = new RadialLayoutAlgorithm();
		} else if (DotAttributes.GRAPH_LAYOUT_FDP.equals(dotLayout)
				|| DotAttributes.GRAPH_LAYOUT_SFDP.equals(dotLayout)) {
			algo = new SpringLayoutAlgorithm();
		} else if (DotAttributes.GRAPH_LAYOUT_GRID.equals(dotLayout)
				|| DotAttributes.GRAPH_LAYOUT_OSAGE.equals(dotLayout)) {
			algo = new GridLayoutAlgorithm();
		} else {
			boolean lr = DotAttributes.GRAPH_RANKDIR_LR.equals(dotRankdir);
			algo = new TreeLayoutAlgorithm(lr ? TreeLayoutAlgorithm.LEFT_RIGHT
					: TreeLayoutAlgorithm.TOP_DOWN);
		}
		zest.put(ZestProperties.GRAPH_LAYOUT_ALGORITHM, algo);

		// convert graph type
		Object dotType = dot.get(DotAttributes.GRAPH_TYPE);
		if (DotAttributes.GRAPH_TYPE_DIRECTED.equals(dotType)) {
			zest.put(ZestProperties.GRAPH_TYPE,
					ZestProperties.GRAPH_TYPE_DIRECTED);
		} else if (DotAttributes.GRAPH_TYPE_UNDIRECTED.equals(dotType)) {
			zest.put(ZestProperties.GRAPH_TYPE,
					ZestProperties.GRAPH_TYPE_UNDIRECTED);
		}
	}
}
