/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.fx.policies;

import java.util.List;

import org.eclipse.gef4.mvc.fx.operations.FXRevealOperation;
import org.eclipse.gef4.mvc.models.FocusModel;
import org.eclipse.gef4.mvc.operations.AbstractCompositeOperation;
import org.eclipse.gef4.mvc.operations.ChangeFocusOperation;
import org.eclipse.gef4.mvc.operations.ForwardUndoCompositeOperation;
import org.eclipse.gef4.mvc.operations.ITransactionalOperation;
import org.eclipse.gef4.mvc.parts.IContentPart;
import org.eclipse.gef4.mvc.parts.IRootPart;
import org.eclipse.gef4.mvc.parts.IVisualPart;
import org.eclipse.gef4.mvc.policies.AbstractTransactionPolicy;
import org.eclipse.gef4.mvc.viewer.IViewer;

import com.google.common.reflect.TypeToken;

import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * The {@link FXFocusTraversalPolicy} can be used to assign focus to the next or
 * previous part in the focus traversal cycle.
 *
 * @author mwienand
 *
 */
public class FXFocusTraversalPolicy extends AbstractTransactionPolicy<Node> {

	private IViewer<Node> viewer;
	private FocusModel<Node> focusModel;

	@Override
	protected ITransactionalOperation createOperation() {
		ForwardUndoCompositeOperation focusAndRevealOperation = new ForwardUndoCompositeOperation(
				"Resize and Reveal");
		focusAndRevealOperation.add(new ChangeFocusOperation<>(viewer, null));
		focusAndRevealOperation.add(new FXRevealOperation(getHost()));
		return focusAndRevealOperation;
	}

	/**
	 * Returns the inner most {@link IContentPart} child within the part
	 * hierarchy of the given {@link IContentPart}. If the given
	 * {@link IContentPart} does not have any {@link IContentPart} children,
	 * then the given {@link IContentPart} is returned.
	 *
	 * @param part
	 *            The {@link IContentPart} for which to determine the inner most
	 *            {@link IContentPart} child.
	 * @return The inner most {@link IContentPart} child within the part
	 *         hierarchy of the given {@link IContentPart}.
	 */
	protected IContentPart<Node, ? extends Node> findInnerMostContentPart(
			IContentPart<Node, ? extends Node> part) {
		ObservableList<IVisualPart<Node, ? extends Node>> children = part
				.getChildrenUnmodifiable();
		while (!children.isEmpty()) {
			for (int i = children.size() - 1; i >= 0; i--) {
				IVisualPart<Node, ? extends Node> child = children.get(i);
				if (child instanceof IContentPart) {
					// continue searching for content part children within this
					// child's part hierarchy
					part = (IContentPart<Node, ? extends Node>) child;
					children = part.getChildrenUnmodifiable();
					break;
				}
			}
		}
		// did not find a content part child => return the given content part
		return part;
	}

	/**
	 * Determines the next {@link IContentPart} to which keyboard focus is
	 * assigned, depending on the currently focused {@link IContentPart}.
	 * <p>
	 * The first content part child of the given focus part is returned as the
	 * next part if a content part child is available.
	 * <p>
	 * The next content part sibling of the given focus part is returned as the
	 * next part if a content part sibling is available. When one sibling list
	 * ends, the search continues with the parent's siblings until it reaches
	 * the root of the hierarchy.
	 * <p>
	 * If the next content part cannot be determined, <code>null</code> is
	 * returned.
	 *
	 * @param current
	 *            The currently focused {@link IContentPart}.
	 * @return The next {@link IContentPart} to which keyboard focus is
	 *         assigned, or <code>null</code> if no subsequent
	 *         {@link IContentPart} could be determined.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected IContentPart<Node, ? extends Node> findNextContentPart(
			IContentPart<Node, ? extends Node> current) {
		// search children for content parts
		List<IVisualPart<Node, ? extends Node>> children = current
				.getChildrenUnmodifiable();
		if (!children.isEmpty()) {
			for (IVisualPart<Node, ? extends Node> child : children) {
				if (child instanceof IContentPart) {
					return (IContentPart<Node, ? extends Node>) child;
				}
			}
		}
		// no content part children available, therefore, we have to search our
		// siblings and move up the hierarchy
		IVisualPart<Node, ? extends Node> parent = current.getParent();
		while (parent instanceof IContentPart || parent instanceof IRootPart) {
			children = parent instanceof IContentPart
					? parent.getChildrenUnmodifiable()
					: ((IRootPart) parent).getContentPartChildren();
			int index = children.indexOf(current) + 1;
			while (index < children.size()) {
				IVisualPart<Node, ? extends Node> part = children.get(index);
				if (part instanceof IContentPart) {
					return (IContentPart<Node, ? extends Node>) part;
				}
				index++;
			}
			if (parent instanceof IContentPart) {
				current = (IContentPart<Node, ? extends Node>) parent;
				parent = current.getParent();
			} else {
				return null;
			}
		}
		// could not find another content part
		return null;
	}

	/**
	 * Determines the previous {@link IContentPart} to which keyboard focus is
	 * assigned, depending on the currently focused {@link IContentPart}.
	 * <p>
	 * At first, the previous content part sibling of the given focus part is
	 * determined. If a siblings list ends, the search continues with the
	 * parent's siblings.
	 * <p>
	 * The inner most content part child of the previous content part sibling is
	 * returned as the previous content part, or <code>null</code> if no
	 * previous content part sibling could be found.
	 *
	 * @param current
	 *            The currently focused {@link IContentPart}.
	 * @return The previous {@link IContentPart} to which keyboard focus is
	 *         assigned, or <code>null</code> if no previous
	 *         {@link IContentPart} could be determined.
	 */
	protected IContentPart<Node, ? extends Node> findPreviousContentPart(
			IContentPart<Node, ? extends Node> current) {
		// find previous content part sibling
		IVisualPart<Node, ? extends Node> parent = current.getParent();
		if (parent instanceof IContentPart || parent instanceof IRootPart) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<IVisualPart<Node, ? extends Node>> children = parent instanceof IContentPart
					? parent.getChildrenUnmodifiable()
					: ((IRootPart) parent).getContentPartChildren();
			int index = children.indexOf(current) - 1;
			while (index >= 0) {
				IVisualPart<Node, ? extends Node> part = children.get(index);
				if (part instanceof IContentPart) {
					return findInnerMostContentPart(
							(IContentPart<Node, ? extends Node>) part);
				}
				index--;
			}
			if (parent instanceof IContentPart) {
				return (IContentPart<Node, ? extends Node>) parent;
			}
		}
		// could not find a previous content part
		return null;
	}

	/**
	 * Assigns focus to the next part in the traversal cycle.
	 */
	public void focusNext() {
		traverse(false);
	}

	/**
	 * Assigns focus to the previous part in the traversal cycle.
	 */
	public void focusPrevious() {
		traverse(true);
	}

	/**
	 * Returns the {@link ChangeFocusOperation} that is used to change the focus
	 * part.
	 *
	 * @return The {@link ChangeFocusOperation} that is used to change the focus
	 *         part.
	 */
	@SuppressWarnings("unchecked")
	protected ChangeFocusOperation<Node> getChangeFocusOperation() {
		return (ChangeFocusOperation<Node>) ((AbstractCompositeOperation) getOperation())
				.getOperations().get(0);
	}

	/**
	 * Returns the {@link FXRevealOperation} that is used to reveal the focus
	 * part.
	 *
	 * @return The {@link FXRevealOperation} that is used to reveal the focus
	 *         part.
	 */
	private FXRevealOperation getRevealOperation() {
		return (FXRevealOperation) ((AbstractCompositeOperation) getOperation())
				.getOperations().get(1);
	}

	@SuppressWarnings("serial")
	@Override
	public void init() {
		viewer = getHost().getRoot().getViewer();
		focusModel = viewer.getAdapter(new TypeToken<FocusModel<Node>>() {
		});
		super.init();
	}

	/**
	 * Traverses the focus forwards or backwards depending on the given flag.
	 *
	 * @param backwards
	 *            <code>true</code> if the focus should be traversed backwards,
	 *            otherwise <code>false</code>.
	 */
	protected void traverse(boolean backwards) {
		// get current focus part
		IContentPart<Node, ? extends Node> current = focusModel.getFocus();
		IContentPart<Node, ? extends Node> next = null;

		// determine the first focus part if no part currently has focus
		if (current == null) {
			List<IContentPart<Node, ? extends Node>> children = viewer
					.getRootPart().getContentPartChildren();
			if (children != null && !children.isEmpty()) {
				if (backwards) {
					// focus last content leaf
					next = findInnerMostContentPart(
							children.get(children.size() - 1));
				} else {
					// focus first content part
					next = children.get(0);
				}
			}
		}

		// find the next/previous part that is focusable
		if (current != null || next != null && !next.isFocusable()) {
			// in case we did not select a next part yet, start with the
			// currently focused part
			if (next == null) {
				next = current;
			}
			// search until a focusable part is found
			if (backwards) {
				do {
					next = findPreviousContentPart(next);
				} while (next != null && !next.isFocusable());
			} else {
				do {
					next = findNextContentPart(next);
				} while (next != null && !next.isFocusable());
			}
		}

		// give focus to the next part or to the viewer (if next is null)
		if (next == null || next.isFocusable()) {
			getChangeFocusOperation().setNewFocused(next);
			getRevealOperation().setPart(next);
		}
	}

}
