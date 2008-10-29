/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action.logicalmodel;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.action.local.BranchTagAction;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.action.ISyncStateFilter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Create branch logical model action for synchronize view
 * 
 * @author Igor Burilo
 */
public class CreateBranchModelAction extends AbstractSynchronizeLogicalModelAction {

	public CreateBranchModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);		
		IResource[] resources = this.getAllSelectedResources();
		for (IResource resource : resources) {
			if (IStateFilter.SF_EXCLUDE_DELETED.accept(SVNRemoteStorage.instance().asLocalResource(resource))) {
				return true;
			}
		}		
	    return false;
	}
		
	protected IActionOperation getOperation() {
		//TODO correctly use treeNodeSelector
		IResource []resources = FileUtility.getResourcesRecursive(this.getSyncInfoSelector().getSelectedResources(), 
				new ISyncStateFilter.StateFilterWrapper(IStateFilter.SF_EXCLUDE_DELETED, false), 				
				IResource.DEPTH_ZERO);
		
		return BranchTagAction.getBranchTagOperation(this.getConfiguration().getSite().getShell(), BranchTagAction.BRANCH_ACTION, resources);
	}

}
