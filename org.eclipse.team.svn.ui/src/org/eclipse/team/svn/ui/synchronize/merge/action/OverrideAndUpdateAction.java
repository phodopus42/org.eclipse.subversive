/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *    Tobias Bosch - [patch] bug fix: Quick fix for JavaSVN problem with "force" option
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.merge.action;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor;
import org.eclipse.team.svn.core.connector.SVNConflictResolution;
import org.eclipse.team.svn.core.connector.SVNConflictDescriptor.Action;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetRemoteContentsOperation;
import org.eclipse.team.svn.core.operation.local.MarkResolvedOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IResourceChange;
import org.eclipse.team.svn.core.synchronize.AbstractSVNSyncInfo;
import org.eclipse.team.svn.core.synchronize.variant.RemoteResourceVariant;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.operation.ClearMergeStatusesOperation;
import org.eclipse.team.svn.ui.synchronize.action.AbstractSynchronizeModelAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Override and update action
 * 
 * @author Alexander Gurov
 */
public class OverrideAndUpdateAction extends AbstractSynchronizeModelAction {

	public OverrideAndUpdateAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter.SyncInfoDirectionFilter(new int[] {SyncInfo.CONFLICTING, SyncInfo.INCOMING}) {			
			public boolean select(SyncInfo info) {							
				//don't apply for tree conflicts which have incoming remote deletion
                return super.select(info) && new IStateFilter.AbstractTreeConflictingStateFilter() {
					protected boolean acceptTreeConflict(SVNConflictDescriptor treeConflict) {
						return treeConflict.action != Action.DELETE;
					}                	
                }.accept(((AbstractSVNSyncInfo)info).getLocalResource());
            }
		};
	}

	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		AbstractSVNSyncInfo []infos = this.getSVNSyncInfos();
		HashMap<String, String> remote2local = new HashMap<String, String>();
		ArrayList<IRepositoryResource> remoteSet = new ArrayList<IRepositoryResource>();
		ArrayList<IResource> localSet = new ArrayList<IResource>();
		for (int i = 0; i < infos.length; i++) {
			IResource resource = infos[i].getLocal();
			localSet.add(resource);
			IRepositoryResource remote = ((IResourceChange)((RemoteResourceVariant)infos[i].getRemote()).getResource()).getOriginator();
			remoteSet.add(remote);
			remote2local.put(SVNUtility.encodeURL(remote.getUrl()), FileUtility.getWorkingCopyPath(resource));
		}
		
		IResource []resources = localSet.toArray(new IResource[localSet.size()]);
		GetRemoteContentsOperation mainOp = new GetRemoteContentsOperation(resources, remoteSet.toArray(new IRepositoryResource[remoteSet.size()]), remote2local);
		CompositeOperation op = new CompositeOperation(mainOp.getId());
		SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(resources);
		op.add(saveOp);
		IResource []conflicting = FileUtility.getResourcesRecursive(resources, IStateFilter.SF_CONFLICTING, IResource.DEPTH_ZERO);
		if (conflicting.length > 0) {
			op.add(new MarkResolvedOperation(conflicting, SVNConflictResolution.CHOOSE_LOCAL_FULL, ISVNConnector.Depth.INFINITY));
		}
		op.add(mainOp);
		op.add(new RestoreProjectMetaOperation(saveOp));
		op.add(new RefreshResourcesOperation(conflicting.length > 0 ? FileUtility.getParents(resources, false) : resources));
		op.add(new ClearMergeStatusesOperation(resources));
		return op;
	}

}
