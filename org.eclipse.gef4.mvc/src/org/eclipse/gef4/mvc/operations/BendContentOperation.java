/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.operations;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef4.mvc.parts.IBendableContentPart;
import org.eclipse.gef4.mvc.parts.IBendableContentPart.BendPoint;
import org.eclipse.gef4.mvc.parts.IResizableContentPart;
import org.eclipse.gef4.mvc.parts.ITransformableContentPart;

/**
 * An {@link ITransactionalOperation} to bend an {@link IBendableContentPart}.
 *
 * @author anyssen
 *
 * @param <VR>
 *            The visual root node of the UI toolkit this
 *            {@link ITransformableContentPart} is used in, e.g.
 *            javafx.scene.Node in case of JavaFX.
 */
public class BendContentOperation<VR> extends AbstractOperation
		implements ITransactionalOperation {

	private final IBendableContentPart<VR, ? extends VR> bendableContentPart;
	private List<BendPoint> initialBendPoints;
	private List<BendPoint> finalBendPoints;

	/**
	 * Creates a new {@link BendContentOperation} to resize the content of the
	 * given {@link IResizableContentPart}.
	 *
	 * @param bendableContentPart
	 *            The part to bend.
	 * @param initialBendPoints
	 *            The initial bend points before applying the change.
	 * @param finalBendPoints
	 *            The final bend points after applying the change.
	 */
	public BendContentOperation(
			IBendableContentPart<VR, ? extends VR> bendableContentPart,
			List<BendPoint> initialBendPoints,
			List<BendPoint> finalBendPoints) {
		super("Bend Content");
		this.bendableContentPart = bendableContentPart;
		this.initialBendPoints = initialBendPoints;
		this.finalBendPoints = finalBendPoints;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		// TODO (bug #493515): retrieve current bend points from
		// bendableContentPart and only
		// call bendContent if a change occurred.
		bendableContentPart.bendContent(finalBendPoints);
		return Status.OK_STATUS;
	}

	@Override
	public boolean isContentRelevant() {
		return true;
	}

	@Override
	public boolean isNoOp() {
		return initialBendPoints == null ? finalBendPoints == null
				: initialBendPoints.equals(finalBendPoints);
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return execute(monitor, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		// TODO (bug #493515): retrieve current bend points from
		// bendableContentPart and only
		// call bendContent if a change occurred.
		bendableContentPart.bendContent(initialBendPoints);
		return Status.OK_STATUS;
	}
}