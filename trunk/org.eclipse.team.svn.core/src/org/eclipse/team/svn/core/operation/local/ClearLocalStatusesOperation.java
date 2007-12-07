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

package org.eclipse.team.svn.core.operation.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;

/**
 * This operation clear local statuses cache in the IRemoteStorage
 * 
 * @author Alexander Gurov
 */
public class ClearLocalStatusesOperation extends AbstractWorkingCopyOperation {
	public ClearLocalStatusesOperation(IResource[] resources) {
		super("Operation.ClearLocalStatuses", resources);
	}

	public ClearLocalStatusesOperation(IResourceProvider provider) {
		super("Operation.ClearLocalStatuses", provider);
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		SVNRemoteStorage.instance().refreshLocalResources(this.operableData(), IResource.DEPTH_INFINITE);
		ProgressMonitorUtility.progress(monitor, 1, 1);
	}
	
}
