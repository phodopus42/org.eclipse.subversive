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

package org.eclipse.team.svn.core.svnstorage;

import java.io.Serializable;

import org.eclipse.team.svn.core.client.ClientWrapperException;
import org.eclipse.team.svn.core.client.Depth;
import org.eclipse.team.svn.core.client.EntryRevisionReference;
import org.eclipse.team.svn.core.client.RepositoryEntry;
import org.eclipse.team.svn.core.client.ISVNClientWrapper;
import org.eclipse.team.svn.core.client.EntryInfo;
import org.eclipse.team.svn.core.client.NodeKind;
import org.eclipse.team.svn.core.client.PropertyData;
import org.eclipse.team.svn.core.client.Revision;
import org.eclipse.team.svn.core.client.RepositoryEntry.Fields;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * SVN based representation of IRepositoryContainer
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryContainer extends SVNRepositoryResource implements IRepositoryContainer, Serializable {
	private static final long serialVersionUID = -6380931196819185798L;

	protected transient IRepositoryResource []children;

	// serialization conventional constructor
	protected SVNRepositoryContainer() {
		super();
	}

	public SVNRepositoryContainer(IRepositoryLocation location, String url, Revision selectedRevision) {
		super(location, url, selectedRevision);
	}
	
	public boolean isChildrenCached() {
		return this.children != null;
	}
	
	public synchronized void refresh() {
		super.refresh();
		this.children = null;
	}
	
	public IRepositoryResource []getChildren() throws ClientWrapperException {
		IRepositoryResource []retVal = this.children;
		
		// synchronize only assignment in order to avoid deadlock with this Sync and UI Sync locked from callback
		//	in result we can perform excessive work but it is acceptable in that case
		if (retVal == null) {
			String thisUrl = this.getUrl();
			RepositoryEntry []children = null;
			
			ISVNClientWrapper proxy = this.getRepositoryLocation().acquireSVNProxy();
			try {
				children = SVNUtility.list(proxy, SVNUtility.getEntryRevisionReference(this), Depth.IMMEDIATES, Fields.ALL, true, new SVNNullProgressMonitor());
			}
			finally {
			    this.getRepositoryLocation().releaseSVNProxy(proxy);
			}
			
			synchronized (this) {
				retVal = new IRepositoryResource[children.length];
				
				for (int i = 0; i < children.length; i++) {
					String childUrl = thisUrl + "/" + children[i].path;
					SVNRepositoryResource resource = children[i].nodeKind == NodeKind.DIR ? (SVNRepositoryResource)this.asRepositoryContainer(childUrl, false) : (SVNRepositoryResource)this.asRepositoryFile(childUrl, false);
					resource.setRevision(children[i].revision);
					resource.setInfo(new IRepositoryResource.Information(children[i].lock, children[i].size, children[i].author, children[i].date, children[i].hasProperties));
					retVal[i] = resource;
				}
				
				this.children = retVal;
			}
		}
		
		return retVal;
	}
	
	protected void getRevisionImpl(ISVNClientWrapper proxy) throws ClientWrapperException {
		EntryRevisionReference reference = SVNUtility.getEntryRevisionReference(this);
		EntryInfo []infos = SVNUtility.info(proxy, reference, Depth.EMPTY, new SVNNullProgressMonitor());
		if (infos != null && infos.length > 0 && infos[0].lastChangedRevision != Revision.SVN_INVALID_REVNUM) {
			this.lastRevision = (Revision.Number)Revision.fromNumber(infos[0].lastChangedRevision);
			PropertyData []data = SVNUtility.properties(proxy, reference, new SVNNullProgressMonitor());
			this.setInfo(new IRepositoryResource.Information(infos[0].lock, 0, infos[0].lastChangedAuthor, infos[0].lastChangedDate, data != null && data.length > 0));
		}
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IRepositoryContainer)) {
			return false;
		}
		return super.equals(obj);
	}
	
}
