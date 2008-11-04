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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.variant.ResourceVariant;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeLogicalModelAction;
import org.eclipse.team.svn.ui.synchronize.action.ShowIncomingPropertiesActionHelper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Show properties logical model action implementation for Synchronize view.
 * 
 * @author Igor Burilo
 */
public class ShowIncomingPropertiesModelAction extends AbstractSynchronizeLogicalModelAction {
	
	protected ShowIncomingPropertiesActionHelper actionHelper;
	
	public ShowIncomingPropertiesModelAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		this.actionHelper = new ShowIncomingPropertiesActionHelper(this, configuration);
	}

	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		if (selection.size() == 1) {
			AbstractSVNSyncInfo syncInfo = this.getSelectedSVNSyncInfo();
			if (syncInfo != null && syncInfo.getKind() != SyncInfo.IN_SYNC) {
				ILocalResource incoming = ((ResourceVariant)syncInfo.getRemote()).getResource();
				return incoming instanceof IResourceChange && IStateFilter.ST_DELETED != incoming.getStatus();
			}					
		}

		return false;
	}
	
	protected IActionOperation getOperation() {
		return this.actionHelper.getOperation();
	}

}
