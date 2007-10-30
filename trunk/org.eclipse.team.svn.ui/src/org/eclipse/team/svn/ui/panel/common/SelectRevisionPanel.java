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

package org.eclipse.team.svn.ui.panel.common;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.team.svn.core.client.LogMessage;
import org.eclipse.team.svn.core.client.Revision;
import org.eclipse.team.svn.core.operation.AbstractNonLockingOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.remote.GetLogMessagesOperation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.StringMatcher;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.composite.LogMessagesComposite;
import org.eclipse.team.svn.ui.dialog.DefaultDialog;
import org.eclipse.team.svn.ui.panel.AbstractDialogPanel;
import org.eclipse.team.svn.ui.panel.view.HistoryFilterPanel;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Select resource revision panel implementation
 * 
 * @author Alexander Gurov
 */
public class SelectRevisionPanel extends AbstractDialogPanel {
	protected LogMessagesComposite history;
	protected LogMessage []logMessages;
	protected IRepositoryResource resource;
	protected long currentRevision;
	protected long []selectedRevisions;
	protected String selectedMessage;
	protected int selectionStyle;
	protected long limit;
	protected boolean pagingEnabled;
	protected LogMessage[] selectedLogMessages;

	protected Text resourceLabel;
	
	protected ToolItem hideUnrelatedItem;
	protected ToolItem stopOnCopyItem;
	protected ToolItem pagingItem;
	protected ToolItem pagingAllItem;
	protected ToolItem filterItem;
	protected ToolItem clearFilterItem;
	protected ToolItem refreshItem;
	protected ToolItem separator;
	
	protected boolean isCommentFilterEnabled = false;
	protected String filterByComment;
	protected String filterByAuthor;
	protected ISelectionChangedListener tableViewerListener;
	protected IPropertyChangeListener configurationListener;
	protected boolean initialStopOnCopy;

	public SelectRevisionPanel(GetLogMessagesOperation msgOp, int selectionStyle) {
		this(msgOp, selectionStyle, Revision.SVN_INVALID_REVNUM);
    }

    public SelectRevisionPanel(GetLogMessagesOperation msgOp, int selectionStyle, long currentRevision) {
    	this.selectionStyle = selectionStyle;
        this.dialogTitle = SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.Title");
        this.dialogDescription = SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.Description");
        this.defaultMessage = SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.Message");
		this.resource = msgOp.getResource();
		this.selectedRevisions = null;
		this.currentRevision = currentRevision;
		this.filterByAuthor = "";
		this.filterByComment = "";
    	this.logMessages = msgOp.getMessages();
    	this.initialStopOnCopy = msgOp.getStopOnCopy();
	}
    
	public String getHelpId() {
    	return "org.eclipse.team.svn.help.revisionLinkDialogContext";
	}

	public long []getSelectedRevisions() {
		return this.selectedRevisions;
	}
	public long getSelectedRevision() {
		return this.selectedRevisions[0];
	}

	public String getSelectedMessage() {
		return this.selectedMessage;
	}

    public String getImagePath() {
        return "icons/dialogs/select_revision.gif";
    }
    
    public void postInit() {
        this.manager.setButtonEnabled(0, false);
    }

    public Point getPrefferedSize() {
        return new Point(700, SWT.DEFAULT);
    }
    
    public void createControls(Composite parent) {
    	IPreferenceStore store = SVNTeamUIPlugin.instance().getPreferenceStore();
    	
    	GridData data;
        GridLayout layout;
        
        Composite labelAndToolbar = new Composite(parent, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = layout.marginWidth = 0;
        layout.numColumns = 2;
        labelAndToolbar.setLayout(layout);
        
        data = new GridData(GridData.FILL_HORIZONTAL);
        labelAndToolbar.setLayoutData(data);  
        
        //Create resource image and label        
        Composite resourceLabelComposite = new Composite(labelAndToolbar, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		resourceLabelComposite.setLayout(layout);
		resourceLabelComposite.setLayoutData(data);
		
		this.resourceLabel = new Text(resourceLabelComposite, SWT.LEFT);
		this.resourceLabel.setEditable(false);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		this.resourceLabel.setLayoutData(data);
        
		// Create the toolbar
        ToolBarManager toolBarMgr = new ToolBarManager(SWT.FLAT);
        final ToolBar toolBar = toolBarMgr.createControl(labelAndToolbar);
        data = new GridData();
        data.horizontalAlignment = GridData.END;
        toolBar.setLayoutData(data);
         
    	this.hideUnrelatedItem = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
    	this.stopOnCopyItem = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
        this.filterItem = new ToolItem(toolBar, SWT.FLAT);
    	this.clearFilterItem = new ToolItem(toolBar, SWT.FLAT);
    	this.separator = new ToolItem(toolBar, SWT.SEPARATOR);
    	this.pagingItem = new ToolItem(toolBar, SWT.FLAT);
    	this.pagingAllItem = new ToolItem(toolBar, SWT.FLAT);
    	this.separator = new ToolItem(toolBar, SWT.SEPARATOR);
    	this.refreshItem = new ToolItem(toolBar, SWT.FLAT);
    	
    	this.hideUnrelatedItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/hide_unrelated.gif").createImage());
    	this.stopOnCopyItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/stop_on_copy.gif").createImage());
    	this.stopOnCopyItem.setSelection(this.initialStopOnCopy);
    	this.filterItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/filter.gif").createImage());
    	this.clearFilterItem.setDisabledImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/clear.gif").createImage());
	    this.clearFilterItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/clear_filter.gif").createImage());
    	this.pagingItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/paging.gif").createImage());
    	this.pagingAllItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/views/history/paging_all.gif").createImage());
    	this.refreshItem.setImage(SVNTeamUIPlugin.instance().getImageDescriptor("icons/common/refresh.gif").createImage());
    	
    	this.hideUnrelatedItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.Unrelated"));
    	this.stopOnCopyItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.StopOnCopy"));
    	this.filterItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.QuickFilter"));
    	this.clearFilterItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.ClearFilter"));
    	this.pagingAllItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.ShowAll"));
    	this.refreshItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.Refresh"));
    	
    	Composite group = new Composite(parent, SWT.BORDER);
        layout = new GridLayout();
        layout.marginHeight = layout.marginWidth = 0;
        layout.numColumns = 1;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
    	
    	this.history = new LogMessagesComposite(group, 75, this.selectionStyle);
    	data = new GridData(GridData.FILL_BOTH);
    	data.heightHint = 350;
    	this.history.setLayoutData(data);
        this.history.setFocus();
        this.tableViewerListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (SelectRevisionPanel.this.manager != null) {
					if (event != null) {
						SelectRevisionPanel.this.manager.setButtonEnabled(0, !event.getSelection().isEmpty());
					}
					else {
						SelectRevisionPanel.this.manager.setButtonEnabled(0, !SelectRevisionPanel.this.history.getTableViewer().getSelection().isEmpty());
					}
				}
			}
		};
		this.history.getTableViewer().addSelectionChangedListener(this.tableViewerListener);
    	
    	if (SVNTeamPreferences.getHistoryBoolean(store, SVNTeamPreferences.HISTORY_PAGING_ENABLE_NAME)) {
    		this.limit = SVNTeamPreferences.getHistoryInt(store, SVNTeamPreferences.HISTORY_PAGE_SIZE_NAME);
    		String msg = SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.ShowNextX");
    	    this.pagingItem.setToolTipText(MessageFormat.format(msg, new String[] {String.valueOf(this.limit)}));
    	    this.pagingEnabled = true;
        }
        else {
        	this.limit = 0;
    	    this.pagingItem.setToolTipText(SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.ShowNextPage"));
    	    this.pagingEnabled = false;
        }
    	this.pagingEnabled = SelectRevisionPanel.this.limit > 0 && this.logMessages.length == SelectRevisionPanel.this.limit;
		this.showMessages(null);
		
        this.setPagingEnabled();
        this.clearFilterItem.setEnabled(false);
    	
    	this.hideUnrelatedItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				SelectRevisionPanel.this.history.setShowRelatedPathsOnly(SelectRevisionPanel.this.hideUnrelatedItem.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});
    	this.stopOnCopyItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				SelectRevisionPanel.this.refresh();
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});
    	
        this.filterItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				if (SelectRevisionPanel.this.quickFilter()) {
					SelectRevisionPanel.this.showMessages(null);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});
        this.clearFilterItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				SelectRevisionPanel.this.clearFilter();
				SelectRevisionPanel.this.showMessages(null);
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});
        
    	this.pagingItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				SelectRevisionPanel.this.showNextPage(false);
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});    	
    	this.pagingAllItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				SelectRevisionPanel.this.showNextPage(true);
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});
    	
    	this.refreshItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				SelectRevisionPanel.this.refresh();
			}
			public void widgetDefaultSelected(SelectionEvent e) {			
			}    		
    	});
        this.showResourceLabel();
    }
    
    public LogMessage[] getSelectedLogMessages() {
        return this.selectedLogMessages;
    }
    
    protected void showResourceLabel() {
		String resourceName = SVNTeamUIPlugin.instance().getResource("SelectRevisionPanel.NotSelected");
		if (this.resource != null) {
		    resourceName = this.resource.getUrl();
		}
		this.resourceLabel.setText(resourceName);
	}

    protected void saveChanges() {
		this.selectedRevisions = this.history.getSelectedRevisions();
		this.selectedMessage = this.history.getSelectedMessage();
		this.selectedLogMessages = this.history.getSelectedLogMessages();
    }
    
    protected void cancelChanges() {
        
    }

    protected void addPage(LogMessage[] newMessages) {
    	if (this.logMessages == null) {
			this.logMessages = newMessages;
		}
		else {
			List oldList = new ArrayList(Arrays.asList(this.logMessages));
			List newList = Arrays.asList(newMessages);
			if (newList.size() > 1) {
				newList = newList.subList(1, newList.size());
				oldList.addAll(newList);
			}		
			this.logMessages = (LogMessage [])oldList.toArray(new LogMessage[oldList.size()]);		
		}
    }
    
    protected void setPagingEnabled() {
    	this.pagingEnabled &= this.resource != null && this.limit > 0;
    	this.pagingItem.setEnabled(this.pagingEnabled);
	    this.pagingAllItem.setEnabled(this.pagingEnabled);
	    this.filterItem.setEnabled(this.resource != null && this.logMessages != null);
	    this.clearFilterItem.setEnabled(this.isFilterEnabled() && this.logMessages != null);
    }
    
    protected void showMessages(final GetLogMessagesOperation msgsOp) {
    	Table table = this.history.getTableViewer().getTable();
		final TableItem[] selected = table.getSelection();
    	IActionOperation showOp = new AbstractNonLockingOperation("Operation.ShowMessages") {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				 SVNTeamUIPlugin.instance().getWorkbench().getDisplay().syncExec(new Runnable() {
					 public void run() {
						 if (msgsOp != null && msgsOp.getExecutionState() == IActionOperation.OK) {
							 SelectRevisionPanel.this.pagingEnabled = SelectRevisionPanel.this.limit > 0 && SelectRevisionPanel.this.logMessages == null ? msgsOp.getMessages().length == SelectRevisionPanel.this.limit : msgsOp.getMessages().length == SelectRevisionPanel.this.limit + 1;
							 SelectRevisionPanel.this.addPage(msgsOp.getMessages());
						 }
						 LogMessage[] toShow = SelectRevisionPanel.this.isFilterEnabled() && SelectRevisionPanel.this.logMessages != null ? SelectRevisionPanel.this.filterMessages(SelectRevisionPanel.this.logMessages) : SelectRevisionPanel.this.logMessages;
						 Revision current = SelectRevisionPanel.this.currentRevision != -1 ? Revision.getInstance(SelectRevisionPanel.this.currentRevision) : null;
						 SelectRevisionPanel.this.history.setLogMessages(current, toShow, SelectRevisionPanel.this.resource);
						 SelectRevisionPanel.this.setPagingEnabled();
					 }
				 });
			}
		};
		IActionOperation selectOp = new AbstractNonLockingOperation("Operation.SaveTableSelection") {
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				SVNTeamUIPlugin.instance().getWorkbench().getDisplay().syncExec(new Runnable() {
					public void run() {
						Table table = SelectRevisionPanel.this.history.getTableViewer().getTable();
					    if (table.getItems().length > 0) {
					        if (selected.length != 0) {
					        	table.setSelection(selected);
					        }
					        SelectRevisionPanel.this.history.getHistoryTableListener().selectionChanged(null);
					        ISelectionChangedListener listener = SelectRevisionPanel.this.tableViewerListener;
					        if (listener != null) {
					        	SelectRevisionPanel.this.tableViewerListener.selectionChanged(null);
					        }
					    }
					}
				});
			}
		};
		CompositeOperation op = new CompositeOperation(showOp.getId());
		if (msgsOp != null) {
			op.add(msgsOp);
		}
		op.add(showOp);
		op.add(selectOp, new IActionOperation[] {showOp});
		UIMonitorUtility.doTaskNowDefault(op, true);
    }
    
    protected void showNextPage(boolean showAll) {
    	if (this.resource != null) {
    		final GetLogMessagesOperation msgsOp = new GetLogMessagesOperation(this.resource, this.stopOnCopyItem.getSelection());
    		msgsOp.setLimit(showAll ? 0 : this.logMessages == null ? this.limit : this.limit + 1);
    		Revision revision = this.resource.getSelectedRevision();
    		if (this.logMessages != null && this.logMessages.length > 1) {
    			LogMessage lm = this.logMessages[this.logMessages.length - 1];
    			revision = Revision.getInstance(lm.revision);
    		}    		
    		msgsOp.setSelectedRevision(revision);
    		this.showMessages(msgsOp);
    	}
    }
    protected boolean quickFilter() {
	    boolean okPressed = false;
	    HistoryFilterPanel panel = new HistoryFilterPanel(this.filterByAuthor, this.filterByComment, this.getSelectedAuthors(), this.isCommentFilterEnabled);
	    DefaultDialog dialog = new DefaultDialog(UIMonitorUtility.getDisplay().getActiveShell(), panel);
	    if (dialog.open() == 0) {
	    	okPressed = true;
	        this.filterByAuthor = panel.getAuthor(); 
	        this.filterByComment = panel.getComment();
	        this.isCommentFilterEnabled = panel.isCommentFilterEnabled();
	    }
	    return okPressed;
	}
    
    protected void clearFilter() {
	    this.filterByAuthor = ""; 
	    this.filterByComment = "";
        this.isCommentFilterEnabled = false;
	}
	
	protected boolean isFilterEnabled() {
	    return this.filterByAuthor.length() > 0 || this.isCommentFilterEnabled; 
	}
	
	protected LogMessage[] filterMessages(LogMessage[] msgs) {
	    ArrayList filteredMessages = new ArrayList();
	    for (int i = 0; i < msgs.length; i++) {
			String author = msgs[i].author;
			String message = msgs[i].message;
			StringMatcher authorMatcher = new StringMatcher(this.filterByAuthor);
			StringMatcher commentMatcher = new StringMatcher(this.filterByComment);
			if (this.filterByAuthor.length() > 0 ? authorMatcher.match(author) : true) {
				if (this.isCommentFilterEnabled ? commentMatcher.match(message) : true) {
			        filteredMessages.add(msgs[i]);
			    }
			}
	    }
	    LogMessage []result = (LogMessage [])filteredMessages.toArray(new LogMessage[filteredMessages.size()]);
	    return result.length > 0 ? result : null;	    
	}
	
	protected String[] getSelectedAuthors() {
		List authors = new ArrayList();
		if (this.logMessages != null) {
			for (int i = 0; i < logMessages.length; i++) {
				String current = logMessages[i].author;
				if (current != null && !authors.contains(current)) {
					authors.add(current);
				}
			}
		}
		return (String[])authors.toArray(new String[authors.size()]);
	}
	
	protected void refresh() {
		long revision = this.history.getSelectedRevision();
		this.pagingEnabled = true;
		this.logMessages = null;
		this.showNextPage(false);
		this.history.setSelectedRevision(revision);
	}
}
