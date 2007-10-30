/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Vladimir Bykov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.remote;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.client.Revision;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.remote.CreateFileOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.action.AbstractRepositoryTeamAction;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.operation.RefreshRemoteResourcesOperation;
import org.eclipse.team.svn.ui.panel.remote.CreateFilePanel;

/**
 * Create Remote File Action implementation
 * 
 * @author Vladimir Bykov
 */
public class CreateFileAction extends AbstractRepositoryTeamAction {
	
	public CreateFileAction() {
		super();
	}
	
	public void runImpl(IAction action) {
		IRepositoryResource resource = this.getSelectedRepositoryResources()[0];
		CreateFilePanel panel = new CreateFilePanel(resource.getUrl());
		DefaultDialog dialog = new DefaultDialog(this.getShell(), panel);
	    if (dialog.open() == 0) {
	    	CreateFileOperation mainOp = new CreateFileOperation(resource, panel.getLocation(), panel.getMessage(), panel.getFileNames());
			CompositeOperation op = new CompositeOperation(mainOp.getId());
			op.add(mainOp);
			op.add(new RefreshRemoteResourcesOperation(this.getSelectedRepositoryResources()));
			this.runScheduled(op);
	    }
	}
	
	public boolean isEnabled() {
		IRepositoryResource []resources = this.getSelectedRepositoryResources();
		return resources.length == 1 && resources[0].getSelectedRevision().getKind() == Revision.Kind.head;
	}
	
}