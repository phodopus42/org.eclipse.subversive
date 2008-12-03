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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.connector.ISVNAnnotationCallback;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNAnnotationData;
import org.eclipse.team.svn.core.connector.SVNConnectorException;
import org.eclipse.team.svn.core.connector.SVNErrorCodes;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.SVNUtility;

/**
 * Resource annotation operation implementation 
 * 
 * @author Alexander Gurov
 */
public class GetResourceAnnotationOperation extends AbstractRepositoryOperation {
	protected SVNAnnotationData []annotatedLines;
	protected byte []content;
	protected boolean includeMerged;

	public GetResourceAnnotationOperation(IRepositoryResource resource) {
		super("Operation_GetAnnotation", new IRepositoryResource[] {resource}); //$NON-NLS-1$
	}
	
	public boolean getIncludeMerged() {
		return this.includeMerged;
	}
	
	public void setIncludeMerged(boolean includeMerged) {
		this.includeMerged = includeMerged;
	}
	
	public IRepositoryResource getRepositoryResource() {
		return this.operableData()[0];
	}

	public SVNAnnotationData []getAnnotatedLines() {
		return this.annotatedLines;
	}
	
	public byte []getContent() {
		return this.content;
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		final ArrayList<SVNAnnotationData> lines = new ArrayList<SVNAnnotationData>();
		IRepositoryResource resource = this.operableData()[0];
		IRepositoryLocation location = resource.getRepositoryLocation();
		ISVNConnector proxy = location.acquireSVNProxy();
		try {
//			this.writeToConsole(IConsoleStream.LEVEL_CMD, "svn blame " + url + "@" + resource.getPegRevision() + " -r 0:" + resource.getSelectedRevision() + " --username \"" + location.getUsername() + "\"\n");
			long options = ISVNConnector.Options.IGNORE_MIME_TYPE;
			options |= this.includeMerged ? ISVNConnector.Options.INCLUDE_MERGED_REVISIONS : ISVNConnector.Options.NONE;
			proxy.annotate(
				SVNUtility.getEntryReference(resource),
				SVNRevision.fromNumber(0),
				resource.getSelectedRevision(),
				options, new ISVNAnnotationCallback() {
					public void annotate(String line, SVNAnnotationData data) {
						lines.add(data);
						try {
							stream.write((line + "\n").getBytes()); //$NON-NLS-1$
						} 
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}, 
				new SVNProgressMonitor(this, monitor, null)
			);
		}
		finally {
			location.releaseSVNProxy(proxy);
		}
		this.annotatedLines = lines.toArray(new SVNAnnotationData[lines.size()]);
		this.content = stream.toByteArray();
	}
	
	protected String getShortErrorMessage(Throwable t) {
		if (t instanceof SVNConnectorException && ((SVNConnectorException)t).getErrorId() == SVNErrorCodes.clientIsBinaryFile) {
			return this.getOperationResource("Error_IsBinary"); //$NON-NLS-1$
		}
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] {this.operableData()[0].getName()});
	}
	

}
