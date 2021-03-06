/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.remote;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.svn.core.connector.ISVNConnector;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.composite.BranchTagSelectionComposite;
import org.eclipse.team.svn.ui.composite.DiffFormatComposite;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.utility.InputHistory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.verifier.AbstractVerifier;

/**
 * Panel for the Compare With Branch/Tag dialog
 * 
 * @author Alexei Goncharov
 */
public class CompareBranchTagPanel extends AbstractDialogPanel {
	protected IRepositoryResource baseResource;
	protected int type;
	protected IRepositoryResource[] branchTagResources;
	protected long currentRevision;
	protected String historyKey;
	protected BranchTagSelectionComposite selectionComposite;
	protected DiffFormatComposite diffFormatComposite;
	protected Label resultText;
	protected long options;
	protected Button ignoreAncestryButton;
	protected InputHistory ignoreHistory;
	
	protected IRepositoryResource resourceToCompareWith;
	
	public CompareBranchTagPanel(IRepositoryResource baseResource, int type, IRepositoryResource[] branchTagResources) {
		super();
		this.baseResource = baseResource;
		this.type = type;
		this.branchTagResources = branchTagResources;
		if (type == BranchTagSelectionComposite.BRANCH_OPERATED) {
			this.dialogTitle = SVNUIMessages.Compare_Branch_Title;
			this.dialogDescription = SVNUIMessages.Compare_Branch_Description;
			this.defaultMessage = SVNUIMessages.Compare_Branch_Message;
			this.historyKey = "branchCompare"; //$NON-NLS-1$
		}
		else {
			this.dialogTitle = SVNUIMessages.Compare_Tag_Title;
			this.dialogDescription = SVNUIMessages.Compare_Tag_Description;
			this.defaultMessage = SVNUIMessages.Compare_Tag_Message;
			this.historyKey = "tagCompare"; //$NON-NLS-1$
		}
	}
	
	protected void createControlsImpl(Composite parent) {
        GridData data = null;
        this.selectionComposite = new BranchTagSelectionComposite(parent, SWT.NONE, this.baseResource, this.historyKey, this, this.type, this.branchTagResources);
        data = new GridData(GridData.FILL_HORIZONTAL);
        this.selectionComposite.setLayoutData(data);
        this.selectionComposite.setCurrentRevision(this.currentRevision);
        
		data = new GridData();
        this.ignoreAncestryButton = new Button(parent, SWT.CHECK);
        this.ignoreAncestryButton.setLayoutData(data);
        this.ignoreAncestryButton.setText(SVNUIMessages.MergePanel_Button_IgnoreAncestry);
        this.ignoreHistory = new InputHistory("ignoreAncestry", InputHistory.TYPE_BOOLEAN, (this.options & ISVNConnector.Options.IGNORE_ANCESTRY) != 0); //$NON-NLS-1$
        this.ignoreAncestryButton.setSelection((Boolean)this.ignoreHistory.getValue());
        
        this.diffFormatComposite = new DiffFormatComposite(parent, this);
                       
        Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label label = new Label(parent, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        label.setLayoutData(data);
        label.setText(SVNUIMessages.CompareBranchTagPanel_ResultDescription);
        
        Composite resultComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 2;
		resultComposite.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		resultComposite.setLayoutData(data);		
		resultComposite.setBackground(UIMonitorUtility.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		this.resultText = new Label(resultComposite, SWT.SINGLE | SWT.WRAP);
		this.resultText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		this.resultText.setBackground(UIMonitorUtility.getDisplay().getSystemColor(SWT.COLOR_WHITE));		                    
		
        this.selectionComposite.addUrlModifyListener(new Listener() {
			public void handleEvent(Event event) {
				CompareBranchTagPanel.this.setResultLabel();
			}
		});
        this.selectionComposite.addUrlVerifier(new AbstractVerifier() {
			protected String getErrorMessage(Control input) {
				/*
				 * As resourceToCompareWith may be not yet re-calculated, we do it explicitly here 
				 */
				if (BranchTagSelectionComposite.getResourceToCompareWith(CompareBranchTagPanel.this.baseResource, CompareBranchTagPanel.this.getSelectedResource()) == null) {
					return SVNUIMessages.CompareBranchTagPanel_ConstructResultVerifierError;
				}
				return null;
			}
			protected String getWarningMessage(Control input) {				
				return null;
			}        	
        });
        
        this.setResultLabel();
	}
	
	protected void setResultLabel() {
		String text = SVNUIMessages.CompareBranchTagPanel_ResultNone;
		this.resourceToCompareWith = null;
		
		if (this.getSelectedResource() != null) {
			this.resourceToCompareWith = BranchTagSelectionComposite.getResourceToCompareWith(this.baseResource, this.getSelectedResource());			
			if (this.resourceToCompareWith != null) {				
				text = this.resourceToCompareWith.getUrl();
			}
		}	
		this.resultText.setText(text);	
	}	
	
	public String getDiffFile() {			
		return this.diffFormatComposite.getDiffFile();
	}
	
	public IRepositoryResource getResourceToCompareWith() {
		return this.resourceToCompareWith;
	}
	
	private IRepositoryResource getSelectedResource() {
		return this.selectionComposite.getSelectedResource();
	}

	public long getDiffOptions() {
		return this.options;
	}
	
	protected void saveChangesImpl() {
		this.selectionComposite.saveChanges();
    	this.options |= this.ignoreAncestryButton.getSelection() ? ISVNConnector.Options.IGNORE_ANCESTRY : ISVNConnector.Options.NONE;
    	this.ignoreHistory.setValue(this.ignoreAncestryButton.getSelection());
	}
	
	protected void cancelChangesImpl() {
	}
	
}
