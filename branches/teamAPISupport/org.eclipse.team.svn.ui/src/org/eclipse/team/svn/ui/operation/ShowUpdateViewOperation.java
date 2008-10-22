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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.svn.core.operation.local.AbstractWorkingCopyOperation;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.synchronize.UpdateSubscriber;
import org.eclipse.team.svn.ui.mapping.ModelHelper;
import org.eclipse.team.svn.ui.mapping.UpdateModelParticipant;
import org.eclipse.team.svn.ui.mapping.UpdateSubscriberContext;
import org.eclipse.team.svn.ui.synchronize.update.UpdateParticipant;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.ResourceScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Synchronize workspace resources operation
 * 
 * @author Alexander Gurov
 */
public class ShowUpdateViewOperation extends AbstractWorkingCopyOperation {
	protected IWorkbenchPart part;
	protected ISynchronizeScope scope;
	protected ResourceMapping[] resourcesMapping;
	
	public ShowUpdateViewOperation(ISynchronizeScope scope, IWorkbenchPart part) {
		super("Operation.ShowUpdateView", (IResource [])null);
		this.part = part;
		this.scope = scope;
	}

	public ShowUpdateViewOperation(IResource []resources, IWorkbenchPart part) {
		super("Operation.ShowUpdateView", resources);
		this.part = part;
	}

	public ShowUpdateViewOperation(IResourceProvider provider, IWorkbenchPart part) {
		super("Operation.ShowUpdateView", provider);
		this.part = part;
	}
	
	public ShowUpdateViewOperation(ResourceMapping[] resourcesMapping, IWorkbenchPart part) {
		super("Operation.ShowUpdateView", (IResource [])null);
		this.part = part;
		this.resourcesMapping = resourcesMapping;
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}
	
	public int getOperationWeight() {
		return 0;
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		if (ModelHelper.isShowModelSync()) {
			//TODO
			/*
			ResourceMapping[] mappings = getCVSResourceMappings();
			if (mappings.length == 0)
				return;
			SubscriberScopeManager manager = WorkspaceSubscriberContext.createWorkspaceScopeManager(mappings, true, CommitAction.isIncludeChangeSets(getShell(), CVSUIMessages.SyncAction_1));
			WorkspaceSubscriberContext context = WorkspaceSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY);
			WorkspaceModelParticipant participant = new WorkspaceModelParticipant(context);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			participant.run(getTargetPart());
			*/
			
			
			ResourceMapping[] mappings = this.getResourcesMapping();
			if (mappings.length == 0)
				return;					
			
			boolean consultModels = true;
			//TODO see also ChangeSetSubscriberScopeManager
			SubscriberScopeManager manager = UpdateSubscriberContext.createWorkspaceScopeManager(mappings, consultModels);
												
			UpdateSubscriberContext context = UpdateSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY);
			//context.initialize();
			
			UpdateModelParticipant participant = new UpdateModelParticipant(context);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			participant.run(this.part);
		} else {
			IResource []resources;
			if (this.scope == null) {
				resources = this.operableData();
				this.scope = new ResourceScope(resources);
			}
			else {
				resources = this.scope.getRoots();
			}
			
			UpdateParticipant participant = null;
			if (resources != null) {
				participant = (UpdateParticipant)SubscriberParticipant.getMatchingParticipant(UpdateParticipant.PARTICIPANT_ID, resources);
			}
			if (participant == null) {
				participant = new UpdateParticipant(this.scope);
				TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			}
			participant.run(this.part);
		}
	}
	
	//TODO add correct impl
	protected ResourceMapping[] getResourcesMapping() {
		/*
		 * 
		 * 1. see ModelSynchronizeWizard
		 * 		GlobalRefreshElementSelectionPage selectionPage
		 * 				ISynchronizeParticipant participant = createParticipant(
		 *				((ModelElementSelectionPage)selectionPage).getSelectedMappings());
		 * 2. see WorkspaceTraversalAction
		 * 		protected ResourceMapping[] getCVSResourceMappings() {
		 */
		return this.resourcesMapping;
	}
	
//	protected void runImpl(IProgressMonitor monitor) throws Exception {
//		IResource []resources;
//		if (this.scope == null) {
//			resources = this.operableData();
//			this.scope = new ResourceScope(resources);
//		}
//		else {
//			resources = this.scope.getRoots();
//		}
//		
//		UpdateParticipant participant = null;
//		if (resources != null) {
//			participant = (UpdateParticipant)SubscriberParticipant.getMatchingParticipant(UpdateParticipant.PARTICIPANT_ID, resources);
//		}
//		if (participant == null) {
//			participant = new UpdateParticipant(this.scope);
//			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
//		}
//		participant.run(this.part);
//	}

}
