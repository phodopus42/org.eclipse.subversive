/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 *    Alexander Gurov - adaptation for Subversive
 *    Dann Martens - [patch] Text decorations 'ascendant' variable, More decoration options
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *******************************************************************************/
 
package org.eclipse.team.svn.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.compare.internal.TabFolderLayout;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.decorator.DecoratorVariables;
import org.eclipse.team.svn.ui.decorator.IVariable;
import org.eclipse.team.svn.ui.decorator.IVariableContentProvider;
import org.eclipse.team.svn.ui.decorator.TextVariableSetProvider;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.ListSelectionPanel;
import org.eclipse.team.svn.ui.utility.DateFormatter;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Resource decoration preferences page implementation
 * 
 * @author Alexander Gurov
 */
public class SVNTeamDecorationPreferencesPage extends AbstractSVNTeamPreferencesPage implements org.eclipse.jface.util.IPropertyChangeListener {
	protected Button useFontsButton;
	protected boolean useFontsDecor;
	
	protected Button indicateConflictedButton;
	protected Button indicateModifiedButton;
	protected Button indicateRemoteButton;
	protected Button indicateAddedButton;
	protected Button indicateNewButton;
	protected Button indicateLockedButton;
	protected Button indicateNeedsLockButton;
	protected Button indicateSwitchedButton;
	protected boolean indicateConflicted;
	protected boolean indicateModified;
	protected boolean indicateRemote;
	protected boolean indicateAdded;
	protected boolean indicateNew;
	protected boolean indicateLocked;
	protected boolean indicateNeedsLock;
	protected boolean indicateSwitched;
	
	protected Text fileFormatField;
	protected Text folderFormatField;
	protected Text projectFormatField;
	protected String fileFormat;
	protected String folderFormat;
	protected String projectFormat;
	
	protected Preview preview;
	
	protected Text outgoingCharsField;
	protected Text addedCharsField;
	protected String outgoingChars;
	protected String addedChars;
	
	protected Text trunkPrefixField;
	protected Text branchPrefixField;
	protected Text tagPrefixField;
	protected String trunkPrefix;
	protected String branchPrefix;
	protected String tagPrefix;

	protected static final Collection<PreviewFile> ROOT;
	static {	
		//name, type, added, new, dirty, ignored, hasRemote, locked
		PreviewFile branchProject = new PreviewFile("ProjectBranch", IResource.PROJECT, false, false, false, false, false, true, false, false, false, true, false, false); //$NON-NLS-1$
		PreviewFile tagProject = new PreviewFile("ProjectTag", IResource.PROJECT, false, false, false, false, false, true, false, false, false, false, true, false); //$NON-NLS-1$
		
		PreviewFile project = new PreviewFile("Project", IResource.PROJECT, false, false, true, false, false, true, false, false, true, false, false, false); //$NON-NLS-1$
		
		PreviewFile modifiedFolder = new PreviewFile("folder", IResource.FOLDER, false, false, true, false, false, true, false, false, true, false, false, false); //$NON-NLS-1$
		ArrayList<PreviewFile> children = new ArrayList<PreviewFile>();
		children.add(new PreviewFile("switched", IResource.FOLDER, false, false, false, false, false, true, false, false, true, false, false, true)); //$NON-NLS-1$
		children.add(new PreviewFile("normal.txt", IResource.FILE, false, false, false, false, false, true, false, false, true, false, false, false)); //$NON-NLS-1$
		children.add(new PreviewFile("modified.cpp", IResource.FILE, false, false, true, false, false, true, false, false, true, false, false, false)); //$NON-NLS-1$
		children.add(new PreviewFile("conflicting.cpp", IResource.FILE, false, false, true, true, false, true, false, false, true, false, false, false)); //$NON-NLS-1$
		children.add(new PreviewFile("ignored.txt", IResource.FILE, false, false, false, false, true, false, false, false, true, false, false, false)); //$NON-NLS-1$
		modifiedFolder.children = children;
		
		children = new ArrayList<PreviewFile>();
		children.add(modifiedFolder);
		children.add(new PreviewFile("new", IResource.FILE, false, true, false, false, false, false, false, false, true, false, false, false)); //$NON-NLS-1$
		children.add(new PreviewFile("added.java", IResource.FILE, true, false, true, false, false, false, false, false, true, false, false, false)); //$NON-NLS-1$
		children.add(new PreviewFile("locked", IResource.FILE, false, false, false, false, false, true, true, false, true, false, false, false)); //$NON-NLS-1$
		children.add(new PreviewFile("needsLock", IResource.FILE, false, false, false, false, false, true, false, true, true, false, false, false)); //$NON-NLS-1$
		
		project.children = children;
		ROOT = new ArrayList<PreviewFile>();
		ROOT.add(project);
		ROOT.add(branchProject);
		ROOT.add(tagProject);
	}
	
	public SVNTeamDecorationPreferencesPage() {
		super();
		getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		getPreferenceStore().removePropertyChangeListener(this);
	}

	protected void saveValues(IPreferenceStore store) {
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FILE_NAME, this.fileFormat);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FOLDER_NAME, this.folderFormat);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_PROJECT_NAME, this.projectFormat);

		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_OUTGOING_NAME, this.outgoingChars);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_ADDED_NAME, this.addedChars);
		
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_TRUNK_PREFIX_NAME, this.trunkPrefix);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_BRANCH_PREFIX_NAME, this.branchPrefix);
		SVNTeamPreferences.setDecorationString(store, SVNTeamPreferences.DECORATION_TAG_PREFIX_NAME, this.tagPrefix);
		
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_CONFLICTED_NAME, this.indicateConflicted);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_MODIFIED_NAME, this.indicateModified);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_REMOTE_NAME, this.indicateRemote);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_ADDED_NAME, this.indicateAdded);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEW_NAME, this.indicateNew);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_LOCKED_NAME, this.indicateLocked);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_NAME, this.indicateNeedsLock);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_SWITCHED_NAME, this.indicateSwitched);

		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_NAME, this.useFontsDecor);
	}
	
	protected void loadDefaultValues(IPreferenceStore store) {
		this.fileFormat = SVNTeamPreferences.DECORATION_FORMAT_FILE_DEFAULT;
		this.folderFormat = SVNTeamPreferences.DECORATION_FORMAT_FOLDER_DEFAULT;
		this.projectFormat = SVNTeamPreferences.DECORATION_FORMAT_PROJECT_DEFAULT;
		
		this.outgoingChars = SVNTeamPreferences.DECORATION_FLAG_OUTGOING_DEFAULT;
		this.addedChars = SVNTeamPreferences.DECORATION_FLAG_ADDED_DEFAULT;
		
		this.trunkPrefix = SVNTeamPreferences.DECORATION_TRUNK_PREFIX_DEFAULT;
		this.branchPrefix = SVNTeamPreferences.DECORATION_BRANCH_PREFIX_DEFAULT;
		this.tagPrefix = SVNTeamPreferences.DECORATION_TAG_PREFIX_DEFAULT;
		
		this.indicateConflicted = SVNTeamPreferences.DECORATION_ICON_CONFLICTED_DEFAULT;
		this.indicateModified = SVNTeamPreferences.DECORATION_ICON_MODIFIED_DEFAULT;
		this.indicateRemote = SVNTeamPreferences.DECORATION_ICON_REMOTE_DEFAULT;
		this.indicateAdded = SVNTeamPreferences.DECORATION_ICON_ADDED_DEFAULT;
		this.indicateNew = SVNTeamPreferences.DECORATION_ICON_NEW_DEFAULT;
		this.indicateLocked = SVNTeamPreferences.DECORATION_ICON_LOCKED_DEFAULT;
		this.indicateNeedsLock = SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_DEFAULT;
		this.indicateSwitched = SVNTeamPreferences.DECORATION_ICON_SWITCHED_DEFAULT;
		
		this.useFontsDecor = SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_DEFAULT;
	}
	
	protected void loadValues(IPreferenceStore store) {
		this.fileFormat = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FILE_NAME);
		this.folderFormat = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_FOLDER_NAME);
		this.projectFormat = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FORMAT_PROJECT_NAME);
		
		this.outgoingChars = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_OUTGOING_NAME);
		this.addedChars = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_FLAG_ADDED_NAME);
		
		this.trunkPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_TRUNK_PREFIX_NAME);
		this.branchPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_BRANCH_PREFIX_NAME);
		this.tagPrefix = SVNTeamPreferences.getDecorationString(store, SVNTeamPreferences.DECORATION_TAG_PREFIX_NAME);
		
		this.indicateConflicted = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_CONFLICTED_NAME);
		this.indicateModified = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_MODIFIED_NAME);
		this.indicateRemote = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_REMOTE_NAME);
		this.indicateAdded = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_ADDED_NAME);
		this.indicateNew = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEW_NAME);
		this.indicateLocked = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_LOCKED_NAME);
		this.indicateNeedsLock = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_NEEDS_LOCK_NAME);
		this.indicateSwitched = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ICON_SWITCHED_NAME);
		
		this.useFontsDecor = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_USE_FONT_COLORS_DECOR_NAME);
	}
	
	protected void initializeControls() {
		this.fileFormatField.setText(this.fileFormat);
		this.folderFormatField.setText(this.folderFormat);
		this.projectFormatField.setText(this.projectFormat);

		this.outgoingCharsField.setText(this.outgoingChars);
		this.addedCharsField.setText(this.addedChars);
		
		this.trunkPrefixField.setText(this.trunkPrefix);
		this.branchPrefixField.setText(this.branchPrefix);
		this.tagPrefixField.setText(this.tagPrefix);
		
		this.indicateConflictedButton.setSelection(this.indicateConflicted);
		this.indicateModifiedButton.setSelection(this.indicateModified);
		this.indicateRemoteButton.setSelection(this.indicateRemote);
		this.indicateAddedButton.setSelection(this.indicateAdded);
		this.indicateNewButton.setSelection(this.indicateNew);
		this.indicateLockedButton.setSelection(this.indicateLocked);
		this.indicateNeedsLockButton.setSelection(this.indicateNeedsLock);
		this.indicateSwitchedButton.setSelection(this.indicateSwitched);
		
		this.useFontsButton.setSelection(this.useFontsDecor);
		
		this.refreshPreview();
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().startsWith(SVNTeamPreferences.DATE_FORMAT_BASE)) {
			this.refreshPreview();
		}
	}
	
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayout(new TabFolderLayout());
		tabFolder.setLayoutData(data = new GridData(GridData.FILL_HORIZONTAL));
		data.heightHint = 200;
		
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_generalTabName);
		tabItem.setControl(this.createGeneralSettingsPage(tabFolder));
		
		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_textTabName);
		tabItem.setControl(this.createTextSettingsPage(tabFolder));
		
		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(SVNUIMessages.PreferencePage_iconsTabName);
		tabItem.setControl(this.createIconsSettingsPage(tabFolder));

		this.preview = new Preview(composite);
		
//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.decorsPreferencesContext"); //$NON-NLS-1$
		
		return composite;
	}
	
	protected Composite createTextSettingsPage(Composite parent) {
		GridLayout layout = null;
		GridData data = null;
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		data = new GridData();
		data.grabExcessVerticalSpace = false;
		composite.setLayoutData(data);
		
		Label label = new Label(composite, SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 450;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.PreferencePage_textPrompt);
		
		Composite groups = new Composite(composite, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		groups.setLayout(layout);
		data = new GridData(GridData.FILL_HORIZONTAL);
		groups.setLayoutData(data);
		
		Group formatGroup = new Group(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.verticalSpacing = 1;
		formatGroup.setLayout(layout);
		formatGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		formatGroup.setText(SVNUIMessages.PreferencePage_formatGroup);
		
		List<IVariable> fileOptions = new ArrayList<IVariable>();
		fileOptions.add(TextVariableSetProvider.VAR_OUTGOING_FLAG);
		fileOptions.add(TextVariableSetProvider.VAR_ADDED_FLAG);
		fileOptions.add(TextVariableSetProvider.VAR_NAME);
		fileOptions.add(TextVariableSetProvider.VAR_REVISION);
		fileOptions.add(TextVariableSetProvider.VAR_DATE);
		fileOptions.add(TextVariableSetProvider.VAR_AUTHOR);
		fileOptions.add(TextVariableSetProvider.VAR_RESOURCE_URL);
		this.fileFormatField = this.createFormatControl(formatGroup, "PreferencePage_textFileFormat", fileOptions, Collections.emptyList()); //$NON-NLS-1$
		this.fileFormatField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.fileFormat = SVNTeamDecorationPreferencesPage.this.fileFormatField.getText();
			}
		});
		
		List<IVariable> folderOptions = new ArrayList<IVariable>();
		folderOptions.addAll(fileOptions);
		this.folderFormatField = this.createFormatControl(formatGroup, "PreferencePage_textFolderFormat", folderOptions, Collections.emptyList()); //$NON-NLS-1$
		this.folderFormatField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.folderFormat = SVNTeamDecorationPreferencesPage.this.folderFormatField.getText();
			}
		});
		
		List<IVariable> projectOptions = new ArrayList<IVariable>();
		projectOptions.add(TextVariableSetProvider.VAR_OUTGOING_FLAG);
		projectOptions.add(TextVariableSetProvider.VAR_NAME);
		projectOptions.add(TextVariableSetProvider.VAR_REVISION);
		projectOptions.add(TextVariableSetProvider.VAR_LOCATION_LABEL);
		projectOptions.add(TextVariableSetProvider.VAR_LOCATION_URL);
		projectOptions.add(TextVariableSetProvider.VAR_ROOT_PREFIX); //5
		projectOptions.add(TextVariableSetProvider.VAR_ASCENDANT);
		projectOptions.add(TextVariableSetProvider.VAR_DESCENDANT);
		projectOptions.add(TextVariableSetProvider.VAR_FULLNAME);
		projectOptions.add(TextVariableSetProvider.VAR_FULLPATH);
		projectOptions.add(TextVariableSetProvider.VAR_RESOURCE_URL);
		projectOptions.add(TextVariableSetProvider.VAR_SHORT_RESOURCE_URL);
		projectOptions.add(TextVariableSetProvider.VAR_REMOTE_NAME);
		projectOptions.add(TextVariableSetProvider.VAR_DATE);
		projectOptions.add(TextVariableSetProvider.VAR_AUTHOR);
		
		this.projectFormatField = this.createFormatControl(formatGroup, "PreferencePage_textProjectFormat", projectOptions, Collections.emptyList()); //$NON-NLS-1$
		this.projectFormatField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.projectFormat = SVNTeamDecorationPreferencesPage.this.projectFormatField.getText();
			}
		});
		
		List<Object> grayedOptions = new ArrayList<Object>();
		grayedOptions.add(projectOptions.get(5));
		
		Group prefixGroup = new Group(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.verticalSpacing = 1;
		prefixGroup.setLayout(layout);
		prefixGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		prefixGroup.setText(SVNUIMessages.PreferencePage_rootPrefixGroup);
		
		this.trunkPrefixField = this.createFormatControl(prefixGroup, "PreferencePage_textTrunkPrefix", projectOptions, grayedOptions); //$NON-NLS-1$
		this.trunkPrefixField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.trunkPrefix = SVNTeamDecorationPreferencesPage.this.trunkPrefixField.getText();
			}
		});

		this.branchPrefixField = this.createFormatControl(prefixGroup, "PreferencePage_textBranchPrefix", projectOptions, grayedOptions); //$NON-NLS-1$
		this.branchPrefixField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.branchPrefix = SVNTeamDecorationPreferencesPage.this.branchPrefixField.getText();
			}
		});

		this.tagPrefixField = this.createFormatControl(prefixGroup, "PreferencePage_textTagPrefix", projectOptions, grayedOptions); //$NON-NLS-1$
		this.tagPrefixField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.tagPrefix = SVNTeamDecorationPreferencesPage.this.tagPrefixField.getText();
			}
		});
		
		Composite outFlagComposite = new Composite(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		outFlagComposite.setLayout(layout);
		outFlagComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		label = new Label(outFlagComposite, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.PreferencePage_textOutgoingFlag);

		this.outgoingCharsField = new Text(outFlagComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		this.outgoingCharsField.setLayoutData(data);
		this.outgoingCharsField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.outgoingChars = SVNTeamDecorationPreferencesPage.this.outgoingCharsField.getText();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		Composite addFlagComposite = new Composite(groups, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		addFlagComposite.setLayout(layout);
		addFlagComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = new Label(addFlagComposite, SWT.NULL);
		label.setLayoutData(new GridData());
		label.setText(SVNUIMessages.PreferencePage_textAddedFlag);
		
		this.addedCharsField = new Text(addFlagComposite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		this.addedCharsField.setLayoutData(data);
		this.addedCharsField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.addedChars = SVNTeamDecorationPreferencesPage.this.addedCharsField.getText();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
	
		return composite;
	}
	
	protected Text createFormatControl(Composite parent, String labelId, final List<IVariable> variables, final List<Object> grayedVariables) {
		Label label = new Label(parent, SWT.NULL);
		label.setLayoutData(new GridData());
		if (labelId != null) {
			label.setText(SVNUIMessages.getString(labelId));
		}
		else {
			label.setText(""); //$NON-NLS-1$
		}
		
		final Text format = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 100;
		data.grabExcessHorizontalSpace = true;
		format.setLayoutData(data);
		
		Button addVariables = new Button(parent, SWT.PUSH);
		addVariables.setText(SVNUIMessages.PreferencePage_textAddVariables);
		data = new GridData();
		data.widthHint = 25;
		addVariables.setLayoutData(data);		
		addVariables.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				SVNTeamDecorationPreferencesPage.this.variableConfigurationDialog(format, variables, grayedVariables);
			}
		});
		format.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		return format;
	}
	
	protected void variableConfigurationDialog(Text field, List<IVariable> variableList, List<Object> grayedVariableList) {
		final IVariable []variables = variableList.toArray(new IVariable[variableList.size()]);
		final IVariable []grayedVariables = grayedVariableList.toArray(new IVariable[grayedVariableList.size()]);
		IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return variables;
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		};
		ILabelProvider labelProvider = new LabelProvider() {
			public String getText(Object element) {
				IVariable var = (IVariable)element;
				return var.getName() + " - " + var.getDescription(); //$NON-NLS-1$
			}
		};
		String dialogPrompt = SVNUIMessages.PreferencePage_textAddVariablesPrompt;
		String dialogMessage = SVNUIMessages.PreferencePage_textAddVariablesMessage;
		
		DecoratorVariables decorator = new DecoratorVariables(TextVariableSetProvider.instance);
		IVariable []realVars = decorator.parseFormatLine(field.getText());
		
		ListSelectionPanel panel = new ListSelectionPanel(this, contentProvider, labelProvider, dialogPrompt, dialogMessage);
		panel.setInitialSelections(realVars);
		panel.setInitialGrayed(grayedVariables);
		if (new DefaultDialog(this.getShell(), panel).open() == 0) {
			List<Object> result = new ArrayList<Object>();
			List<Object> newSelection = Arrays.asList(panel.getResultSelections());
			for (int i = 0; i < realVars.length; i++) {
				if (TextVariableSetProvider.instance.getVariable(realVars[i].getName()) == null) {
					result.add(realVars[i]);
				}
				else if (newSelection.contains(realVars[i])) {
					result.add(realVars[i]);
				}
			}
			for (int i = 0; i < newSelection.size(); i++) {
				if (!result.contains(newSelection.get(i))) {
					result.add(newSelection.get(i));
				}
			}
			realVars = result.toArray(new IVariable[result.size()]);
			
			field.setText(DecoratorVariables.prepareFormatLine(realVars));
		}
	}
	
	protected Composite createIconsSettingsPage(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(composite, SWT.NULL);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(SVNUIMessages.PreferencePage_iconsPrompt);
		
		this.indicateModifiedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateModifiedButton.setLayoutData(data);
		this.indicateModifiedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateModified);
		this.indicateModifiedButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateModified = SVNTeamDecorationPreferencesPage.this.indicateModifiedButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateConflictedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateConflictedButton.setLayoutData(data);
		this.indicateConflictedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateConflicted);
		this.indicateConflictedButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateConflicted = SVNTeamDecorationPreferencesPage.this.indicateConflictedButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateRemoteButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateRemoteButton.setLayoutData(data);
		this.indicateRemoteButton.setText(SVNUIMessages.PreferencePage_iconsIndicateRemote);
		this.indicateRemoteButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateRemote = SVNTeamDecorationPreferencesPage.this.indicateRemoteButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateLockedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateLockedButton.setLayoutData(data);
		this.indicateLockedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateLocked);
		this.indicateLockedButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateLocked = SVNTeamDecorationPreferencesPage.this.indicateLockedButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateAddedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateAddedButton.setLayoutData(data);
		this.indicateAddedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateAdded);
		this.indicateAddedButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateAdded = SVNTeamDecorationPreferencesPage.this.indicateAddedButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateNeedsLockButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateNeedsLockButton.setLayoutData(data);
		this.indicateNeedsLockButton.setText(SVNUIMessages.PreferencePage_iconsIndicateNeedsLock);
		this.indicateNeedsLockButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateNeedsLock = SVNTeamDecorationPreferencesPage.this.indicateNeedsLockButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateNewButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateNewButton.setLayoutData(data);
		this.indicateNewButton.setText(SVNUIMessages.PreferencePage_iconsIndicateNew);
		this.indicateNewButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateNew = SVNTeamDecorationPreferencesPage.this.indicateNewButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		this.indicateSwitchedButton = new Button(composite, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		this.indicateSwitchedButton.setLayoutData(data);
		this.indicateSwitchedButton.setText(SVNUIMessages.PreferencePage_iconsIndicateSwitched);
		this.indicateSwitchedButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.indicateSwitched = SVNTeamDecorationPreferencesPage.this.indicateSwitchedButton.getSelection();
				SVNTeamDecorationPreferencesPage.this.refreshPreview();
			}
		});
		
		return composite;
	}
	
	protected Composite createGeneralSettingsPage(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite noteComposite = new Composite(composite, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		noteComposite.setLayout(layout);
		noteComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label noteLabel = new Label(noteComposite, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		data.heightHint = this.convertHeightInCharsToPixels(4);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;		
		noteLabel.setLayoutData(data);
		noteLabel.setText(SVNUIMessages.PreferencePage_noteLabel);
		
		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new PreferenceLinkArea(composite, SWT.NONE, SVNUIMessages.PreferencePage_generalUseLabels_1, SVNUIMessages.PreferencePage_generalUseLabels_2, (IWorkbenchPreferenceContainer)this.getContainer(), null);
		
		this.useFontsButton = new Button(composite, SWT.CHECK);
		this.useFontsButton.setLayoutData(new GridData());
		this.useFontsButton.setText(SVNUIMessages.PreferencePage_generalUseFonts_2);
		new PreferenceLinkArea(composite, SWT.NONE, SVNUIMessages.PreferencePage_generalUseFonts_1, SVNUIMessages.PreferencePage_generalUseFonts_3, (IWorkbenchPreferenceContainer)this.getContainer(), null);
		this.useFontsButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamDecorationPreferencesPage.this.useFontsDecor = SVNTeamDecorationPreferencesPage.this.useFontsButton.getSelection();
			}
		});
		
		return composite;
	}
	
	protected ImageDescriptor getOverlay(PreviewFile element) {
		if (element.ignored) {
			return null;
		} 
		else if (element.locked && this.indicateLocked) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/lock.gif"); //$NON-NLS-1$
		}
		else if (element.isSwitched && this.indicateSwitched) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/switched.gif"); //$NON-NLS-1$
		}
		else if (element.needsLock && this.indicateNeedsLock) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/needs_lock.gif"); //$NON-NLS-1$
		}
		else if (element.newResource && this.indicateNew) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/new_resource.gif"); //$NON-NLS-1$
		}
		else if (element.added && this.indicateAdded) {
			return TeamImages.getImageDescriptor(ISharedImages.IMG_HOURGLASS_OVR);
		}
		else if (element.conflicted && this.indicateConflicted) {
			return SVNTeamUIPlugin.instance().getImageDescriptor("icons/overlays/conflicted_unresolved.gif"); //$NON-NLS-1$
		}
		else if (element.dirty && this.indicateModified) {
			return TeamImages.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR);
		}
		else if (element.hasRemote && this.indicateRemote) {
			return TeamImages.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR);
		}
		return null;
	}
	
	protected void refreshPreview() {
		if (this.preview != null) {
			this.preview.refresh();
		}
	}

	protected class DemoDecoration {
		
		protected String fullName;
		
		public DemoDecoration(String baseName) {
			this.fullName = baseName;
		}
		
		public String getFullName() {
			return this.fullName;
		}

		public void addPrefix(String prefix) {
			this.fullName = prefix + this.fullName;
		}

		public void addSuffix(String suffix) {
			this.fullName += suffix;
		}

		public void addOverlay(ImageDescriptor overlay) {
			
		}

		public void addOverlay(ImageDescriptor overlay, int quadrant) {
			
		}
		
		public void setFont(Font font) {
			
		}
		
		public void setForegroundColor(Color color) {
			
		}
		
		public void setBackgroundColor(Color color) {
			
		}
		
	}
	
	protected class DemoVariableContentProvider implements IVariableContentProvider {
		protected PreviewFile preview;
		protected String demoRevision;
		
		public DemoVariableContentProvider(PreviewFile preview, String demoRevision) {
			this.preview = preview;
			this.demoRevision = demoRevision;
		}

		public String getValue(IVariable var) {
			if (var.equals(TextVariableSetProvider.VAR_ADDED_FLAG)) {
				if (this.preview.added) {
					return SVNTeamDecorationPreferencesPage.this.addedChars;
				}
				return ""; //$NON-NLS-1$
			}
			else if (var.equals(TextVariableSetProvider.VAR_OUTGOING_FLAG)) {
				if (this.preview.dirty) {
					return SVNTeamDecorationPreferencesPage.this.outgoingChars;
				}
				return ""; //$NON-NLS-1$
			}
			else if (var.equals(TextVariableSetProvider.VAR_ROOT_PREFIX)) {
				if (this.preview.isTag) {
					return SVNTeamDecorationPreferencesPage.this.tagPrefix;
				}
				else if (this.preview.isBranch) {
					return SVNTeamDecorationPreferencesPage.this.branchPrefix;
				}
				else if (this.preview.isTrunk) {
					return SVNTeamDecorationPreferencesPage.this.trunkPrefix;
				}
				return ""; //$NON-NLS-1$
			}
			else if (var.equals(TextVariableSetProvider.VAR_ASCENDANT)) {
				return SVNUIMessages.PreferencePage_demoAscendant;
			}
			else if (var.equals(TextVariableSetProvider.VAR_DESCENDANT)) {
				return SVNUIMessages.PreferencePage_demoDescendant;
			}
			else if (var.equals(TextVariableSetProvider.VAR_FULLNAME)) {
				return SVNUIMessages.PreferencePage_demoFullname;
			}
			else if (var.equals(TextVariableSetProvider.VAR_FULLPATH)) {
				return SVNUIMessages.PreferencePage_demoFullpath;
			}
			else if (var.equals(TextVariableSetProvider.VAR_AUTHOR)) {
				return SVNUIMessages.PreferencePage_demoAuthor;
			}
			else if (var.equals(TextVariableSetProvider.VAR_NAME)) {
				return this.preview.name;
			}
			else if (var.equals(TextVariableSetProvider.VAR_LOCATION_URL)) {
				return SVNUIMessages.PreferencePage_demoLocationURL;
			}
			else if (var.equals(TextVariableSetProvider.VAR_LOCATION_LABEL)) {
				return SVNUIMessages.PreferencePage_demoLocationLabel;
			}
			else if (var.equals(TextVariableSetProvider.VAR_RESOURCE_URL)) {
				return SVNUIMessages.PreferencePage_demoResourceURL;
			}
			else if (var.equals(TextVariableSetProvider.VAR_SHORT_RESOURCE_URL)) {
				return SVNUIMessages.PreferencePage_demoShortURL;
			}
			else if (var.equals(TextVariableSetProvider.VAR_REMOTE_NAME)) {
				return SVNUIMessages.PreferencePage_demoRemoteName;
			}
			else if (var.equals(TextVariableSetProvider.VAR_DATE)) {
				return DateFormatter.formatDate(new Date());
			}
			else if (var.equals(TextVariableSetProvider.VAR_REVISION)) {
				return this.demoRevision;
			}
			return var.toString();
		}
		
	}

	protected class Preview extends LabelProvider implements Observer, ITreeContentProvider {
		
		protected Map<ImageDescriptor, Image> images;
		private final TreeViewer fViewer;
		
		protected DecoratorVariables decoratorVariables;

		public Preview(Composite parent) {
			this.decoratorVariables = new DecoratorVariables(TextVariableSetProvider.instance);
			this.images = new HashMap<ImageDescriptor, Image>();
			Composite composite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.grabExcessVerticalSpace = true;
			composite.setLayoutData(data);
			Label label = new Label(composite, SWT.NULL);
			label.setLayoutData(new GridData());
			label.setText(SVNUIMessages.PreferencePage_preview);
			this.fViewer = new TreeViewer(composite);
			data = new GridData(GridData.FILL_BOTH);
			data.heightHint = Math.max(SVNTeamDecorationPreferencesPage.this.convertHeightInCharsToPixels(1), 16) * 13;
			data.grabExcessVerticalSpace = true;
			this.fViewer.getControl().setLayoutData(data);
			this.fViewer.setContentProvider(this);
			this.fViewer.setLabelProvider(this);
			this.fViewer.setInput(ROOT);
			this.fViewer.expandAll();
			this.fViewer.setSelection(new StructuredSelection(ROOT.iterator().next()));
			this.fViewer.getTree().showSelection();
			this.fViewer.setSelection(null);
		}
		
		public void refresh() {
			this.fViewer.refresh(true);
		}
		
		public void update(Observable o, Object arg) {
			refresh();
		}
		
		public Object[] getChildren(Object parentElement) {
			return ((PreviewFile)parentElement).children.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return !((PreviewFile)element).children.isEmpty();
		}
		
		public Object[] getElements(Object inputElement) {
			return ((Collection<?>)inputElement).toArray();
		}

		public void dispose() {
            for (Image image : this.images.values()) {
				image.dispose();
			}
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public String getText(Object element) {
			IVariableContentProvider provider = null;
			IVariable []realVars = null;
			PreviewFile previewFile = (PreviewFile)element;
			switch (previewFile.type) {
			case IResource.PROJECT:
				provider = new DemoVariableContentProvider(previewFile,	SVNUIMessages.PreferencePage_demoProjectRevision);
				realVars = this.decoratorVariables.parseFormatLine(SVNTeamDecorationPreferencesPage.this.projectFormatField.getText());
				break;
			case IResource.FOLDER:
				provider = new DemoVariableContentProvider(previewFile, SVNUIMessages.PreferencePage_demoFolderRevision);
				realVars = this.decoratorVariables.parseFormatLine(SVNTeamDecorationPreferencesPage.this.folderFormatField.getText());
				break;
			default:
				provider = new DemoVariableContentProvider(previewFile,	SVNUIMessages.PreferencePage_demoFileRevision);
				realVars = this.decoratorVariables.parseFormatLine(SVNTeamDecorationPreferencesPage.this.fileFormatField.getText());
				break;
			}
			DemoDecoration decoration = new DemoDecoration(previewFile.name);
			this.decorateText(decoration, realVars, provider);
			return decoration.getFullName();
		}
		
		private String getValue(IVariable var, IVariableContentProvider provider) {
			return amend(var, provider);
		}
		
		/**
		 * Helper method which recurses through variables in variables, first order only.
		 * @param var A variable wrapper.
		 * @param provider A <code>IVariableContentProvider</code>
		 * @return The amended value of this variable.
		 */
		private String amend(IVariable var, IVariableContentProvider provider) {
			IVariable[] variables = this.decoratorVariables.parseFormatLine(provider.getValue(var));
			String value = ""; //$NON-NLS-1$
			for (int i = 0; i < variables.length; i++) {
				String variableValue = provider.getValue(variables[i]);
				if (!variables[i].equals(var)) {
					value += variableValue;
				}
				else {
					if (variableValue.equals(variables[i].getName())) {
						value += variableValue;
					}
					else {
						value += "?{" + variables[i].getName() + "}?"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			return value;
		}
		
		public void decorateText(DemoDecoration decoration, IVariable []format, IVariableContentProvider provider) {
			int centerPoint = Arrays.asList(format).indexOf(TextVariableSetProvider.instance.getCenterVariable());
			String prefix = ""; //$NON-NLS-1$
			String suffix = ""; //$NON-NLS-1$
			for (int i = 0; i < format.length; i++) {
				if (!format[i].equals(TextVariableSetProvider.instance.getCenterVariable())) {
					if (centerPoint != -1 && i < centerPoint) {
						prefix += getValue(format[i], provider);
					}
					else {
						suffix += getValue(format[i], provider);
					}
				}
			}
			decoration.addPrefix(prefix);
			decoration.addSuffix(suffix);
		}
		
		public Image getImage(Object element) {
			ImageDescriptor descriptor = null;
			
			switch (((PreviewFile)element).type) {
			case IResource.PROJECT:
				descriptor = SVNTeamUIPlugin.instance().getImageDescriptor("icons/objects/project.gif"); //$NON-NLS-1$
				break;
			case IResource.FOLDER:
				descriptor = SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/folder.gif"); //$NON-NLS-1$
				break;
			default:
				descriptor = SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/file.gif"); //$NON-NLS-1$
				break;
			}
			Image image = this.images.get(descriptor);
			if (image == null) {
				this.images.put(descriptor, image = descriptor.createImage());
			}
			
			ImageDescriptor overlay = SVNTeamDecorationPreferencesPage.this.getOverlay((PreviewFile)element);
			if (overlay == null) {
				return image;
			}
			try {
				ImageDescriptor imgDescr = new OverlayedImageDescriptor(image, overlay, new Point(image.getBounds().width, image.getBounds().height), OverlayedImageDescriptor.BOTTOM | OverlayedImageDescriptor.RIGHT);
				Image overlayedImg = this.images.get(imgDescr);
				if (overlayedImg == null) {
					overlayedImg = imgDescr.createImage();
					this.images.put(imgDescr, overlayedImg);
				}
                return overlayedImg;
            } 
			catch (Exception e) {
                LoggedOperation.reportError(SVNUIMessages.Error_DecoratorImage, e);
            }
            return null;
		}
	}
	
	private static class PreviewFile {
		public final String name;
		public final int type;
		public final boolean added, dirty, conflicted, hasRemote, ignored, newResource, locked, needsLock, isTrunk, isBranch, isTag, isSwitched;
		public Collection<PreviewFile> children;
		
		public PreviewFile(String name, int type, boolean added, boolean newResource, boolean dirty, boolean conflicted, boolean ignored, boolean hasRemote, boolean locked, boolean needsLock, boolean isTrunk, boolean isBranch, boolean isTag, boolean isSwitched) {
			this.name = name;
			this.type = type;
			this.added = added;
			this.ignored = ignored;
			this.dirty = dirty;
			this.conflicted = conflicted;
			this.hasRemote = hasRemote;
			this.newResource = newResource;
			this.locked = locked;
			this.needsLock = needsLock;
			this.children = Collections.emptyList();
			this.isTrunk = isTrunk;
			this.isBranch = isBranch;
			this.isTag = isTag;
			this.isSwitched = isSwitched;
		}
	}
		
}
