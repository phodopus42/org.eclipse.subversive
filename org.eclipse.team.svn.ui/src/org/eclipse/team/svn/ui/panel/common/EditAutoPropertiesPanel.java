/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexey Mikoyan - Initial implementation
 *    Gabor Liptak - Speedup Pattern's usage
 *******************************************************************************/

package org.eclipse.team.svn.ui.panel.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPropsPreferencePage;
import org.eclipse.team.svn.ui.verifier.AbstractVerifierProxy;
import org.eclipse.team.svn.ui.verifier.CompositeVerifier;
import org.eclipse.team.svn.ui.verifier.FileNameTemplateVerifier;
import org.eclipse.team.svn.ui.verifier.MultiLinePropertyVerifier;
import org.eclipse.team.svn.ui.verifier.NonEmptyFieldVerifier;

/**
 * Edit automatic properties panel
 *
 * @author Alexey Mikoyan
 *
 */
public class EditAutoPropertiesPanel extends AbstractDialogPanel {

	protected SVNTeamPropsPreferencePage.AutoProperty property;
	protected Text txtFileName;
	protected Text txtProperties;
	protected String fileName;
	protected String properties;
	
	public EditAutoPropertiesPanel(SVNTeamPropsPreferencePage.AutoProperty property) {
		super();
		this.property = property;
		this.dialogTitle = property == null ? SVNUIMessages.EditAutoPropertiesPanel_Title_Add : SVNUIMessages.EditAutoPropertiesPanel_Title_Edit;
		this.dialogDescription = SVNUIMessages.EditAutoPropertiesPanel_Description;
		this.defaultMessage = SVNUIMessages.EditAutoPropertiesPanel_Message;
	}
	
	public void createControlsImpl(Composite parent) {
		GridLayout layout;
		GridData layoutData;
		Label label;
		
		Composite composite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginBottom = 5;
		composite.setLayout(layout);
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(layoutData);
		
		label = new Label(composite, SWT.NONE);
		label.setText(SVNUIMessages.EditAutoPropertiesPanel_FileName);
		
		this.txtFileName = new Text(composite, SWT.BORDER);
		this.txtFileName.setText((this.property == null) ? "" : this.property.fileName); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_HORIZONTAL);
		this.txtFileName.setLayoutData(layoutData);
		String fieldName = SVNUIMessages.EditAutoPropertiesPanel_FileName_Verifier;
		CompositeVerifier verifier = new CompositeVerifier();
		verifier.add(new NonEmptyFieldVerifier(fieldName));
		verifier.add(new AbstractVerifierProxy(new FileNameTemplateVerifier(fieldName)) {
			protected boolean isVerificationEnabled(Control input) {
				return EditAutoPropertiesPanel.this.txtFileName.getText().trim().length() > 0;
			}
		});
		this.attachTo(this.txtFileName, verifier);
		
		Group group = new Group(parent, SWT.NONE);
		group.setText(SVNUIMessages.EditAutoPropertiesPanel_Properties);
		layoutData = new GridData(GridData.FILL_BOTH);
		group.setLayoutData(layoutData);
		layout = new GridLayout();
		group.setLayout(layout);
		
		label = new Label(group, SWT.NONE);
		label.setText(SVNUIMessages.EditAutoPropertiesPanel_Properties_Hint);
		
		this.txtProperties = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		this.txtProperties.setText(this.property == null ? "" : this.property.properties.trim()); //$NON-NLS-1$
		layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.heightHint = DefaultDialog.convertHeightInCharsToPixels(this.txtProperties, 7);
		this.txtProperties.setLayoutData(layoutData);
		this.attachTo(this.txtProperties, new AbstractVerifierProxy(new MultiLinePropertyVerifier(SVNUIMessages.EditAutoPropertiesPanel_Properties_Verifier)) {
			protected boolean isVerificationEnabled(Control input) {
				return EditAutoPropertiesPanel.this.txtProperties.getText().trim().length() > 0;
			}
		});
	}
	
	protected void cancelChangesImpl() {
	}

	protected void saveChangesImpl() {
		this.fileName = this.txtFileName.getText().trim();
		this.properties = this.txtProperties.getText().trim();
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	public String getProperties() {
		return this.properties;
	}

}
