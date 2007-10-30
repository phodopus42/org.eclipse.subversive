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

package org.eclipse.team.svn.ui.wizard;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.client.ISVNClientWrapper;
import org.eclipse.team.svn.core.client.PropertyData;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.AbstractNonLockingOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.IResourcePropertyProvider;
import org.eclipse.team.svn.core.operation.LoggedOperation;
import org.eclipse.team.svn.core.operation.SVNProgressMonitor;
import org.eclipse.team.svn.core.operation.local.AddToSVNOperation;
import org.eclipse.team.svn.core.operation.local.RefreshResourcesOperation;
import org.eclipse.team.svn.core.operation.local.RestoreProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.SaveProjectMetaOperation;
import org.eclipse.team.svn.core.operation.local.property.SetPropertiesOperation;
import org.eclipse.team.svn.core.operation.remote.CheckoutAsOperation;
import org.eclipse.team.svn.core.operation.remote.LocateProjectsOperation;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryResourceProvider;
import org.eclipse.team.svn.core.resource.IResourceProvider;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.action.remote.CheckoutAction;
import org.eclipse.team.svn.ui.extension.ExtensionsManager;
import org.eclipse.team.svn.ui.operation.GetRemoteFolderChildrenOperation;
import org.eclipse.team.svn.ui.operation.MoveProjectsToWorkingSetOperation;
import org.eclipse.team.svn.ui.operation.ObtainProjectNameOperation;
import org.eclipse.team.svn.ui.utility.DefaultOperationWrapperFactory;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.team.svn.ui.wizard.checkoutas.CheckoutAsFolderPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.CheckoutMethodSelectionPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.MultipleCheckoutMethodSelectionPage;
import org.eclipse.team.svn.ui.wizard.checkoutas.ProjectLocationSelectionPage;

/**
 * Checkout as wizard
 * 
 * @author Alexander Gurov
 */
public class CheckoutAsWizard extends AbstractSVNWizard {
	public static final int SIZING_WIZARD_WIDTH = 570;
	public static final int SIZING_WIZARD_HEIGHT = 500;
	
	protected HashMap names2resources;
	
	protected IRepositoryResource []resources;
	protected MultipleCheckoutMethodSelectionPage multipleMethodPage;
	protected CheckoutMethodSelectionPage methodSelectionPage;
	protected ProjectLocationSelectionPage locationSelectionPage;
	protected CheckoutAsFolderPage selectFolderPage;
	protected String projectName;
	protected boolean singleMode;
	protected IActionOperation priorOp;
	
	public CheckoutAsWizard(IRepositoryResource []resources) {
		this(resources, null);
	}
	
	public CheckoutAsWizard(IRepositoryResource []resources, IActionOperation priorOp) {
		super();
		this.setWindowTitle(SVNTeamUIPlugin.instance().getResource("CheckoutAsWizard.Title"));
		this.setForcePreviousAndNextButtons(true);
		this.resources = resources;
		this.singleMode = this.resources.length == 1;
		this.priorOp = priorOp;
	}
	
	public boolean isUseNewProjectWizard() {
		return this.methodSelectionPage == null ? false : this.methodSelectionPage.isUseNewProjectWizard();
	}
	
	public boolean isFindProjectsSelected() {
		return this.singleMode ? (this.methodSelectionPage == null ? false : this.methodSelectionPage.isFindProjectsSelected()) :
			this.multipleMethodPage == null ? false : this.multipleMethodPage.isFindProjectsSelected();
	}
	
	public boolean isCheckoutAsFolderSelected() {
		return this.singleMode ? (this.methodSelectionPage == null ? false : this.methodSelectionPage.isCheckoutAsFolderSelected()) :
			this.multipleMethodPage == null ? false : this.multipleMethodPage.isCheckoutAsFolderSelected();
	}
	
	public IContainer getTargetFolder() {
		return this.selectFolderPage.getTargetFolder();
	}

	public boolean isCheckoutRecursivelySelected() {
		return this.singleMode ? (this.methodSelectionPage == null ? true : this.methodSelectionPage.isCheckoutRecursivelySelected()) :
			this.multipleMethodPage == null ? true : this.multipleMethodPage.isCheckoutRecursivelySelected();
	}

	public String getProjectName() {
		return this.methodSelectionPage == null ? this.resources[0].getName() : this.methodSelectionPage.getProjectName();
	}
	
	public String getLocation() {
		return this.isCheckoutAsFolderSelected() ? FileUtility.getWorkingCopyPath(this.getTargetFolder()) : this.locationSelectionPage.getLocation();
	}
	
	public String getWorkingSetName() {
		return this.locationSelectionPage.getWorkingSetName();
	}

	public void addPages() {
		if (this.resources.length == 1) {
			this.projectName = this.fetchProjectName();
			GetRemoteFolderChildrenOperation op = new GetRemoteFolderChildrenOperation((IRepositoryContainer)this.resources[0], false);
			UIMonitorUtility.doTaskBusy(op, new DefaultOperationWrapperFactory() {
				public IActionOperation getLogged(IActionOperation operation) {
					return new LoggedOperation(operation);
				}
			});
			boolean isEclipseProject = false;
			if (op.getExecutionState() == IActionOperation.OK) {
				IRepositoryResource []children = op.getChildren();
				for (int i = 0; i < children.length; i++) {
					if (children[i].getName().equals(".project")) {
						isEclipseProject = true;
						break;
					}
				}
			}
			this.addPage(this.methodSelectionPage = new CheckoutMethodSelectionPage(this.projectName, !isEclipseProject));
		} else {
			this.addPage(this.multipleMethodPage = new MultipleCheckoutMethodSelectionPage(this.resources));
		}
		this.addPage(this.selectFolderPage = new CheckoutAsFolderPage(this.resources));
		this.addPage(this.locationSelectionPage = new ProjectLocationSelectionPage(this.resources.length > 1, null));
	}
	
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == this.selectFolderPage) {
			return null;
		}
		
		if (page instanceof CheckoutMethodSelectionPage) {
			if (this.methodSelectionPage.isUseNewProjectWizard() || this.methodSelectionPage.isFindProjectsSelected()) {
				return null;
			}
		} else if (page instanceof MultipleCheckoutMethodSelectionPage) {
			if (this.multipleMethodPage.isFindProjectsSelected()) {
				return null;
			}
		}
		
		if ((page == this.methodSelectionPage || page == this.multipleMethodPage) && !this.isCheckoutAsFolderSelected()) {
			return super.getNextPage(super.getNextPage(page));
		}
		
		return super.getNextPage(page);
	}

	public boolean performFinish() {
		if (this.isFindProjectsSelected()) {
			final CompositeOperation op = this.getLocateProjectsOperation(this.resources, this.isCheckoutRecursivelySelected());
			UIMonitorUtility.doTaskScheduledActive(op);
		}
		else if (this.obtainNames()) {
			this.doCheckout(this.getLocation(), this.getProjectName(), this.isUseNewProjectWizard(), this.isCheckoutRecursivelySelected(), this.getWorkingSetName());
		}
		return true;
	}

	protected String fetchProjectName() {
		ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(this.resources);
		UIMonitorUtility.doTaskNowDefault(obtainOperation, true);
		if (obtainOperation.getExecutionState() != IStatus.OK) {
			return this.resources[0].getName();
		}
		this.names2resources = ExtensionsManager.getInstance().getCurrentCheckoutFactory().prepareName2resources(obtainOperation.getNames2Resources());
		return (String)this.names2resources.keySet().iterator().next();
	}
	
	protected boolean obtainNames() {
		if (this.names2resources == null) {
			ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(this.resources);
			UIMonitorUtility.doTaskNowDefault(obtainOperation, true);
			if (obtainOperation.getExecutionState() != IStatus.OK) {
				return false;
			}
			this.names2resources = ExtensionsManager.getInstance().getCurrentCheckoutFactory().prepareName2resources(obtainOperation.getNames2Resources());
		}
		return true;
	}
	
	protected void doCheckout(String location, String projectName, boolean useNewProjectWizard, boolean checkoutRecursive, String workingSetName) {
		if (!useNewProjectWizard && this.names2resources.keySet().size() == 1) {
			Object resource = this.names2resources.get(this.names2resources.keySet().iterator().next());
			this.names2resources.clear();
			this.names2resources.put(projectName, resource);
		}
		ArrayList operateResources = new ArrayList();
		if (useNewProjectWizard) {
			operateResources.add(this.names2resources.get(this.names2resources.keySet().iterator().next()));
		}
		else {
			operateResources = CheckoutAction.getOperateResources(this.names2resources, CheckoutAction.getResources2Names(names2resources), this.getShell(), location, !this.isCheckoutAsFolderSelected());
		}
		
		if (operateResources.size() > 0) {
			IActionOperation op = null;
			if (useNewProjectWizard) {
				// this is the wrong way in the multithreaded environment, 
				// but it should work in the interactive environment of the Eclipse IDE
				// and, at the same time, right way is much complex and requires to override 
				// too many of built-in Eclipse Team-Services functionality
				ProjectAdditionListener listener = new ProjectAdditionListener();
				ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
				(new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow())).run();
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
			
				IProject selectedProject = listener.getProject();
				if (selectedProject != null) {
					op = this.prepareForOne((IRepositoryResource)operateResources.get(0), selectedProject.getName(), FileUtility.getResourcePath(selectedProject).removeLastSegments(1).toString(), true, checkoutRecursive, workingSetName);
				}
			}
			else if (this.isCheckoutAsFolderSelected()) {
				Map resources2Names = new HashMap();
				if (this.names2resources != null) {
					for (Iterator it = this.names2resources.entrySet().iterator(); it.hasNext(); ) {
						Map.Entry entry = (Map.Entry)it.next();
						resources2Names.put(entry.getValue(), entry.getKey());
					}
				}
				Map mappings = this.getExternalsFolderNames(resources, resources2Names);
				IResource destinationRoot = ResourcesPlugin.getWorkspace().getRoot().findMember(selectFolderPage.getTargetFolder().getFullPath());
				IRemoteStorage storage = SVNRemoteStorage.instance();
	    		ILocalResource localDest =  storage.asLocalResource(destinationRoot);
	    		if (localDest == null) {
	    			op = this.getCheckoutAsFolderOperationUnshared(this.getTargetFolder(), this.resources, mappings);
	    		}
	    		else {
	    			op = this.getCheckoutAsFolderOperation(this.getTargetFolder(), this.resources, mappings);
				}
			}
			else if (this.singleMode) {
				op = this.prepareForOne((IRepositoryResource)operateResources.get(0), projectName, location, false, checkoutRecursive, workingSetName);
			}
			else {
				HashMap operateMap = new HashMap();
				for (Iterator iter = operateResources.iterator(); iter.hasNext();) {
					IRepositoryResource resource = (IRepositoryResource)iter.next();
					HashMap resources2names = CheckoutAction.getResources2Names(this.names2resources);
					operateMap.put(resources2names.get(resource), resource);						
				}
				op = this.prepareForMultiple(operateMap, location, checkoutRecursive, workingSetName);
			}
			if (op != null) {
				if (this.priorOp != null) {
					CompositeOperation tmp = new CompositeOperation(op.getId());
					tmp.add(this.priorOp);
					tmp.add(op, new IActionOperation[] {this.priorOp});
					op = tmp;
				}
				UIMonitorUtility.doTaskScheduledActive(op);
			}
		}
	}
	
	protected IActionOperation getCheckoutAsFolderOperationUnshared(IContainer targetFolder, IRepositoryResource []resources, Map mappings) {
		CompositeOperation op = new CompositeOperation(SVNTeamUIPlugin.instance().getResource("Operation.CheckoutAsFolder"));
		for (int i = 0; i < resources.length; i++) {
			IPath location = FileUtility.getResourcePath(targetFolder);
			File target = location.append((String)mappings.get(resources[i])).toFile();
			op.add(new org.eclipse.team.svn.core.operation.file.CheckoutAsOperation(target, resources[i], this.isCheckoutRecursivelySelected(), false, false));
		}
		IResource []localResources = new IResource[] {targetFolder};
		op.add(new RefreshResourcesOperation(localResources), null);
		return op;
	}
	
	protected IActionOperation getCheckoutAsFolderOperation(IContainer targetFolder, IRepositoryResource []resources, Map mappings) {
		String externalsData = "";
		for (int i = 0; i < resources.length; i++) {
			String line = (String)mappings.get(resources[i]) + "\t" + SVNUtility.encodeURL(resources[i].getUrl()) + "\n";;
			externalsData += line;
		}
		
		CompositeOperation op = new CompositeOperation(SVNTeamUIPlugin.instance().getResource("Operation.CheckoutAsFolder"));
		IActionOperation [] dependency = null;
		IResource []localResources = new IResource[] {targetFolder};
		ILocalResource localResource = SVNRemoteStorage.instance().asLocalResource(targetFolder);
		IResource []newResources = null;
		if (IStateFilter.SF_NONVERSIONED.accept(targetFolder, localResource.getStatus(), localResource.getChangeMask())){
			newResources = FileUtility.addOperableParents(localResources, IStateFilter.SF_NONVERSIONED);
		}
		if (newResources != null && newResources.length > 0) {
			IActionOperation addToSVN = new AddToSVNOperation(newResources);
			op.add(addToSVN);
			dependency = new IActionOperation[] {addToSVN};
		}
		
		IResourcePropertyProvider concatenateProps = new CheckoutAsWizard.ConcatenateProperyDataOperation(targetFolder, PropertyData.EXTERNALS, externalsData.getBytes());
		op.add(concatenateProps, dependency);
		dependency = new IActionOperation[] {concatenateProps};
		
		SetPropertiesOperation setProps = new SetPropertiesOperation(localResources, concatenateProps, false);
		op.add(setProps, dependency);
		dependency = new IActionOperation[] {setProps};
		
		for (int i = 0; i < resources.length; i++) {
			IPath location = targetFolder.getLocation();
			if (location != null) {
				File target = location.append((String)mappings.get(resources[i])).toFile();
				op.add(new org.eclipse.team.svn.core.operation.file.CheckoutAsOperation(target, resources[i], this.isCheckoutRecursivelySelected(), false, false), dependency);
			}
		}
		op.add(new RefreshResourcesOperation(localResources), dependency);
		return op;
	}
	
	protected Map getExternalsFolderNames(IRepositoryResource []resources, Map resource2Name) {
		Map retVal = new HashMap();
		Set allNames = new HashSet();
		for (int i = 0; i < resources.length; i++) {
			String name = (String)resource2Name.get(resources[i]);
			if (name == null) {
				name = resources[i].getName();
			}
			name = this.getName(this.getTargetFolder(), name, allNames);
			retVal.put(resources[i], name);
			allNames.add(name);
		}
		return retVal;
	}
	
	protected String getName(IContainer targetFolder, String baseName, Set allNames) {
		baseName = baseName.replace(' ', '_');
		if (targetFolder == null || !targetFolder.exists(new Path(baseName))) {
			return baseName;
		}
		String name;
		for (int i = 1; true; i++)
		{
			name = baseName + "_(" + i + ")"; 
			if (!targetFolder.exists(new Path(name)) && !allNames.contains(name)) {
				break;
			}
		}
		return name;
	}
	
	protected CompositeOperation prepareForOne(IRepositoryResource resource, final String projectName, String location, boolean isUseNewProjectWizard, boolean checkoutRecursive, String workingSetName) {
		CheckoutAsOperation mainOp = new CheckoutAsOperation(projectName, resource, location, checkoutRecursive);
		
		CompositeOperation op = new CompositeOperation(mainOp.getId());

		if (isUseNewProjectWizard) {
			SaveProjectMetaOperation saveOp = new SaveProjectMetaOperation(new IResource[] {mainOp.getProject()}, "");
			op.add(saveOp);
			op.add(mainOp);
			mainOp.setRestoreOperation(new RestoreProjectMetaOperation(saveOp, true));
		}
		else {
			op.add(mainOp);
		}
		if (workingSetName != null) {
			op.add(new MoveProjectsToWorkingSetOperation(new IProject[] {mainOp.getProject()}, workingSetName));
		}
		
		return op;
	}
	
	protected CompositeOperation prepareForMultiple(HashMap name2resources, String location, boolean checkoutRecursive, String workingSetName) {
		CompositeOperation op = new CompositeOperation("");
		IResource []locals = new IResource[name2resources.keySet().size()];
		String name;
		int i = 0;
		for (Iterator iter = name2resources.keySet().iterator(); iter.hasNext(); i++) {
			name = (String)iter.next();	
			CheckoutAsOperation mainOp = new CheckoutAsOperation(name, (IRepositoryResource)name2resources.get(name), false, location, checkoutRecursive);
			locals[i] = mainOp.getProject();
			op.add(mainOp);
			op.setOperationName(mainOp.getId());
		}
		if (workingSetName != null) {
			op.add(new MoveProjectsToWorkingSetOperation(locals, workingSetName));
		}
		
		return op;
	}

	protected CompositeOperation getLocateProjectsOperation(IRepositoryResource[] resources, boolean checkoutRecursively) {
		LocateProjectsOperation mainOp = new LocateProjectsOperation(resources, ExtensionsManager.getInstance().getCurrentCheckoutFactory().getLocateFilter(), 5); //TODO level limitation now is hardcoded
		
		final CompositeOperation op = new CompositeOperation(mainOp.getId());
		op.add(mainOp);
		IRepositoryResourceProvider provider = ExtensionsManager.getInstance().getCurrentCheckoutFactory().additionalProcessing(op, mainOp);
		ObtainProjectNameOperation obtainOperation = new ObtainProjectNameOperation(provider);
		op.add(obtainOperation, new IActionOperation[] {mainOp});
		op.add(this.getCheckoutProjectOperation(resources, obtainOperation, checkoutRecursively), new IActionOperation[] {obtainOperation});
		
		return op;
	}
	
	protected AbstractActionOperation getCheckoutProjectOperation(final IRepositoryResource []resources, final ObtainProjectNameOperation obtainOperation, final boolean checkoutRecursively) {
		return new AbstractNonLockingOperation("Operation.CheckoutProjects") {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				UIMonitorUtility.getDisplay().syncExec(new Runnable() {
					public void run() {
						HashMap name2resources = obtainOperation.getNames2Resources();						
						if (name2resources.isEmpty()) {
							new MessageDialog(UIMonitorUtility.getShell(), getOperationResource("Title"), null, getOperationResource("Message"), MessageDialog.INFORMATION, new String[] {IDialogConstants.OK_LABEL}, 0).open();
							return;
						}
						CheckoutProjectsWizard wizard = new CheckoutProjectsWizard(resources, name2resources);
						WizardDialog dialog = new WizardDialog(UIMonitorUtility.getShell(), wizard);
						dialog.create();
						wizard.postInit();
						dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), SIZING_WIZARD_HEIGHT);
						if (dialog.open() == 0) {//finish button pressed
							List selection = wizard.getResultSelections();
							IActionOperation op;
							final Set projectNames;
							if (wizard.isCheckoutAsFoldersSelected()) {
								projectNames = null;
								op = CheckoutAsWizard.this.getCheckoutAsFolderOperation(wizard.getTargetFolder(), (IRepositoryResource [])selection.toArray(new IRepositoryResource[selection.size()]), name2resources);
							}
							else {
								HashMap selectedMap = new HashMap();
								List projects = new ArrayList();
								projectNames = name2resources.keySet();
								for (Iterator iter = projectNames.iterator(); iter.hasNext(); ) {
									String projName = (String)iter.next();
									if (wizard.getResultSelections().contains(name2resources.get(projName))) {
										selectedMap.put(projName, name2resources.get(projName));
										projects.add(name2resources.get(projName));
									}
								}
								op = ExtensionsManager.getInstance().getCurrentCheckoutFactory().getCheckoutOperation(CheckoutAsWizard.this.getShell(), (IRepositoryResource[])projects.toArray(new IRepositoryResource[projects.size()]), selectedMap, wizard.isRespectHierarchy(), wizard.getLocation(), checkoutRecursively);
							}
							if (op != null) {
								String wsName = wizard.getWorkingSetName();
								if (CheckoutAsWizard.this.priorOp != null || wsName != null) {
									CompositeOperation tmp = new CompositeOperation(op.getId());
									if (CheckoutAsWizard.this.priorOp != null) {
										tmp.add(CheckoutAsWizard.this.priorOp);
										tmp.add(op, new IActionOperation[] {CheckoutAsWizard.this.priorOp});
									}
									else {
										tmp.add(op);
									}
									if (wsName != null) {
										tmp.add(new MoveProjectsToWorkingSetOperation(new IResourceProvider() {
											public IResource[] getResources() {
												List projects = new ArrayList();
												for (Iterator it = projectNames.iterator(); it.hasNext(); ) {
													String name = (String)it.next();
													IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
													if (prj != null) {
														projects.add(prj);
													}
												}
												return (IProject [])projects.toArray(new IProject[projects.size()]);
											}
										}, wsName));
									}
									op = tmp;
								}
								UIMonitorUtility.doTaskScheduledActive(op);
							}
						}
					}
				});
			}
		};
	}
	
	public boolean canFinish() {
		IWizardPage currentPage = this.getContainer().getCurrentPage();
		if (((currentPage instanceof CheckoutMethodSelectionPage) || (currentPage instanceof MultipleCheckoutMethodSelectionPage)) &&
			this.isCheckoutAsFolderSelected()) {
			return false;
		}
		return super.canFinish();
	}
	
	protected class ProjectAdditionListener implements IResourceChangeListener {
		protected IProject project = null;
		
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta []deltas = event.getDelta().getAffectedChildren();
			for (int i = 0; i < deltas.length; i++) {
				IResource resource = deltas[i].getResource();
				if (resource instanceof IProject) {
					if (deltas[i].getKind() == IResourceDelta.ADDED) {
						this.project = (IProject)resource;
					}
					else if (deltas[i].getKind() == IResourceDelta.REMOVED && this.project == resource) {
						// wizard will be cancelled ?
						this.project = null;
					}
				}
			}
		}
		
		public IProject getProject() {
			return this.project;
		}
		
	}
	
	protected class ConcatenateProperyDataOperation extends AbstractNonLockingOperation implements IResourcePropertyProvider {
		protected IResource resource;
		protected String propertyName;
		protected byte[] concatenatedData;
		
		protected PropertyData property;
		
		public ConcatenateProperyDataOperation(IResource resource, String propertyName, byte[] concatenatedData) {
			super("Operation.ConcatenatePropertyData");
			this.resource = resource;
			this.propertyName = propertyName;
			this.concatenatedData = concatenatedData;
			this.property = new PropertyData(propertyName, null, concatenatedData);
		}

		protected void runImpl(IProgressMonitor monitor) throws Exception {
			final String wcPath = FileUtility.getWorkingCopyPath(resource);
			IRepositoryLocation location = SVNRemoteStorage.instance().getRepositoryLocation(resource);
			final ISVNClientWrapper proxy = location.acquireSVNProxy();
			PropertyData existingProperty;
			try {
				existingProperty = proxy.propertyGet(wcPath, PropertyData.EXTERNALS, null, null, new SVNProgressMonitor(CheckoutAsWizard.ConcatenateProperyDataOperation.this, monitor, null));
			}
			finally {
				location.releaseSVNProxy(proxy);
			}
			if (existingProperty != null && (existingProperty.value != null || existingProperty.data != null)) {
				byte[] existingData = existingProperty.value != null ? existingProperty.value.getBytes() : existingProperty.data;
				byte[] newData = new byte[existingData.length + this.concatenatedData.length];
				System.arraycopy(existingData, 0, newData, 0, existingData.length);
				System.arraycopy(this.concatenatedData, 0, newData, existingData.length, this.concatenatedData.length);
				this.property = new PropertyData(this.propertyName, null, newData);
			}
		}

		public IResource getLocal() {
			return this.resource;
		}

		public PropertyData[] getProperties() {
			return new PropertyData[] {this.property};
		}

		public IRepositoryResource getRemote() {
			return SVNRemoteStorage.instance().asRepositoryResource(this.resource);
		}

		public boolean isEditAllowed() {
			return false;
		}

		public void refresh() {
			
		}
		
		protected String getShortErrorMessage(Throwable t) {
			return MessageFormat.format(super.getShortErrorMessage(t), new String[] {this.propertyName, this.resource.getName()});
		}
		
	}
	
}
