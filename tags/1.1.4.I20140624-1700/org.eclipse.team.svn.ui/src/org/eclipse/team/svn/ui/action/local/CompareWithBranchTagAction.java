/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.action.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.factory.ISVNConnectorFactory;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.AbstractWorkingCopyAction;
import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.history.ISVNHistoryView;
import org.eclipse.team.svn.ui.operation.CompareResourcesOperation;
import org.eclipse.team.svn.ui.operation.ShowHistoryViewOperation;
import org.eclipse.team.svn.ui.panel.remote.CompareBranchTagPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * Compare with branch /tag action implementation
 * 
 * @author Alexei Goncharov
 */
public class CompareWithBranchTagAction extends AbstractWorkingCopyAction {
	protected int type;
	
	public CompareWithBranchTagAction(int type) {
		super();
		this.type = type;
	}

	public boolean isEnabled() {
		if (this.getSelectedResources().length == 1 && this.checkForResourcesPresence(CompareWithWorkingCopyAction.COMPARE_FILTER)) {
			IResource resource = this.getSelectedResources()[0];
			ILocalResource local = SVNRemoteStorage.instance().asLocalResource(resource);
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
			if (local.isCopied()) {
				IRepositoryResource remote = SVNUtility.getCopiedFrom(resource);		
				location = remote.getRepositoryLocation();
			}
			boolean isCompareFoldersAllowed = CoreExtensionsManager.instance().getSVNConnectorFactory().getSVNAPIVersion() >= ISVNConnectorFactory.APICompatibility.SVNAPI_1_5_x;
			boolean recommendedLayoutUsed = 
				SVNTeamPreferences.getRepositoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.BRANCH_TAG_CONSIDER_STRUCTURE_NAME) &&
				location.isStructureEnabled();
			return (isCompareFoldersAllowed || this.getSelectedResources()[0].getType() == IResource.FILE) && recommendedLayoutUsed;
		}
		return false;
	}

	public void runImpl(IAction action) {
		IResource resource = this.getSelectedResources()[0];
		ILocalResource local = SVNRemoteStorage.instance().asLocalResourceAccessible(resource);
		IRepositoryResource remote = local.isCopied() ? SVNUtility.getCopiedFrom(resource) : SVNRemoteStorage.instance().asRepositoryResource(resource);
			
		boolean considerStructure = BranchTagSelectionComposite.considerStructure(remote);
		IRepositoryResource[] branchTagResources = considerStructure ? BranchTagSelectionComposite.calculateBranchTagResources(remote, this.type) : new IRepositoryResource[0];
		if (!(considerStructure && branchTagResources.length == 0)) {
			CompareBranchTagPanel panel = new CompareBranchTagPanel(remote, this.type, branchTagResources);
			DefaultDialog dlg = new DefaultDialog(this.getShell(), panel);
			if (dlg.open() == 0 && panel.getResourceToCompareWith() != null){
				remote = panel.getResourceToCompareWith();
				String diffFile = panel.getDiffFile();
				CompareResourcesOperation mainOp = new CompareResourcesOperation(local, remote);
				mainOp.setDiffFile(diffFile);
				CompositeOperation op = new CompositeOperation(mainOp.getId(), mainOp.getMessagesClass());
				op.add(mainOp);
				if (SVNTeamPreferences.getHistoryBoolean(SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.HISTORY_CONNECT_TO_COMPARE_WITH_NAME)) {
					op.add(new ShowHistoryViewOperation(resource, remote, ISVNHistoryView.COMPARE_MODE, ISVNHistoryView.COMPARE_MODE), new IActionOperation[] {mainOp});
				}
				this.runScheduled(op);
			}	
		}		
	}

	protected boolean needsToSaveDirtyEditors() {
		return true;
	}
	
}
