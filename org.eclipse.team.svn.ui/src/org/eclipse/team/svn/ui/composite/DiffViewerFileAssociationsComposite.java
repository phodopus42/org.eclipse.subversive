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

package org.eclipse.team.svn.ui.composite;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.IDiffViewerChangeListener;
import org.eclipse.team.svn.core.operation.local.DiffViewerSettings.ResourceSpecificParameters;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.common.EditFileAssociationsPanel;
import org.eclipse.team.svn.ui.verifier.IValidationManager;

/**
 * 
 * File associations for diff viewer
 * It associates either file extension or file mime type with external diff program
 * 
 * @author Igor Burilo
 */
public class DiffViewerFileAssociationsComposite extends Composite {

	protected static final int COLUMN_CHECKBOX = 0;
	protected static final int COLUMN_EXTENSION = 1;
	protected static final int COLUMN_PATH = 2;
	
	protected IValidationManager validationManager;	
	protected DiffViewerSettings diffSettings;
	
	protected CheckboxTableViewer tableViewer;
	protected Text parametersText;
	protected Button addButton;
	protected Button editButton;
	protected Button removeButton;
	
	public DiffViewerFileAssociationsComposite(Composite parent, IValidationManager validationManager) {
		super(parent, SWT.NONE);
		this.validationManager = validationManager;
		
		this.createControls();
	}

	public void initializeControls(DiffViewerSettings diffSettings) {
		this.diffSettings = diffSettings;
		
		this.parametersText.setText(""); //$NON-NLS-1$
		this.tableViewer.setInput(diffSettings);		
		
		//set checked
		ResourceSpecificParameters[] params = diffSettings.getResourceSpecificParameters();
		for (ResourceSpecificParameters param : params) {
			this.tableViewer.setChecked(param, param.isEnabled);
		}
	}
	
	protected void createControls() {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 5;
		layout.numColumns = 2;
		GridData data = new GridData(GridData.FILL_BOTH);
		this.setLayout(layout);
		this.setLayoutData(data);
		
		Composite tableComposite = new Composite(this, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.numColumns = 1;
		data = new GridData(GridData.FILL_BOTH);
		tableComposite.setLayout(layout);
		tableComposite.setLayoutData(data);
		
		this.createFileAssociationsTable(tableComposite);
		this.createParametersPreview(tableComposite);
				
		this.createButtonsControls(this);
		this.enableButtons();
	}

	protected void createParametersPreview(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayout(layout);
		group.setLayoutData(data);
		group.setText(SVNUIMessages.DiffViewerFileAssociationsComposite_ProgramArguments_Label);
				
		this.parametersText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);		
		data.heightHint = DefaultDialog.convertHeightInCharsToPixels(parametersText, 11);
		this.parametersText.setLayoutData(data);
		this.parametersText.setBackground(parametersText.getBackground());
		this.parametersText.setEditable(false);				
	}

	protected void createFileAssociationsTable(Composite parent) {		
		Table table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnPixelData(20, false));
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(70, true));
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		table.setLayout(layout);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);				
		
		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setResizable(false);
		
		column = new TableColumn(table, SWT.NONE);
		column.setText(SVNUIMessages.DiffViewerFileAssociationsComposite_ExtensionMimeType_Column);
		
		column = new TableColumn(table, SWT.NONE);
		column.setText(SVNUIMessages.DiffViewerFileAssociationsComposite_ProgramPath_Column);
				
		this.tableViewer = new CheckboxTableViewer(table);
		this.tableViewer.setUseHashlookup(true);
		//this.tableViewer.setColumnProperties(columnNames);
				
		this.tableViewer.setContentProvider(new FileAssociationsContentProvider());				
		this.tableViewer.setLabelProvider(new FileAssociationsLabelProvider());
		
		this.tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {			
				ResourceSpecificParameters param = (ResourceSpecificParameters) event.getElement();
				param.isEnabled = event.getChecked();
			}			
		});
		
		this.tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {				
				ResourceSpecificParameters param = getSelectedResourceSpecificParameter();
				DiffViewerFileAssociationsComposite.this.editFileAssociations(param);
			}			
		});
		
		//selection listener
		this.tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				//init parameters control
				ResourceSpecificParameters param = DiffViewerFileAssociationsComposite.this.getSelectedResourceSpecificParameter();
				if (param != null) {
					String paramsStr = param.params.paramatersString;
					if (paramsStr != null) {
						DiffViewerFileAssociationsComposite.this.parametersText.setText(paramsStr);
					}
				}
				
				DiffViewerFileAssociationsComposite.this.enableButtons();								
			}			
		});								
	}
	
	protected void enableButtons() {
		boolean hasSelection = this.getSelectedResourceSpecificParameter() != null;
		this.editButton.setEnabled(hasSelection);
		this.removeButton.setEnabled(hasSelection);
	}
	
	protected void createButtonsControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = layout.marginHeight = 0;
		
		GridData data = new GridData();
		data.verticalAlignment = SWT.TOP;
		composite.setLayout(layout);
		composite.setLayoutData(data);
		
		this.addButton = new Button(composite, SWT.PUSH);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = DefaultDialog.computeButtonWidth(this.addButton);
		this.addButton.setLayoutData(data);
		this.addButton.setText(SVNUIMessages.DiffViewerFileAssociationsComposite_Add_Button);
		
		this.editButton = new Button(composite, SWT.PUSH);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = DefaultDialog.computeButtonWidth(this.editButton);
		this.editButton.setLayoutData(data);
		this.editButton.setText(SVNUIMessages.DiffViewerFileAssociationsComposite_Edit_Button);
		
		this.removeButton = new Button(composite, SWT.PUSH);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = DefaultDialog.computeButtonWidth(this.removeButton);
		this.removeButton.setLayoutData(data);
		this.removeButton.setText(SVNUIMessages.DiffViewerFileAssociationsComposite_Remove_Button);
		
		//handlers
		
		this.addButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {			
				EditFileAssociationsPanel editPanel = new EditFileAssociationsPanel(null);
				DefaultDialog dialog = new DefaultDialog(DiffViewerFileAssociationsComposite.this.getShell(), editPanel);
				if (dialog.open() == 0) {
					ResourceSpecificParameters resourceParams = editPanel.getResourceSpecificParameters();					
					DiffViewerFileAssociationsComposite.this.diffSettings.addResourceSpecificParameters(resourceParams);
				}
			}			
		});
		
		this.editButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {		
				ResourceSpecificParameters resourceParams = DiffViewerFileAssociationsComposite.this.getSelectedResourceSpecificParameter();
				DiffViewerFileAssociationsComposite.this.editFileAssociations(resourceParams);									
			}			
		});
		
		this.removeButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {			
				ResourceSpecificParameters resourceParams = DiffViewerFileAssociationsComposite.this.getSelectedResourceSpecificParameter();
				if (resourceParams != null) {					
					DiffViewerFileAssociationsComposite.this.diffSettings.removeResourceSpecificParameters(resourceParams);
				}					
			}			
		});
	}
	
	protected void editFileAssociations(ResourceSpecificParameters resourceParams) {
		if (resourceParams != null) {
			EditFileAssociationsPanel editPanel = new EditFileAssociationsPanel(resourceParams);
			DefaultDialog dialog = new DefaultDialog(DiffViewerFileAssociationsComposite.this.getShell(), editPanel);
			if (dialog.open() == 0) {
				resourceParams = editPanel.getResourceSpecificParameters();					
				DiffViewerFileAssociationsComposite.this.diffSettings.updateResourceSpecificParameters(resourceParams);
			}
		}		
	}
	
	protected ResourceSpecificParameters getSelectedResourceSpecificParameter() {
		ResourceSpecificParameters resourceParams = null;
	
		ISelection sel =  this.tableViewer.getSelection();
		if (!sel.isEmpty() && sel instanceof IStructuredSelection) {					
			IStructuredSelection selection = (IStructuredSelection) sel;
			resourceParams = (ResourceSpecificParameters) selection.getFirstElement();
		}
		return resourceParams;			
	}
	
	/*
	 * Label provider for file associations table
	 */
	protected class FileAssociationsLabelProvider extends LabelProvider implements ITableLabelProvider {

		public String getColumnText(Object element, int columnIndex) {
			String res = ""; //$NON-NLS-1$
			ResourceSpecificParameters param = (ResourceSpecificParameters) element;
			switch (columnIndex) {
				case DiffViewerFileAssociationsComposite.COLUMN_CHECKBOX:
					res = ""; //$NON-NLS-1$
					break;
				case DiffViewerFileAssociationsComposite.COLUMN_EXTENSION:
					res = param.kind.formatKindValue();
					break;
				case DiffViewerFileAssociationsComposite.COLUMN_PATH:
					res = param.params.programPath;
					break;	
			}
			return res;
		}
		
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		
	}
	
	/*
	 * Content provider for file associations table
	 */
	protected class FileAssociationsContentProvider implements IStructuredContentProvider, IDiffViewerChangeListener {
		
		public Object[] getElements(Object inputElement) {
			DiffViewerSettings diffSettings = (DiffViewerSettings) inputElement;
			return diffSettings.getResourceSpecificParameters();
		}
		
		public void addResourceSpecificParameters(ResourceSpecificParameters params) {
			DiffViewerFileAssociationsComposite.this.tableViewer.add(params);
			DiffViewerFileAssociationsComposite.this.tableViewer.setChecked(params, params.isEnabled);
		}

		public void changeResourceSpecificParameters(ResourceSpecificParameters params) {
			DiffViewerFileAssociationsComposite.this.tableViewer.update(params, null);
			//update parametersText
			DiffViewerFileAssociationsComposite.this.parametersText.setText(params.params.paramatersString);
		}

		public void removeResourceSpecificParameters(ResourceSpecificParameters params) {
			DiffViewerFileAssociationsComposite.this.tableViewer.remove(params);
			//clear parametersText
			DiffViewerFileAssociationsComposite.this.parametersText.setText(""); //$NON-NLS-1$
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != null) {
				((DiffViewerSettings) newInput).addChangeListener(this);
			}
			if (oldInput != null) {
				((DiffViewerSettings) oldInput).removeChangeListener(this);
			}
		}			
		
		public void dispose() {
			DiffViewerFileAssociationsComposite.this.diffSettings.removeChangeListener(this);
		}
	}

}
