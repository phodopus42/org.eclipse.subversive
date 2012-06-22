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

package org.eclipse.team.svn.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.resource.events.ResourceStatesChangedEvent;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * Workspace resource changes listener
 * 
 * @author Alexander Gurov
 */
public class ResourceChangeListener implements IResourceChangeListener, ISaveParticipant {
	public static int INTERESTING_CHANGES = 
	    IResourceDelta.MOVED_FROM | 
		IResourceDelta.MOVED_TO |
		IResourceDelta.OPEN | 
		IResourceDelta.REPLACED |
		IResourceDelta.TYPE;

	public ResourceChangeListener() {

	}

	public void resourceChanged(final IResourceChangeEvent event) {
		ProgressMonitorUtility.doTaskScheduledDefault(new AbstractActionOperation("Operation_ResourcesChanged", SVNMessages.class) { //$NON-NLS-1$
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				final Set<IResource> modified = new HashSet<IResource>();
				event.getDelta().accept(new IResourceDeltaVisitor() {
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						
						if (resource.getType() == IResource.ROOT) {
							return true;
						}
						if (!FileUtility.isConnected(resource)) {
						    return false;
						}
						if (FileUtility.isSVNInternals(resource)) {
							IContainer parent = resource.getParent();
							modified.add(parent);
							if (parent.exists()) {
								modified.addAll(Arrays.asList(parent.members()));
							}
							return false;
						}

						if (delta.getKind() == IResourceDelta.ADDED ||
							delta.getKind() == IResourceDelta.REMOVED) {
							modified.add(resource);
						}
						if (delta.getKind() == IResourceDelta.CHANGED) {
							int flags = delta.getFlags();
							if (resource instanceof IContainer && (flags & ResourceChangeListener.INTERESTING_CHANGES) != 0 ||
								resource instanceof IFile && (flags & (ResourceChangeListener.INTERESTING_CHANGES | IResourceDelta.CONTENT)) != 0) {
								modified.add(resource);
							}
						}
						
						return true;
					}
				}, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);

				// reset statuses only for changed resources
				IResource []resources = modified.toArray(new IResource[modified.size()]);
				SVNRemoteStorage.instance().refreshLocalResources(resources, IResource.DEPTH_INFINITE);
				
				// but notify including parents
				SVNRemoteStorage.instance().fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(FileUtility.getPathNodes(resources), IResource.DEPTH_ZERO, ResourceStatesChangedEvent.PATH_NODES));
				SVNRemoteStorage.instance().fireResourceStatesChangedEvent(new ResourceStatesChangedEvent(resources, IResource.DEPTH_ZERO, ResourceStatesChangedEvent.CHANGED_NODES));
			}
		});
	}

	public void handleInitialWorkspaceDelta() throws CoreException {
	    // We register a save participant so we can get the delta from the workbench startup to plugin startup.
		ISavedState ss = ResourcesPlugin.getWorkspace().addSaveParticipant(SVNTeamPlugin.instance(), this);
		if (ss != null) {
			ss.processResourceChangeEvents(this);
		}
	}

	public void doneSaving(ISaveContext context) {

	}

	public void prepareToSave(ISaveContext context) throws CoreException {

	}

	public void rollback(ISaveContext context) {

	}

	public void saving(ISaveContext context) throws CoreException {
		context.needDelta();
	}

}
