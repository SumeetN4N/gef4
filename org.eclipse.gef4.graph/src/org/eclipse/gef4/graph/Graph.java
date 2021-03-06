/*******************************************************************************
 * Copyright (c) 2013, 2016 Fabian Steeg and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabian Steeg    - initial API and implementation (bug #372365)
 *     Alexander Nyßen - refactoring of builder API (bug #480293)
 *
 *******************************************************************************/
package org.eclipse.gef4.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.gef4.common.attributes.IAttributeStore;
import org.eclipse.gef4.common.beans.property.ReadOnlyListWrapperEx;
import org.eclipse.gef4.common.beans.property.ReadOnlyMapWrapperEx;
import org.eclipse.gef4.common.collections.CollectionUtils;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * A {@link Graph} is a container for {@link Node}s and {@link Edge}s between
 * those {@link Node}s.
 *
 * @author Fabian Steeg
 * @author anyssen
 *
 */
public final class Graph implements IAttributeStore {

	/**
	 * The {@link Builder} can be used to construct a new {@link Graph} little
	 * by little.
	 */
	public static class Builder {

		/**
		 * A context object passed to nested builders when creating a builder
		 * chain.
		 *
		 */
		protected class Context {
			/**
			 * The {@link Graph.Builder} used to construct the {@link Graph},
			 * i.e. the root of the builder chain.
			 */
			protected Graph.Builder builder;
			/**
			 * {@link Node.Builder}s, which are part of the builder chain,
			 * mapped to their keys.
			 */
			protected Map<Object, Node.Builder> nodeBuilders = new HashMap<>();
			/**
			 * {@link Edge.Builder}s, which are part of the builder chain.
			 */
			protected List<Edge.Builder> edgeBuilders = new ArrayList<>();
		}

		// use linked hash map to preserve ordering
		private LinkedHashMap<Object, Node> nodes = new LinkedHashMap<>();
		private List<Edge> edges = new ArrayList<>();
		private Context context;

		private Map<String, Object> attrs = new HashMap<>();

		/**
		 * Constructs a new {@link Builder} without {@link Node}s and
		 * {@link Edge}s.
		 */
		public Builder() {
			context = new Context();
			context.builder = this;
		}

		/**
		 * Puts the given <i>key</i>-<i>value</i>-pair into the
		 * {@link Graph#attributesProperty() attributes map} of the
		 * {@link Graph} which is constructed by this {@link Builder}.
		 *
		 * @param key
		 *            The attribute name which is inserted.
		 * @param value
		 *            The attribute value which is inserted.
		 * @return <code>this</code> for convenience.
		 */
		public Graph.Builder attr(String key, Object value) {
			attrs.put(key, value);
			return this;
		}

		/**
		 * Constructs a new {@link Graph} from the values which have been
		 * supplied to this {@link Builder}.
		 *
		 * @return A new {@link Graph} from the values which have been supplied
		 *         to this {@link Builder}.
		 */
		public Graph build() {
			for (Node.Builder nb : context.nodeBuilders.values()) {
				nodes.put(nb.getKey(), nb.buildNode());
			}
			for (Edge.Builder eb : context.edgeBuilders) {
				edges.add(eb.buildEdge());
			}

			return new Graph(attrs, nodes.values(), edges);
		}

		/**
		 * Constructs a new {@link Edge.Builder}.
		 *
		 * @param sourceNodeOrKey
		 *            The source {@link Node} or a key to identify the source
		 *            {@link Node} (or its {@link Node.Builder}).
		 *
		 * @param targetNodeOrKey
		 *            The target {@link Node} or a key to identify the target
		 *            {@link Node} (or its {@link Node.Builder}).
		 *
		 * @return A new {@link Edge.Builder}.
		 */
		public Edge.Builder edge(Object sourceNodeOrKey, Object targetNodeOrKey) {
			Edge.Builder eb = new Edge.Builder(context, sourceNodeOrKey, targetNodeOrKey);
			return eb;
		}

		/**
		 * Adds the given {@link Edge}s to the {@link Graph} which is
		 * constructed by this {@link Builder}.
		 *
		 * @param edges
		 *            The {@link Edge}s which are added to the {@link Graph}
		 *            which is constructed by this {@link Builder}.
		 * @return <code>this</code> for convenience.
		 */
		public Graph.Builder edges(Collection<Edge> edges) {
			return edges(edges.toArray(new Edge[] {}));
		}

		/**
		 * Adds the given {@link Edge}s to the {@link Graph} which is
		 * constructed by this {@link Builder}.
		 *
		 * @param edges
		 *            The {@link Edge}s which are added to the {@link Graph}
		 *            which is constructed by this {@link Builder}.
		 * @return <code>this</code> for convenience.
		 */
		public Graph.Builder edges(Edge... edges) {
			this.edges.addAll(Arrays.asList(edges));
			return this;
		}

		/**
		 * Retrieves the node already created by a builder for the given key, or
		 * creates a new one via the respective {@link Node.Builder}.
		 *
		 * @param key
		 *            The key to identify the {@link Node} or
		 *            {@link Node.Builder}.
		 * @return An existing or newly created {@link Node}.
		 */
		protected Node findOrCreateNode(Object key) {
			// if we have already created a new with the given key, return the
			// created node
			if (nodes.containsKey(key)) {
				return nodes.get(key);
			} else {
				// create a new node
				org.eclipse.gef4.graph.Node.Builder nodeBuilder = context.nodeBuilders.get(key);
				if (nodeBuilder == null) {
					return null;
				}
				nodes.put(key, nodeBuilder.buildNode());
			}
			return nodes.get(key);
		}

		/**
		 * Constructs a new (anonymous) {@link Node.Builder}.
		 *
		 * @return A new {@link Node.Builder}.
		 */
		public Node.Builder node() {
			Node.Builder nb = new Node.Builder(context);
			return nb;
		}

		/**
		 * Constructs a new (identifiable) {@link Node.Builder}.
		 *
		 * @param key
		 *            The key that can be used to identify the
		 *            {@link Node.Builder}
		 *
		 * @return A new {@link Node.Builder}.
		 */
		public Node.Builder node(Object key) {
			Node.Builder nb = new Node.Builder(context, key);
			return nb;
		}

		/**
		 * Adds the given {@link Node}s to the {@link Graph} which is
		 * constructed by this {@link Builder}.
		 *
		 * @param nodes
		 *            The {@link Node}s which are added to the {@link Graph}
		 *            which is constructed by this {@link Builder}.
		 * @return <code>this</code> for convenience.
		 */
		public Graph.Builder nodes(Collection<Node> nodes) {
			return nodes(nodes.toArray(new Node[] {}));
		}

		/**
		 * Adds the given {@link Node}s to the {@link Graph} which is
		 * constructed by this {@link Builder}.
		 *
		 * @param nodes
		 *            The {@link Node}s which are added to the {@link Graph}
		 *            which is constructed by this {@link Builder}.
		 * @return <code>this</code> for convenience.
		 */
		public Graph.Builder nodes(Node... nodes) {
			for (Node n : nodes) {
				// use a unique id for each given node (they are not
				// identifiable from outside, so we just have to ensure the key
				// is not already used)
				this.nodes.put(UUID.randomUUID(), n);
			}
			return this;
		}
	}

	/**
	 * The name of the {@link #getNodes() nodes property}.
	 */
	public static final String NODES_PROPERTY = "nodes";

	/**
	 * The name of the {@link #getEdges() edgesProperty property}.
	 */
	public static final String EDGES_PROPERTY = "edgesProperty";

	/**
	 * {@link Node}s directly contained by this {@link Graph}.
	 */
	private final ReadOnlyListWrapper<Node> nodesProperty = new ReadOnlyListWrapperEx<>(this, NODES_PROPERTY,
			CollectionUtils.<Node> observableArrayList());

	/**
	 * {@link Edge}s for which this {@link Graph} is a common ancestor for
	 * {@link Edge#getSource() source} and {@link Edge#getTarget() target}.
	 */
	private final ReadOnlyListWrapper<Edge> edgesProperty = new ReadOnlyListWrapperEx<>(this, EDGES_PROPERTY,
			CollectionUtils.<Edge> observableArrayList());

	/**
	 * Attributes of this {@link Graph}.
	 */
	private final ReadOnlyMapWrapper<String, Object> attributesProperty = new ReadOnlyMapWrapperEx<>(this,
			ATTRIBUTES_PROPERTY, FXCollections.<String, Object> observableHashMap());

	/**
	 * {@link Node} which contains this {@link Graph}. May be <code>null</code>
	 * .
	 */
	private Node nestingNode; // when contained as a nested graph within a node

	/**
	 * Default constructor, using empty collections for attributes, nodes, and
	 * edgesProperty.
	 */
	public Graph() {
		this(new HashMap<String, Object>(), new ArrayList<Node>(), new ArrayList<Edge>());
	}

	/**
	 * Constructs a new {@link Graph} from the given attributes, nodes, and
	 * edgesProperty. Associates all nodes and edgesProperty with this
	 * {@link Graph}.
	 *
	 * @param attributes
	 *            Map of graph attributes.
	 * @param nodes
	 *            List of {@link Node}s.
	 * @param edges
	 *            List of {@link Edge}s.
	 */
	public Graph(Map<String, Object> attributes, Collection<? extends Node> nodes, Collection<? extends Edge> edges) {
		this.attributesProperty.putAll(attributes);
		this.nodesProperty.addAll(nodes);
		this.edgesProperty.addAll(edges);
		// set graph on nodes and edgesProperty
		for (Node n : nodes) {
			n.setGraph(this);
		}
		for (Edge e : edges) {
			e.setGraph(this);
		}
	}

	@Override
	public ReadOnlyMapProperty<String, Object> attributesProperty() {
		return attributesProperty.getReadOnlyProperty();
	}

	/**
	 * Returns a read-only list property containing the {@link Edge}s of this
	 * {@link Graph}.
	 *
	 * @return The list of {@link Edge}s of this {@link Graph}.
	 */
	public ReadOnlyListProperty<Edge> edgesProperty() {
		return edgesProperty.getReadOnlyProperty();
	}

	@Override
	public ObservableMap<String, Object> getAttributes() {
		return attributesProperty.get();
	}

	/**
	 * Returns the edgesProperty of this {@link Graph}.
	 *
	 * @return A list containing the edgesProperty.
	 */
	public ObservableList<Edge> getEdges() {
		return edgesProperty.getReadOnlyProperty();
	}

	/**
	 * Returns the {@link Node} in which this {@link Graph} is nested. Returns
	 * <code>null</code> when this {@link Graph} is not nested.
	 *
	 * @return The {@link Node} in which this {@link Graph} is nested, or
	 *         <code>null</code>.
	 */
	public Node getNestingNode() {
		return nestingNode;
	}

	/**
	 * Returns the nodes of this Graph.
	 *
	 * @return A list containing the nodes.
	 */
	public ObservableList<Node> getNodes() {
		return nodesProperty.getReadOnlyProperty();
	}

	/**
	 * Returns a read-only list property containing the {@link Node}s of this
	 * {@link Graph}.
	 *
	 * @return A read-only list property.
	 */
	public ReadOnlyListProperty<Node> nodesProperty() {
		return nodesProperty.getReadOnlyProperty();
	}

	/**
	 * Sets the nesting {@link Node} of this {@link Graph}.
	 *
	 * @param nestingNode
	 *            The new {@link Node} in which this {@link Graph} is nested.
	 */
	public void setNestingNode(Node nestingNode) {
		this.nestingNode = nestingNode;
		if (nestingNode.getNestedGraph() != this) {
			nestingNode.setNestedGraph(this);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Graph");
		sb.append(" attr {");
		boolean separator = false;

		TreeMap<String, Object> sortedAttrs = new TreeMap<>();
		sortedAttrs.putAll(attributesProperty);
		for (Object attrKey : sortedAttrs.keySet()) {
			if (separator) {
				sb.append(", ");
			} else {
				separator = true;
			}
			sb.append(attrKey.toString() + " : " + attributesProperty.get(attrKey));
		}
		sb.append("}");
		sb.append(".nodes {");
		separator = false;
		for (Node n : getNodes()) {
			if (separator) {
				sb.append(", ");
			} else {
				separator = true;
			}
			sb.append(n.toString());
		}
		sb.append("}");
		sb.append(".edges {");
		separator = false;
		for (Edge e : getEdges()) {
			if (separator) {
				sb.append(", ");
			} else {
				separator = true;
			}
			sb.append(e.toString());
		}
		sb.append("}");
		return sb.toString();
	}

}
