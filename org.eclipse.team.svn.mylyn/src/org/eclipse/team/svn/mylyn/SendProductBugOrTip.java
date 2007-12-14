/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.mylyn;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;


/**
 * Allows to post the Subversive product bug or tip directly to Eclipse.org Bugzilla
 * 
 * @author Alexander Gurov
 */
public class SendProductBugOrTip {
	public static final String ECLIPSE_BUGZILLA_URL = "https://bugs.eclipse.org/bugs";
	
	protected TaskRepository repository;
	protected AbstractTaskDataHandler taskDataHandler;
	
	public static SendProductBugOrTip makeInstance() {
		TaskRepositoryManager manager = TasksUiPlugin.getRepositoryManager();
		TaskRepository repository = manager.getRepository(SendProductBugOrTip.ECLIPSE_BUGZILLA_URL);
		if (repository == null) {
			return null;
		}
		AbstractRepositoryConnector connector = manager.getRepositoryConnector(repository.getConnectorKind());
		AbstractTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
		if (taskDataHandler == null) {
			return null;
		}
		return new SendProductBugOrTip(repository, taskDataHandler);
	}
	
	public void doSend() {
		String kind = this.repository.getConnectorKind();
		AbstractAttributeFactory attributeFactory = this.taskDataHandler.getAttributeFactory(this.repository.getUrl(), kind, AbstractTask.DEFAULT_TASK_KIND);
		final RepositoryTaskData taskData = new RepositoryTaskData(attributeFactory, kind, this.repository.getUrl(), TasksUiPlugin.getDefault().getNextNewRepositoryTaskId());
		taskData.setNew(true);
		
//		TaskRepositoryManager manager = TasksUiPlugin.getRepositoryManager();
//		AbstractRepositoryConnector connector = manager.getRepositoryConnector(repository.getConnectorKind());
//		connector.createTaskFromTaskData(this.repository, taskData, false, new NullProgressMonitor());
		
		IActionOperation op = UIMonitorUtility.doTaskScheduledDefault(new AbstractActionOperation("Initialize Report") {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				if (!SendProductBugOrTip.this.taskDataHandler.initializeSubTaskData(SendProductBugOrTip.this.repository, taskData, taskData, monitor)) {
//				if (!SendProductBugOrTip.this.taskDataHandler.initializeTaskData(SendProductBugOrTip.this.repository, taskData, monitor)) {
//					throw new CoreException(new RepositoryStatus(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
//							RepositoryStatus.ERROR_REPOSITORY,
//							"The selected repository does not support creating new tasks."));
				}
			}
		}).getOperation();
		
		if (op.getExecutionState() == IActionOperation.OK) {
			taskData.setSummary("autogenerated summary");
			taskData.setDescription("autogenerated description (stack trace etc.)");
			taskData.setAttributeValue(RepositoryTaskAttribute.PRODUCT, "Subversive");
			taskData.setAttributeValue("severity", "normal");
			
			// open task editor
			NewTaskEditorInput editorInput = new NewTaskEditorInput(this.repository, taskData);
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			TasksUiUtil.openEditor(editorInput, TaskEditor.ID_EDITOR, page);
		}
	}
	
	private SendProductBugOrTip(TaskRepository repository, AbstractTaskDataHandler taskDataHandler) {
		this.repository = repository;
		this.taskDataHandler = taskDataHandler;
	}
	
}
