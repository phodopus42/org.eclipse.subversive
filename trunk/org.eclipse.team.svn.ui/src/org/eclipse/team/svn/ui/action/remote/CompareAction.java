/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elena Matokhina - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.CompareRepositoryResourcesOperation;
import org.eclipse.team.svn.ui.panel.remote.ComparePanel;

/**
 * Compare two repository resources action (available from Repositories View)
 * 
 * @author Elena Matokhina
 */
public class CompareAction extends AbstractRepositoryTeamAction {
    public CompareAction() {
        super();
    }

    public void run(IAction action) {
        IRepositoryResource first = this.getSelectedRepositoryResources()[0];
        ComparePanel panel = new ComparePanel(first);
        DefaultDialog dlg = new DefaultDialog(this.getShell(), panel);
        if (dlg.open() == 0) {
            this.runScheduled(new CompareRepositoryResourcesOperation(first, panel.getSelectedResource()));
        }
    }

    protected boolean isEnabled() throws TeamException {
		IRepositoryResource []resources = this.getSelectedRepositoryResources();
		if (resources.length != 1) {
			return false;
		}
		if (CoreExtensionsManager.instance().getSVNClientWrapperFactory().isCompareFoldersAllowed()) {
	        return true;
		}
		return resources[0] instanceof IRepositoryFile;
    }

}
