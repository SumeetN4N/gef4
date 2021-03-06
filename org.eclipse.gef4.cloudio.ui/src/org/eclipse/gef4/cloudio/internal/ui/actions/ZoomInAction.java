/******************************************************************************
 * Copyright (c) 2011, 2015 Stephan Schwiebert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Schwiebert - initial API and implementation
 *     
 *******************************************************************************/
package org.eclipse.gef4.cloudio.internal.ui.actions;

import org.eclipse.jface.action.IAction;

/**
 * 
 * @author sschwieb
 *
 */
public class ZoomInAction extends AbstractTagCloudAction {

	@Override
	public void run(IAction action) {
		getViewer().zoomIn();
	}

}
