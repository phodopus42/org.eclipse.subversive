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

package org.eclipse.team.svn.ui.operation;

import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.BaseMessages;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNDepth;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntryRevisionReference;
import org.eclipse.team.svn.core.connector.SVNRevisionRange;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.compare.ResourceCompareInput;
import org.eclipse.team.svn.ui.compare.TwoWayResourceCompareInput;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Two way compare for repository resources operation implementation
 * 
 * @author Alexander Gurov
 */
public class CompareRepositoryResourcesInernalOperation extends AbstractActionOperation {
	protected IRepositoryResource next;
	protected IRepositoryResource prev;
	protected IRepositoryResourceProvider provider;
	protected boolean forceReuse;
	protected String forceId;
	protected long options;

	public CompareRepositoryResourcesInernalOperation(IRepositoryResource prev, IRepositoryResource next, boolean forceReuse, long options) {
		super("Operation_CompareRepository", SVNUIMessages.class); //$NON-NLS-1$
		this.prev = prev;
		this.next = next;
		this.forceReuse = forceReuse;
		this.options = options;
	}
	
	public CompareRepositoryResourcesInernalOperation(IRepositoryResourceProvider provider, boolean forceReuse, long options) {
		this(null, null, forceReuse, options);
		this.provider = provider;
	}

	public void setForceId(String forceId) {
		this.forceId = forceId;
	}

	public String getForceId() {
		return this.forceId;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (this.provider != null) {
			IRepositoryResource []toCompare = this.provider.getRepositoryResources(); 
			this.prev = toCompare[0];
			this.next = toCompare[1];
		}
		
		IRepositoryLocation location = this.prev.getRepositoryLocation();
		final ISVNConnector proxy = location.acquireSVNProxy();
		final ArrayList<SVNDiffStatus> statuses = new ArrayList<SVNDiffStatus>();
		
		ProgressMonitorUtility.setTaskInfo(monitor, this, SVNMessages.Progress_Running);
		this.protectStep(new IUnprotectedOperation() {
			public void run(IProgressMonitor monitor) throws Exception {
				SVNEntryRevisionReference refPrev = SVNUtility.getEntryRevisionReference(CompareRepositoryResourcesInernalOperation.this.prev);
				SVNEntryRevisionReference refNext = SVNUtility.getEntryRevisionReference(CompareRepositoryResourcesInernalOperation.this.next);
				if (SVNUtility.useSingleReferenceSignature(refPrev, refNext)) {
					SVNUtility.diffStatus(proxy, statuses, refPrev, new SVNRevisionRange(refPrev.revision, refNext.revision), SVNDepth.INFINITY, CompareRepositoryResourcesInernalOperation.this.options, new SVNProgressMonitor(CompareRepositoryResourcesInernalOperation.this, monitor, null, false));
				}
				else {
					SVNUtility.diffStatus(proxy, statuses, refPrev, refNext, SVNDepth.INFINITY, CompareRepositoryResourcesInernalOperation.this.options, new SVNProgressMonitor(CompareRepositoryResourcesInernalOperation.this, monitor, null, false));
				}
			}
		}, monitor, 100, 20);
		
		location.releaseSVNProxy(proxy);
		
		if (!monitor.isCanceled()) {
			this.protectStep(new IUnprotectedOperation() {
				public void run(IProgressMonitor monitor) throws Exception {
					CompareConfiguration cc = new CompareConfiguration();
					cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.FALSE);
					final TwoWayResourceCompareInput compare = new TwoWayResourceCompareInput(cc, CompareRepositoryResourcesInernalOperation.this.next, CompareRepositoryResourcesInernalOperation.this.prev, statuses);
					compare.setForceId(CompareRepositoryResourcesInernalOperation.this.forceId);
					compare.initialize(monitor);
					UIMonitorUtility.getDisplay().syncExec(new Runnable() {
						public void run() {
							ResourceCompareInput.openCompareEditor(compare, CompareRepositoryResourcesInernalOperation.this.forceReuse);
						}
					});
				}
			}, monitor, 100, 20);
		}
	}
	
    protected String getShortErrorMessage(Throwable t) {
		return BaseMessages.format(super.getShortErrorMessage(t), new Object[] {this.next.getName(), this.prev.getName()});
	}

}
