/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.revision.graph.graphic.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.revision.graph.graphic.RevisionGraphEditor;
import org.eclipse.team.svn.revision.graph.graphic.RevisionNode;
import org.eclipse.team.svn.revision.graph.graphic.editpart.RevisionEditPart;
import org.eclipse.team.svn.revision.graph.graphic.editpart.RevisionGraphEditPart;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * 
 * @author Igor Burilo
 */
public class RevisionGraphContextMenuManager extends ContextMenuProvider {

	public static final String GROUP_OPEN = "open";
	public static final String GROUP_COMPARE = "compare";
	public static final String GROUP_1 = "group1";
	public static final String GROUP_2 = "group2";
	public static final String GROUP_3 = "group3";
	public static final String GROUP_EXPAND_COLLAPSE = "expandCollapse";
	
	protected ActionRegistry actionRegistry;
	protected RevisionGraphEditor graphEditor;
	
	public RevisionGraphContextMenuManager(EditPartViewer viewer, RevisionGraphEditor graphEditor, ActionRegistry actionRegistry) {
		super(viewer);
		this.actionRegistry = actionRegistry;
		this.graphEditor = graphEditor;
	}

	@SuppressWarnings("unchecked")
	@Override	
	public void buildContextMenu(IMenuManager menu) {		
		IRepositoryResource graphCalledResource = ((RevisionGraphEditPart) this.getViewer().getContents()).getCastedModel().getRepositoryResource();
		boolean isGraphCalledOnFile = graphCalledResource instanceof IRepositoryFile;						
		
		if (isGraphCalledOnFile) {
			menu.add(new Separator(RevisionGraphContextMenuManager.GROUP_OPEN));	
		}		
		menu.add(new Separator(RevisionGraphContextMenuManager.GROUP_COMPARE));
		menu.add(new Separator(RevisionGraphContextMenuManager.GROUP_1));
		menu.add(new Separator(RevisionGraphContextMenuManager.GROUP_2));
		menu.add(new Separator(RevisionGraphContextMenuManager.GROUP_3));
		menu.add(new Separator(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	
		List<EditPart> editParts = this.getViewer().getSelectedEditParts();
		List<RevisionEditPart> revisionEditParts = new ArrayList<RevisionEditPart>();
		for (EditPart editPart : editParts) {
			if (editPart instanceof RevisionEditPart) {
				revisionEditParts.add((RevisionEditPart) editPart);	
			}			
		}
		
		if (isGraphCalledOnFile) {	
			String resourceName = null;
			if (revisionEditParts.size() == 1) {
				IRepositoryResource selectedResource = BaseRevisionGraphAction.convertToResource(revisionEditParts.get(0));
				resourceName = selectedResource.getName();
			}						
			
			IAction action = this.actionRegistry.getAction(OpenAction.OpenAction_ID);
			menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_OPEN, action);
			action.setImageDescriptor(resourceName != null ? SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry().getImageDescriptor(resourceName) : null);

			
			//---- open with
						
			MenuManager sub = new MenuManager(SVNUIMessages.HistoryView_OpenWith, "graphOpenWithMenu");
			sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			
			sub.add(new Separator("nonDefaultTextEditors"));
			if (resourceName != null) {
				IEditorDescriptor[] editors = SVNTeamUIPlugin.instance().getWorkbench().getEditorRegistry().getEditors(resourceName);
				for (int i = 0; i < editors.length; i++) {
					if (!editors[i].getId().equals(EditorsUI.DEFAULT_TEXT_EDITOR_ID)) {
						this.addMenuItem(this.getViewer(), sub, editors[i].getLabel(), new OpenFileWithAction(this.graphEditor, editors[i].getId(), false));
					}
				}	
			}			
			
			sub.add(new Separator("variousEditors")); //$NON-NLS-1$
			this.addMenuItem(this.getViewer(), sub, SVNUIMessages.HistoryView_TextEditor, new OpenFileWithAction(this.graphEditor));
			this.addMenuItem(this.getViewer(), sub, SVNUIMessages.HistoryView_SystemEditor, new OpenFileWithExternalAction(this.graphEditor));
			this.addMenuItem(this.getViewer(), sub, SVNUIMessages.HistoryView_InplaceEditor, new OpenFileWithInplaceAction(this.graphEditor));
			this.addMenuItem(this.getViewer(), sub, SVNUIMessages.HistoryView_DefaultEditor, new OpenFileAction(this.graphEditor));
			
			menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_OPEN, sub);
		}
		
		IAction action = this.actionRegistry.getAction(CompareWithEachOtherAction.CompareWithEachOtherAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_COMPARE, action);
		
		action = this.actionRegistry.getAction(CompareWithHeadAction.CompareWithHeadAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_COMPARE, action);
		
		action = this.actionRegistry.getAction(CompareWithPreviousAction.CompareWithPreviousAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_COMPARE, action);	
		
		action = this.actionRegistry.getAction(ShowHistoryAction.ShowHistoryAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_1, action);
		
		action = this.actionRegistry.getAction(ShowPropertiesAction.ShowPropertiesAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_1, action);
		
		action = this.actionRegistry.getAction(ComparePropertiesAction.ComparePropertiesAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_1, action);	
		
		if (isGraphCalledOnFile) {
			action = this.actionRegistry.getAction(ShowAnnotationAction.ShowAnnotationAction_ID);
			menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_1, action);		
		}		
		
		action = this.actionRegistry.getAction(ExportAction.ExportAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_2, action);
		
		action = this.actionRegistry.getAction(CreatePatchAction.CreatePatchAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_2, action);
		
		action = this.actionRegistry.getAction(ExtractAction.ExtractAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_2, action);
				
		action = this.actionRegistry.getAction(CreateBranchTagAction.CreateBranchAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_3, action);
		
		action = this.actionRegistry.getAction(CreateBranchTagAction.CreateTagAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_3, action);
		
		action = this.actionRegistry.getAction(AddRevisionLinksAction.AddRevisionLinksAction_ID);
		menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_3, action);
		
		this.addExpandCollapseActions(menu, revisionEditParts);		
	}
	
	protected void addExpandCollapseActions(IMenuManager menu, List<RevisionEditPart> revisionEditParts) {
		if (revisionEditParts.size() == 1) {
			RevisionNode node = revisionEditParts.get(0).getCastedModel();
			
			if (node.isNextCollapsed()) {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.ExpandNextAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			} else {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.CollapseNextAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			}
			
			if (node.isPreviousCollapsed()) {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.ExpandPreviousAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			} else {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.CollapsePreviousAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			}
			
			if (node.isCopiedToCollapsed()) {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.ExpandCopiedToAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			} else {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.CollapseCopiedToAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			}
			
			if (node.isCopiedFromCollapsed()) {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.ExpandCopiedFromAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			} else {
				IAction action = this.actionRegistry.getAction(CollapseExpandAction.CollapseCopiedFromAction_ID);
				menu.appendToGroup(RevisionGraphContextMenuManager.GROUP_EXPAND_COLLAPSE, action);
			}
		}
	}

	protected Action addMenuItem(EditPartViewer viewer, MenuManager menuManager, String label, final SelectionAction action) {
		action.setText(label);
		action.setToolTipText(label);
		
		menuManager.add(action);
		
		action.update();				
		return action;		
	}

}