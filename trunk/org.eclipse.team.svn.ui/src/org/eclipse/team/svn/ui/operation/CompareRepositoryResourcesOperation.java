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

package org.eclipse.team.svn.ui.operation;

import java.text.MessageFormat;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.client.ISVNClientWrapper;
import org.eclipse.team.svn.core.client.Status;
import org.eclipse.team.svn.core.operation.AbstractNonLockingOperation;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.compare.TwoWayResourceCompareInput;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Two way compare for repository resources operation implementation
 * 
 * @author Alexander Gurov
 */
public class CompareRepositoryResourcesOperation extends AbstractNonLockingOperation {
	protected IRepositoryResource left;
	protected IRepositoryResource right;
	protected IRepositoryResourceProvider provider;

	public CompareRepositoryResourcesOperation(IRepositoryResource left, IRepositoryResource right) {
		super("Operation.CompareRepository");
		this.left = left;
		this.right = right;
	}
	
	public CompareRepositoryResourcesOperation(IRepositoryResourceProvider provider) {
		this(null, null);
		this.provider = provider;
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (this.provider != null) {
			IRepositoryResource []toCompare = provider.getRepositoryResources(); 
			this.left = toCompare[0];
			this.right = toCompare[1];
		}
		
    	long rev1 = this.left.getRevision();
    	long rev2 = this.right.getRevision();
    	if (rev2 > rev1) {
    		IRepositoryResource tmp = this.left;
    		this.left = this.right;
    		this.right = tmp;
    	}
    	
		IRepositoryLocation location = this.left.getRepositoryLocation();
		final ISVNClientWrapper proxy = location.acquireSVNProxy();
		final Status [][]statuses = new Status[1][];
		
		this.protectStep(new IUnprotectedOperation() {
			public void run(IProgressMonitor monitor) throws Exception {
				statuses[0] = proxy.diffStatus(
						SVNUtility.encodeURL(CompareRepositoryResourcesOperation.this.right.getUrl()), CompareRepositoryResourcesOperation.this.right.getPegRevision(), CompareRepositoryResourcesOperation.this.right.getSelectedRevision(), 
						SVNUtility.encodeURL(CompareRepositoryResourcesOperation.this.left.getUrl()), CompareRepositoryResourcesOperation.this.left.getPegRevision(), CompareRepositoryResourcesOperation.this.left.getSelectedRevision(),
						true, false, new SVNProgressMonitor(CompareRepositoryResourcesOperation.this, monitor, null, false));
			}
		}, monitor, 2);
		
		location.releaseSVNProxy(proxy);
		
		if (statuses[0] != null && !monitor.isCanceled()) {
			this.protectStep(new IUnprotectedOperation() {
				public void run(IProgressMonitor monitor) throws Exception {
					CompareConfiguration cc = new CompareConfiguration();
					cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.FALSE);
					final TwoWayResourceCompareInput compare = new TwoWayResourceCompareInput(cc, CompareRepositoryResourcesOperation.this.left, CompareRepositoryResourcesOperation.this.right, statuses[0]);
					compare.initialize(monitor);
					UIMonitorUtility.getDisplay().syncExec(new Runnable() {
						public void run() {
							CompareUI.openCompareEditor(compare);
						}
					});
				}
			}, monitor, 2);
		}
	}
	
    protected String getShortErrorMessage(Throwable t) {
		return MessageFormat.format(super.getShortErrorMessage(t), new String[] {this.left.getName(), this.right.getName()});
	}

}
