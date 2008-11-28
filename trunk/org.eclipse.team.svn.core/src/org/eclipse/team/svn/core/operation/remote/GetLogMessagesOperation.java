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

package org.eclipse.team.svn.core.operation.remote;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Get repository resource LogMessage's opearation implementation
 * 
 * @author Alexander Gurov
 */
public class GetLogMessagesOperation extends AbstractRepositoryOperation {
	protected SVNLogEntry []msg;
	protected boolean stopOnCopy;
	protected boolean discoverPaths;
	protected SVNRevision startRevision;
	protected SVNRevision endRevision;
	protected long limit;
	protected boolean includeMerged;
	
	public GetLogMessagesOperation(IRepositoryResource resource) {
		this(resource, false);
	}
	
	public GetLogMessagesOperation(IRepositoryResource resource, boolean stopOnCopy) {
		super("Operation_GetLogMessages", new IRepositoryResource[] {resource}); //$NON-NLS-1$
		this.stopOnCopy = stopOnCopy;
		this.includeMerged = false;
		this.discoverPaths = true;
		this.limit = 0;
		this.endRevision = SVNRevision.fromNumber(0); 
	}
	
	public boolean getIncludeMerged() {
		return this.includeMerged;
	}
	
	public void setIncludeMerged(boolean includeMerged) {
		this.includeMerged = includeMerged;
	}
	
	public boolean getStopOnCopy() {
		return this.stopOnCopy;
	}
	
	public void setStopOnCopy(boolean stopOnCopy) {
		this.stopOnCopy = stopOnCopy;
	}
	
	public boolean getDiscoverPaths() {
		return this.discoverPaths;
	}

	public void setDiscoverPaths(boolean discoverPaths) {
		this.discoverPaths = discoverPaths;
	}
	
	public long getLimit() {
		return this.limit;
	}
	
	public void setLimit(long limit) {
		this.limit = limit;
	}
	
	public void setStartRevision(SVNRevision revision) {
		this.startRevision = revision;
	}

	public void setEndRevision(SVNRevision revision) {
		this.endRevision = revision;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IRepositoryResource resource = this.operableData()[0];
		if (this.startRevision == null) {
			this.startRevision = resource.getSelectedRevision();
		}
		IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn log " + SVNUtility.encodeURL(this.resource.getUrl()) + (this.limit != 0 ? (" --limit " + this.limit) : "") + (this.stopOnCopy ? " --stop-on-copy" : "") + " -r " + this.selectedRevision + ":0 --username \"" + location.getUsername() + "\"\n");
			long options = this.discoverPaths ? ISVNConnector.Options.DISCOVER_PATHS : ISVNConnector.Options.NONE;
			options |= this.stopOnCopy ? ISVNConnector.Options.STOP_ON_COPY : ISVNConnector.Options.NONE;
			options |= this.includeMerged ? ISVNConnector.Options.INCLUDE_MERGED_REVISIONS : ISVNConnector.Options.NONE;
			this.msg = SVNUtility.logEntries(proxy, SVNUtility.getEntryReference(resource), this.startRevision, this.endRevision, options, ISVNConnector.DEFAULT_LOG_ENTRY_PROPS, this.limit, new SVNProgressMonitor(this, monitor, null));
		}
		finally {
			location.releaseSVNProxy(proxy);
		}
	}
	
	public SVNLogEntry []getMessages() {
		return this.msg;
	}
	
	public IRepositoryResource getResource() {
		return this.operableData()[0];
	}
	
	protected String getShortErrorMessage(Throwable t) {
		return MessageFormat.format(super.getShortErrorMessage(t), new Object[] {this.operableData()[0].getUrl()});
	}

}
