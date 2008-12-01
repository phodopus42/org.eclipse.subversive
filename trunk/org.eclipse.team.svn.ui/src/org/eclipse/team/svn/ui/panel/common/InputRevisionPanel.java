/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.RevisionComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;

/**
 * Panel to type or select one of existent revisions
 * 
 * @author Sergiy Logvin
 */
public class InputRevisionPanel extends AbstractDialogPanel {
	
	protected IRepositoryResource resource;
	protected SVNRevision selectedRevision;
	protected RevisionComposite revComposite;
	
	public InputRevisionPanel(IRepositoryResource resource, String dialogTitle) {
		super();
		this.resource = resource;
		this.dialogTitle = dialogTitle;
		this.dialogDescription = SVNUIMessages.InputRevisionPanel_Description;
		this.defaultMessage = SVNUIMessages.InputRevisionPanel_Message;
	}
	
	public SVNRevision getSelectedRevision() {
		return this.selectedRevision;
	}
	
	protected void createControlsImpl(Composite parent) {
		this.revComposite = new RevisionComposite(parent, this, false,  new String [] {SVNUIMessages.InputRevisionPanel_Caption_First, SVNUIMessages.InputRevisionPanel_Caption_Second}, SVNRevision.HEAD, false);
		this.revComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.revComposite.setSelectedResource(this.resource);
	}
	
	public String getHelpId() {
    	return "org.eclipse.team.svn.help.revisionDialogContext";
	}
	
	protected void saveChangesImpl() {
		this.resource = this.revComposite.getSelectedResource();
		this.selectedRevision = this.revComposite.getSelectedRevision();
	}

	protected void cancelChangesImpl() {
	}

}
