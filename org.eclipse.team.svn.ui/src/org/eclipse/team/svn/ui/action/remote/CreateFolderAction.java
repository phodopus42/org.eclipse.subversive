/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.connector.ISVNConnector.Options;
import org.eclipse.team.svn.core.connector.SVNRevision.Kind;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.CreateFolderOperation;
import org.eclipse.team.svn.core.operation.remote.SetRevisionAuthorNameOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.remote.CreateFolderPanel;

/**
 * Create remote folder action
 * 
 * @author Alexander Gurov
 */
public class CreateFolderAction extends AbstractRepositoryTeamAction {

	public CreateFolderAction() {
		super();
	}
	
	public void runImpl(IAction action) {
	    CreateFolderPanel panel = new CreateFolderPanel();
		DefaultDialog dialog = new DefaultDialog(this.getShell(), panel);
				
		if (dialog.open() == 0) {
			IRepositoryResource []resources = this.getSelectedRepositoryResources();
			String folder = panel.getResourceName();
			String message = panel.getMessage();

			CreateFolderOperation mainOp = new CreateFolderOperation(resources[0], folder, message);
			CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
			
			op.add(mainOp);
			op.add(new RefreshRemoteResourcesOperation(resources));
			op.add(new SetRevisionAuthorNameOperation(mainOp, Options.FORCE), new IActionOperation[] {mainOp});
			
			this.runScheduled(op);
		}
	}

	public boolean isEnabled() {
		IRepositoryResource []resources = this.getSelectedRepositoryResources();
		return resources.length == 1 && resources[0].getSelectedRevision().getKind() == Kind.HEAD;
	}

}
