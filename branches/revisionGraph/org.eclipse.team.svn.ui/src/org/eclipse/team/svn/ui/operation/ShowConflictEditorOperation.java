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

import java.util.HashSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.connector.SVNChangeStatus;
import org.eclipse.team.svn.core.connector.ISVNConnector.Depth;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ExternalProgramParameters;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.DetectExternalCompareOperationHelper;
import org.eclipse.team.svn.core.operation.local.RunExternalCompareOperation.ExternalCompareOperationHelper;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.compare.ComparePanel;
import org.eclipse.team.svn.ui.compare.ConflictingFileEditorInput;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.preferences.SVNTeamDiffViewerPage;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Show conflict editor operation implementation
 * 
 * @author Alexander Gurov
 */
public class ShowConflictEditorOperation extends AbstractWorkingCopyOperation {

	protected boolean showInDialog;
	
	public ShowConflictEditorOperation(IResource []resources, boolean showInDialog) {
		super("Operation_ShowConflictEditor", resources); //$NON-NLS-1$
		this.showInDialog = showInDialog;
	}

	public ShowConflictEditorOperation(IResourceProvider provider, boolean showInDialog) {
		super("Operation_ShowConflictEditor", provider); //$NON-NLS-1$
		this.showInDialog = showInDialog;
	}

    public ISchedulingRule getSchedulingRule() {
    	ISchedulingRule rule = super.getSchedulingRule();
    	if (rule instanceof IWorkspaceRoot) {
    		return rule;
    	}
    	IResource []resources = this.operableData();
    	HashSet<ISchedulingRule> ruleSet = new HashSet<ISchedulingRule>();
    	for (int i = 0; i < resources.length; i++) {
			ruleSet.add(SVNResourceRuleFactory.INSTANCE.refreshRule(resources[i].getParent()));
    	}
    	return new MultiRule(ruleSet.toArray(new IResource[ruleSet.size()]));
    }

    public int getOperationWeight() {
		return 0;
	}
    
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		IResource []conflictingResources = this.operableData();
		
		for (int i = 0; i < conflictingResources.length && !monitor.isCanceled(); i++) {
			final IResource current = conflictingResources[i];
			if (current.getType() == IResource.FILE) {
				this.protectStep(new IUnprotectedOperation() {
					public void run(IProgressMonitor monitor) throws Exception {
						ShowConflictEditorOperation.this.showEditorFor((IFile)current, monitor);
					}
				}, monitor, conflictingResources.length);
			}
		}
	}

	protected void showEditorFor(IFile resource, IProgressMonitor monitor) throws Exception {
		IRemoteStorage storage = SVNRemoteStorage.instance();
		
		IRepositoryLocation location = storage.getRepositoryLocation(resource);
		ISVNConnector proxy = location.acquireSVNProxy();
		
		try {
			SVNChangeStatus []status = SVNUtility.status(proxy, FileUtility.getWorkingCopyPath(resource), Depth.EMPTY, ISVNConnector.Options.NONE, new SVNNullProgressMonitor());
			if (status.length == 1) {
				IContainer parent = resource.getParent();
				parent.refreshLocal(IResource.DEPTH_ONE, monitor);
				/*
				 * If Subversion considers the file to be unmergeable, then the .mine file isn't 
				 * created, since it would be identical to the working file.
				 */
				IFile local = null;
				if (status[0].conflictWorking != null && !"".equals(status[0].conflictWorking)) { //$NON-NLS-1$
					local = parent.getFile(new Path(status[0].conflictWorking));
					if (!local.exists()) {
						local = null;
					}					
				}
				local = local == null ? resource : local;
				
				IFile remote = parent.getFile(new Path(status[0].conflictNew));
				IFile ancestor = parent.getFile(new Path(status[0].conflictOld));
				
				//detect compare editor
				DetectExternalCompareOperationHelper detectCompareEditorHelper = new DetectExternalCompareOperationHelper(resource, SVNTeamDiffViewerPage.loadDiffViewerSettings(), false);
				detectCompareEditorHelper.execute(monitor);
				ExternalProgramParameters externalProgramParams = detectCompareEditorHelper.getExternalProgramParameters();
				if (externalProgramParams != null) {
					this.openExternalEditor(resource, local, remote, ancestor, externalProgramParams, monitor);
				} else {
					this.openEclipseEditor(resource, local, remote, ancestor, monitor);	
				}
			}
		}
		finally {
			location.releaseSVNProxy(proxy);
		}
	}
	
	protected void openExternalEditor(IFile target, IFile left, IFile right, IFile ancestor, ExternalProgramParameters externalProgramParams, IProgressMonitor monitor) throws Exception {
		String targetFile = FileUtility.getWorkingCopyPath(target);
		String oldFile = FileUtility.getWorkingCopyPath(ancestor);
		String workingFile = FileUtility.getWorkingCopyPath(left);
		String newFile = FileUtility.getWorkingCopyPath(right);				
		
		ExternalCompareOperationHelper compareRunner = new ExternalCompareOperationHelper(oldFile, workingFile, newFile, targetFile, externalProgramParams, false);
		compareRunner.execute(monitor);
	}

	protected void openEclipseEditor(final IFile target, IFile left, IFile right, IFile ancestor, IProgressMonitor monitor) throws Exception {
		CompareConfiguration cc = new CompareConfiguration();
		cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.TRUE);
		final ConflictingFileEditorInput compare = new ConflictingFileEditorInput(cc, target, left, right, ancestor);
		compare.run(monitor);
		UIMonitorUtility.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (ShowConflictEditorOperation.this.showInDialog) {
					ComparePanel panel = new ComparePanel(compare, target);
					DefaultDialog dlg = new DefaultDialog(UIMonitorUtility.getShell(), panel);
					dlg.open();
					//CompareUI.openCompareDialog(compare);
				}
				else {
					CompareUI.openCompareEditor(compare);
				}
			}
		});
	}
	
}