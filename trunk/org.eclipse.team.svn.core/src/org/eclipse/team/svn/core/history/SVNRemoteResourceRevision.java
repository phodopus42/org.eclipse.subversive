/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.history;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.svn.core.client.LogMessage;
import org.eclipse.team.svn.core.resource.IRepositoryResource;

/**
 * Resource revision based on the resource history
 * 
 * @author Alexander Gurov
 */
public class SVNRemoteResourceRevision extends FileRevision {
	protected LogMessage msg;
	protected IRepositoryResource remote;
	protected boolean isDeletionRev;

	public SVNRemoteResourceRevision(IRepositoryResource remote, LogMessage msg) {
		this.remote = remote;
		this.msg = msg;
		if (this.msg.changedPaths != null) {
			for (int i = 0; i < this.msg.changedPaths.length && !this.isDeletionRev; i++) {
				if (this.msg.changedPaths[i].action == 'D' && this.remote.getUrl().endsWith(this.msg.changedPaths[i].path)) {
					this.isDeletionRev = true;
				}
			}
		}
	}

	public URI getURI() {
		try {
			return new URI(this.remote.getUrl());
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	public long getTimestamp() {
		return this.msg.date == 0 ? -1 : this.msg.date;
	}

	public boolean exists() {
		return !this.isDeletionRev;
	}

	public String getContentIdentifier() {
		return String.valueOf(this.msg.revision);
	}

	public String getAuthor() {
		return this.msg.author;
	}

	public String getComment() {
		return this.msg.message;
	}
	
	public String getName() {
		return this.remote.getName();
	}

	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		ResourceContentStorage retVal = new ResourceContentStorage(this.remote);
		retVal.fetchContents(monitor);
		return retVal;
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		return this;
	}

}
