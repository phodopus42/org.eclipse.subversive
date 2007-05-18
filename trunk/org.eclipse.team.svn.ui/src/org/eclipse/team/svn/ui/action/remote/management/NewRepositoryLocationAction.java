/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote.management;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.svn.ui.wizard.NewRepositoryLocationWizard;

/**
 * New repository location action implementation
 * 
 * @author Alexander Gurov
 */
public class NewRepositoryLocationAction extends TeamAction {

	public NewRepositoryLocationAction() {
		super();
	}
	
	public void run(IAction action) {
		NewRepositoryLocationWizard wizard = new NewRepositoryLocationWizard();
		WizardDialog dialog = new WizardDialog(this.getShell(), wizard);
		dialog.open();
	}

	protected boolean isEnabled() throws TeamException {
		return true;
	}

}
