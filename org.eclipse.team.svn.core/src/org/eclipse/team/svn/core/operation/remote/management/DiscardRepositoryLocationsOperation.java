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

package org.eclipse.team.svn.core.operation.remote.management;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * Discard location operation
 * 
 * @author Alexander Gurov
 */
public class DiscardRepositoryLocationsOperation extends AbstractActionOperation {
	protected IRepositoryLocation []locations;
	
	public DiscardRepositoryLocationsOperation(IRepositoryLocation []locations) {
		super("Operation_DiscardRepositoryLocation", SVNMessages.class); //$NON-NLS-1$
		this.locations = locations;
	}
	
	public int getOperationWeight() {
		return 0;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		for (int i = 0; i < this.locations.length; i++) {
			final IRepositoryLocation current = this.locations[i];
			this.protectStep(new IUnprotectedOperation() {
				public void run(IProgressMonitor monitor) throws Exception {
					SVNRemoteStorage.instance().removeRepositoryLocation(current);
				}
			}, monitor, this.locations.length);
		}
	}

}
