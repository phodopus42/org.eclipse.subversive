/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *    Igor Burilo - Bug 245509: Improve extract log
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.ExtractToOperationLocal;
import org.eclipse.team.svn.core.operation.local.FiniExtractLogOperation;
import org.eclipse.team.svn.core.operation.local.InitExtractLogOperation;
import org.eclipse.team.svn.core.operation.remote.ExtractToOperationRemote;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.ui.synchronize.variant.RemoteResourceVariant;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Extract To action for Synchronize View (both incoming and outgoing - 
 * the conflicting resources are ignored)
 * 
 * @author Alexei Goncharov
 */
public class ExtractToAction extends AbstractSynchronizeModelAction {

	public ExtractToAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] {SyncInfo.INCOMING, SyncInfo.OUTGOING, SyncInfo.CONFLICTING});
	}
	
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		AbstractSVNSyncInfo [] infos = this.getSVNSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			if (SyncInfo.getDirection(infos[i].getKind()) == SyncInfo.CONFLICTING) {
				return false;
			}
		}
		return infos.length > 0;
	}
	
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		DirectoryDialog fileDialog = new DirectoryDialog(configuration.getSite().getShell());
		fileDialog.setText(SVNTeamUIPlugin.instance().getResource("ExtractToAction.Select.Title"));
		fileDialog.setMessage(SVNTeamUIPlugin.instance().getResource("ExtractToAction.Select.Description"));
		String path = fileDialog.open();
		if (path == null) {
			return null;
		}
		
		IResource []outgoingChanges = this.syncInfoSelector.getSelectedResources(
				new ISyncStateFilter.StateFilterWrapper(new IStateFilter.OrStateFilter(
						new IStateFilter[] {IStateFilter.SF_COMMITABLE, IStateFilter.SF_NEW}), null, true));
		HashSet<IResource> outgoingResources = new HashSet<IResource>(Arrays.asList(outgoingChanges));
		for (IResource current : outgoingChanges) {
			outgoingResources.add(current.getProject());
		}
		outgoingChanges = outgoingResources.toArray(new IResource[outgoingResources.size()]);
		IResource []incomingChanges = this.syncInfoSelector.getSelectedResources(
				new ISyncStateFilter.StateFilterWrapper(null, IStateFilter.SF_ANY_CHANGE, true));
		HashSet<IResource> incomingWithProjects = new HashSet<IResource>(Arrays.asList(incomingChanges));
		for (IResource current : incomingChanges) {
			incomingWithProjects.add(current.getProject());
		}
		incomingChanges = incomingWithProjects.toArray(new IResource[incomingWithProjects.size()]);
		HashSet<IResource> deletionsOnly = new HashSet<IResource>(Arrays.asList(this.syncInfoSelector.getSelectedResources(
				new ISyncStateFilter.StateFilterWrapper(null, IStateFilter.SF_DELETED, false))));
		HashSet<IRepositoryResource> incomingResourcesToOperate = new HashSet<IRepositoryResource>();
		HashSet<String> markedForDelition = new HashSet<String>();
		HashMap<String, String> resource2project = new HashMap<String, String>();
		HashMap<String, String> url2status = new HashMap<String, String>();
		for (IResource current : incomingChanges) {
			IRepositoryResource remote = SVNRemoteStorage.instance().asRepositoryResource(current);
			IRepositoryResource projectRemote = SVNRemoteStorage.instance().asRepositoryResource(current.getProject());
			if (current instanceof IProject) {
				resource2project.put(remote.getUrl(), current.getName());
			}
			else if (!new Path(projectRemote.getUrl()).isPrefixOf(new Path(remote.getUrl()))) {
				//external reference
				resource2project.put(remote.getUrl(), current.getFullPath().toString().substring(1));
			}
			incomingResourcesToOperate.add(remote);
			AbstractSVNSyncInfo[] syncInfos = this.getSVNSyncInfos();
			for (AbstractSVNSyncInfo info : syncInfos) {
				if (SyncInfo.getDirection(info.getKind()) == SyncInfo.INCOMING) {
					IResourceChange change = ((IResourceChange)((RemoteResourceVariant)info.getRemote()).getResource());
					if (remote.getUrl().equals(change.getOriginator().getUrl())) {
						url2status.put(remote.getUrl(), change.getStatus());
					}
				}
			}
			if (deletionsOnly.contains(current)) {
				markedForDelition.add(remote.getUrl());
			}
		}
		CompositeOperation op = new CompositeOperation("Operation.ExtractTo");
		InitExtractLogOperation logger = new InitExtractLogOperation(path);
		op.add(logger);
		op.add(new ExtractToOperationLocal(outgoingChanges, path, true, logger));
		op.add(new ExtractToOperationRemote(incomingResourcesToOperate.toArray(new IRepositoryResource[incomingResourcesToOperate.size()]), url2status, markedForDelition, path, resource2project, logger, true));
		op.add(new FiniExtractLogOperation(logger));
		return op;
	}

}
