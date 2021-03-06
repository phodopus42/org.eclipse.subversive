/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Performance options
 * 
 * @author Alexander Gurov
 */
public class SVNTeamPerformancePage extends AbstractSVNTeamPreferencesPage {
	protected Button computeDeepButton;
	protected Button preciseEnablementsButton;
	protected Button enableCacheButton;
	protected Button enablePersistentSSHConnectionButton;
//	protected Button enableFileReplacementAutoundoButton;
	
	protected boolean computeDeep;
	protected boolean preciseEnablements;
	protected boolean enableCache;
	protected boolean enablePersistentSSHConnection;
//	protected boolean enableFileReplacementAutoundo;

	public SVNTeamPerformancePage() {
		super();
	}
	
	public void init(IWorkbench workbench) {
		setDescription(SVNUIMessages.PerformancePreferencePage_optionsDesc);
	}

	protected void saveValues(IPreferenceStore store) {
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_COMPUTE_DEEP_NAME, this.computeDeep);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_NAME, this.preciseEnablements);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_CACHE_NAME, this.computeDeep | this.enableCache);
		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_PERSISTENT_SSH_NAME, this.enablePersistentSSHConnection);
//		SVNTeamPreferences.setDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_FILE_REPLACEMENT_AUTOUNDO_NAME, this.enableFileReplacementAutoundo);
	}
	
	protected void loadDefaultValues(IPreferenceStore store) {
		this.computeDeep = SVNTeamPreferences.DECORATION_COMPUTE_DEEP_DEFAULT;
		this.preciseEnablements = SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_DEFAULT;
		this.enableCache = SVNTeamPreferences.DECORATION_ENABLE_CACHE_DEFAULT;
		this.enablePersistentSSHConnection = SVNTeamPreferences.DECORATION_ENABLE_PERSISTENT_SSH_DEFAULT;
//		this.enableFileReplacementAutoundo = SVNTeamPreferences.DECORATION_ENABLE_FILE_REPLACEMENT_AUTOUNDO_DEFAULT;
	}
	
	protected void loadValues(IPreferenceStore store) {
		this.computeDeep = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_COMPUTE_DEEP_NAME);
		this.preciseEnablements = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_PRECISE_ENABLEMENTS_NAME);
		this.enableCache = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_CACHE_NAME);
		this.enablePersistentSSHConnection = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_PERSISTENT_SSH_NAME);
//		this.enableFileReplacementAutoundo = SVNTeamPreferences.getDecorationBoolean(store, SVNTeamPreferences.DECORATION_ENABLE_FILE_REPLACEMENT_AUTOUNDO_NAME);
	}
	
	protected void initializeControls() {
		this.computeDeepButton.setSelection(this.computeDeep);
		this.preciseEnablementsButton.setSelection(this.preciseEnablements);
		this.enableCacheButton.setSelection(this.enableCache);
		this.enablePersistentSSHConnectionButton.setSelection(this.enablePersistentSSHConnection);
//		this.enableFileReplacementAutoundoButton.setSelection(this.enableFileReplacementAutoundo);
		if (this.computeDeep || this.preciseEnablements) {
			this.enableCacheButton.setEnabled(false);
		}
		else if (!this.enableCache) {
			this.computeDeepButton.setEnabled(false);
			this.preciseEnablementsButton.setEnabled(false);
		}
	}
	
	protected Control createContentsImpl(Composite parent) {
		Composite composite = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = false;
		composite.setLayoutData(data);
		
		Composite noteComposite = new Composite(composite, SWT.FILL);
		layout = new GridLayout();
		layout.marginWidth = 0;
		noteComposite.setLayout(layout);
		noteComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label separator = new Label(noteComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		separator.setLayoutData(data);
		
		this.computeDeepButton = new Button(composite, SWT.CHECK);
		this.computeDeepButton.setLayoutData(new GridData());
		this.computeDeepButton.setText(SVNUIMessages.PerformancePreferencePage_computeDeep);
		this.computeDeepButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamPerformancePage.this.computeDeep = SVNTeamPerformancePage.this.computeDeepButton.getSelection();
				SVNTeamPerformancePage.this.enableCacheButton.setEnabled(!(SVNTeamPerformancePage.this.computeDeep | SVNTeamPerformancePage.this.preciseEnablements));
			}
		});
		
		this.preciseEnablementsButton = new Button(composite, SWT.CHECK);
		this.preciseEnablementsButton.setLayoutData(new GridData());
		this.preciseEnablementsButton.setText(SVNUIMessages.PerformancePreferencePage_preciseEnablements);
		this.preciseEnablementsButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamPerformancePage.this.preciseEnablements = SVNTeamPerformancePage.this.preciseEnablementsButton.getSelection();
				SVNTeamPerformancePage.this.enableCacheButton.setEnabled(!(SVNTeamPerformancePage.this.computeDeep | SVNTeamPerformancePage.this.preciseEnablements));
			}
		});
		
		this.enableCacheButton = new Button(composite, SWT.CHECK);
		this.enableCacheButton.setLayoutData(new GridData());
		this.enableCacheButton.setText(SVNUIMessages.PerformancePreferencePage_enableCache);
		this.enableCacheButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamPerformancePage.this.enableCache = SVNTeamPerformancePage.this.enableCacheButton.getSelection();
				SVNTeamPerformancePage.this.computeDeepButton.setEnabled(SVNTeamPerformancePage.this.enableCache);
				SVNTeamPerformancePage.this.preciseEnablementsButton.setEnabled(SVNTeamPerformancePage.this.enableCache);
			}
		});
		
		this.enablePersistentSSHConnectionButton = new Button(composite, SWT.CHECK);
		this.enablePersistentSSHConnectionButton.setLayoutData(new GridData());
		this.enablePersistentSSHConnectionButton.setText(SVNUIMessages.PerformancePreferencePage_enablePersistentSSHConnection);
		this.enablePersistentSSHConnectionButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				SVNTeamPerformancePage.this.enablePersistentSSHConnection = SVNTeamPerformancePage.this.enablePersistentSSHConnectionButton.getSelection();
			}
		});
		
//		this.enableFileReplacementAutoundoButton = new Button(composite, SWT.CHECK);
//		this.enableFileReplacementAutoundoButton.setLayoutData(new GridData());
//		this.enableFileReplacementAutoundoButton.setText(SVNUIMessages.PerformancePreferencePage_enableFileReplacementAutoundo);
//		this.enableFileReplacementAutoundoButton.addListener(SWT.Selection, new Listener() {
//			public void handleEvent (Event event) {
//				SVNTeamPerformancePage.this.enableFileReplacementAutoundo = SVNTeamPerformancePage.this.enableFileReplacementAutoundoButton.getSelection();
//			}
//		});
		
//		Setting context help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.team.svn.help.performancePreferencesContext"); //$NON-NLS-1$
		
		return composite;
	}
	
}
