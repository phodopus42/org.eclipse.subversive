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

package org.eclipse.team.svn.ui.compare;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.svn.core.connector.SVNDiffStatus;
import org.eclipse.team.svn.core.connector.SVNEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.AbstractGetFileContentOperation;
import org.eclipse.team.svn.core.operation.CompositeOperation;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.GetLocalFileContentOperation;
import org.eclipse.team.svn.core.operation.remote.GetFileContentOperation;
import org.eclipse.team.svn.core.operation.remote.LocateResourceURLInHistoryOperation;
import org.eclipse.team.svn.core.resource.ILocalFile;
import org.eclipse.team.svn.core.resource.ILocalResource;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.utility.ProgressMonitorUtility;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.repository.model.RepositoryFolder;
import org.eclipse.team.svn.ui.utility.UIMonitorUtility;

/**
 * Compare editor input for the versioned trees
 * 
 * @author Alexander Gurov
 */
public abstract class ResourceCompareInput extends CompareEditorInput {
	protected ResourceCompareViewer viewer;
	protected BaseCompareNode root;
	
	protected IRepositoryResource rootLeft;
	protected IRepositoryResource rootAncestor;
	protected IRepositoryResource rootRight;
	
	public ResourceCompareInput(CompareConfiguration configuration) {
		super(configuration);
	}

	public void initialize(IProgressMonitor monitor) throws Exception {
		this.refreshTitles();
	}
	
	public final Viewer createDiffViewer(Composite parent) {
		return this.viewer = this.createDiffViewerImpl(parent, this.getCompareConfiguration());
	}
	
	protected ResourceCompareViewer createDiffViewerImpl(Composite parent, CompareConfiguration config) {
		return new ResourceCompareViewer(parent, config);
	}
	
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (this.root != null) {
			ResourceElement left = (ResourceElement)this.root.getLeft();
			ResourceElement ancestor = (ResourceElement)this.root.getAncestor();
			ResourceElement right = (ResourceElement)this.root.getRight();
			
			//TODO additionally decorate resources in order to show property changes
			if ((left.getType() == ITypedElement.FOLDER_TYPE || ancestor != null && ancestor.getType() == ITypedElement.FOLDER_TYPE || right.getType() == ITypedElement.FOLDER_TYPE) && 
				(this.root.getKind() & Differencer.CHANGE_TYPE_MASK) != 0) {
				this.root = (BaseCompareNode)this.root.getParent();
			}
			ProgressMonitorUtility.doTaskExternal(this.root.getFetcher(), monitor);
		}
		monitor.done();
		return this.root;
	}

	protected void findRootNode(Map path2node, IRepositoryResource resource, IProgressMonitor monitor) {
		this.root = (BaseCompareNode)path2node.get(new Path(resource.getUrl()));
		if (this.root == null && !path2node.isEmpty()) {
			LocateResourceURLInHistoryOperation op = new LocateResourceURLInHistoryOperation(new IRepositoryResource[] {resource}, false);
			UIMonitorUtility.doTaskExternalDefault(op, monitor);
			IRepositoryResource converted = op.getRepositoryResources()[0];
			this.root = (BaseCompareNode)path2node.get(new Path(converted.getUrl()));
		}
	}
	
	protected void refreshTitles() throws Exception {
		if (this.root == null) {
			return;
		}
		CompareConfiguration cc = this.getCompareConfiguration();
		
		cc.setLeftLabel(this.getLeftLabel());
		cc.setLeftImage(this.getLeftImage());
		
		cc.setRightLabel(this.getRightLabel());
		cc.setRightImage(this.getRightImage());
		
		ResourceElement left = this.getLeftResourceElement();
		String leftRevisionPart = this.getRevisionPart(left);
		String leftResourceName = left.getName();
		ResourceElement right = this.getRightResourceElement();
		String rightRevisionPart = this.getRevisionPart(right);
		String rightResourceName = right.getName();
		
		if (this.isThreeWay()) {
			cc.setAncestorLabel(this.getAncestorLabel());
			cc.setAncestorImage(this.getAncestorImage());
			
			ResourceElement ancestor = this.getAncestorResourceElement();
			String ancestorRevisionPart = this.getRevisionPart(ancestor);
			String ancestorResourceName = ancestor.getName();
			
			String leftPart = leftResourceName + " [" + leftRevisionPart;
			String ancestorPart = " ";
			String rightPart = " ";
			boolean leftEquals = leftResourceName.equals(ancestorResourceName);
			boolean rightEquals = rightResourceName.equals(ancestorResourceName);
			if (leftEquals) {
				leftPart += " ";
				if (rightEquals) {
					ancestorPart += ancestorRevisionPart + " ";
					rightPart += rightRevisionPart + "]";
				}
				else {
					ancestorPart += ancestorRevisionPart + "] ";
					rightPart += rightResourceName + " [" + rightRevisionPart + "]";
				}
			}
			else if (rightEquals) {
				leftPart += "] ";
				ancestorPart += ancestorResourceName + " [" + ancestorRevisionPart + " ";
				rightPart += rightRevisionPart + "]";
			}
			else {
				leftPart += "] ";
				ancestorPart += ancestorResourceName + " [" + ancestorRevisionPart + "] ";
				rightPart += rightResourceName + " [" + rightRevisionPart + "]";
			}

			String format = CompareUI.getResourceBundle().getString("ResourceCompare.threeWay.title");
			this.setTitle(MessageFormat.format(format, new String[] {leftPart, ancestorPart, rightPart}));	
		} 
		else {
			String leftPart = leftResourceName + " [" + leftRevisionPart;
			String rightPart = " ";
			if (leftResourceName.equals(rightResourceName)){
				leftPart += " ";
				rightPart += rightRevisionPart + "]";
			}
			else {
				leftPart += "] ";
				rightPart += rightResourceName + " [" + rightRevisionPart + "]";
			}
			
			String format = CompareUI.getResourceBundle().getString("ResourceCompare.twoWay.title");
			this.setTitle(MessageFormat.format(format, new String[] {leftPart, rightPart}));
		}
	}
	
	protected String getAncestorLabel() throws Exception {
		return this.getLabel(this.getAncestorResourceElement());
	}

	protected Image getAncestorImage() throws Exception {
		return CompareUI.getImage(RepositoryFolder.wrapChild(null, this.getAncestorResourceElement().getRepositoryResource()));
	}

	protected String getLeftLabel() throws Exception {
		return this.getLabel(this.getLeftResourceElement());
	}
	
	protected Image getLeftImage() throws Exception {
		return CompareUI.getImage(RepositoryFolder.wrapChild(null, this.getLeftResourceElement().getRepositoryResource()));
	}
	
	protected String getRightLabel() throws Exception {
		return this.getLabel(this.getRightResourceElement());
	}
	
	protected Image getRightImage() throws Exception {
		return CompareUI.getImage(RepositoryFolder.wrapChild(null, this.getRightResourceElement().getRepositoryResource()));
	}
	
	protected String getLabel(ResourceElement element) throws Exception {
		return element.getRepositoryResource().getUrl() + " [" + this.getRevisionPart(element) + "]";
	}
	
	protected String getRevisionPart(ResourceElement element) throws Exception {
		IRepositoryResource resource = element.getRepositoryResource();
		SVNRevision selected = resource.getSelectedRevision();
		if (selected == SVNRevision.INVALID_REVISION) {
			return SVNTeamUIPlugin.instance().getResource("ResourceCompareInput.ResourceIsNotAvailable");
		}
		String msg = SVNTeamUIPlugin.instance().getResource("ResourceCompareInput.RevisionSign");
		return MessageFormat.format(msg, new String[] {String.valueOf(resource.getRevision())});
	}
	
	protected ResourceElement getLeftResourceElement() {
		DiffNode node = this.getSelectedNode();
		if (node != null) {
			return (ResourceElement)node.getLeft();
		}
		return (ResourceElement)this.root.getLeft();
	}
	
	protected ResourceElement getRightResourceElement() {
		DiffNode node = this.getSelectedNode();
		if (node != null) {
			return (ResourceElement)node.getRight();
		}
		return (ResourceElement)this.root.getRight();
	}
	
	protected ResourceElement getAncestorResourceElement() {
		DiffNode node = this.getSelectedNode();
		if (node != null) {
			return (ResourceElement)node.getAncestor();
		}
		return (ResourceElement)this.root.getAncestor();
	}
	
	protected DiffNode getSelectedNode() {
		if (this.viewer != null) {
			IStructuredSelection selection = (IStructuredSelection)this.viewer.getSelection();
			if (selection != null && !selection.isEmpty() && selection.getFirstElement() instanceof DiffNode) {
				return (DiffNode)selection.getFirstElement();
			}
		}
		return null;
	}
	
	protected IDiffContainer getParentCompareNode(IRepositoryResource current, Map path2node) throws Exception {
		IRepositoryResource parent = current.getParent();
		if (parent == null) {
			return null;
		}
		
		Path parentUrl = new Path(parent.getUrl());
		IDiffContainer node = (IDiffContainer)path2node.get(parentUrl);
		if (node == null) {
			path2node.put(parentUrl, node = this.makeStubNode(this.getParentCompareNode(parent, path2node), parent));
		}
		return node;
	}
	
	protected static int getDiffKind(int textStatus, int propStatus) {
		if (textStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.ADDED ||
			textStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.UNVERSIONED) {
			return Differencer.ADDITION;
		}
		if (textStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.DELETED) {
			return Differencer.DELETION;
		}
		if (textStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.REPLACED) {
			return Differencer.CHANGE;
		}
		if (textStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.MODIFIED ||
			propStatus == org.eclipse.team.svn.core.connector.SVNEntryStatus.Kind.MODIFIED) {
			return Differencer.CHANGE;
		}
		return Differencer.NO_CHANGE;
	}
	
	protected int getNodeKind(SVNDiffStatus st) {
		return SVNUtility.getNodeKind(st.pathPrev, st.nodeKind, false);
	}
	
	protected IRepositoryResource createResourceFor(IRepositoryLocation location, int kind, String url) {
		IRepositoryResource retVal = null;
		if (kind == SVNEntry.Kind.FILE) {
			retVal = location.asRepositoryFile(url, false);
		}
		else if (kind == SVNEntry.Kind.DIR) {
			retVal = location.asRepositoryContainer(url, false);
		}
		if (retVal == null) {
			throw new RuntimeException(SVNTeamUIPlugin.instance().getResource("Error.CompareUnknownNodeKind"));
		}
		return retVal;
	}
	
	protected abstract boolean isThreeWay();
	protected abstract IDiffContainer makeStubNode(IDiffContainer parent, IRepositoryResource node);
	
	public class ResourceElement implements ITypedElement, IStreamContentAccessor, IContentChangeNotifier, IEditableContent {
		protected Vector listenerList;
		protected boolean dirty;
		
		protected IRepositoryResource resource;
		protected AbstractGetFileContentOperation op;
		protected ILocalResource localAlias;
		protected boolean editable;
		
		public ResourceElement(IRepositoryResource resource, ILocalResource alias, boolean showContent) {
			this.resource = resource;
			this.localAlias = alias;
			this.editable = false;
			this.listenerList = new Vector();
			if (!showContent) {
				this.resource.setSelectedRevision(SVNRevision.INVALID_REVISION);
			}
		}
		
		public void addContentChangeListener(IContentChangeListener listener) {
			this.listenerList.add(listener);
		}
		
		public void removeContentChangeListener(IContentChangeListener listener) {
			this.listenerList.remove(listener);
		}
		
		public boolean isDirty() {
			return this.dirty;
		}
		
		public boolean isEditable() {
			return this.editable && this.localAlias instanceof ILocalFile;
		}

		public void setEditable(boolean editable) {
			this.editable = editable;
		}

		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			return dest;
		}
		
		public void commit(IProgressMonitor pm) throws CoreException {
			if (this.isDirty()) {
				IFile file = (IFile)this.localAlias.getResource();
				file.refreshLocal(IResource.DEPTH_ZERO, pm);
				this.dirty = false;
			}
		}

		public void setContent(byte[] newContent) {
			if (this.isEditable()) {
				if (this.op != null) {
					this.op.setContent(newContent);
					this.fireContentChanged();
				}
			}
		}
		
		public IRepositoryResource getRepositoryResource() {
			return this.resource;
		}
		
		public ILocalResource getLocalResource() {
			return this.localAlias;
		}
		
		public String getName() {
			return this.resource.getName();
		}
	
		public Image getImage() {
			return CompareUI.getImage(RepositoryFolder.wrapChild(null, this.resource));
		}
	
		public String getType() {
			if (this.resource instanceof IRepositoryContainer) {
				return ITypedElement.FOLDER_TYPE;
			}
			String fileName = this.resource.getName();
			int dotIdx = fileName.lastIndexOf('.');
			return dotIdx == -1 ? ITypedElement.UNKNOWN_TYPE : fileName.substring(dotIdx + 1);
		}

		public AbstractGetFileContentOperation getFetcher() {
			if (this.op != null && this.op.getExecutionState() == IActionOperation.OK) {
				return null;
			}
			if (this.resource.getSelectedRevision() != SVNRevision.INVALID_REVISION && this.resource instanceof IRepositoryFile) {
				int revisionKind = this.resource.getSelectedRevision().getKind();
				return this.op = revisionKind == SVNRevision.Kind.WORKING || revisionKind == SVNRevision.Kind.BASE ? 
					(AbstractGetFileContentOperation)new GetLocalFileContentOperation(this.localAlias.getResource(), revisionKind) : 
					new GetFileContentOperation(this.resource);
			}
			return this.op = null;
		}
	
		public InputStream getContents() throws CoreException {
			return this.op == null || this.op.getExecutionState() != IActionOperation.OK ? null : this.op.getContent();
		}
	
		protected void fireContentChanged() {
			this.dirty = true;
			Object []listeners = this.listenerList.toArray();
			for (int i= 0; i < listeners.length; i++) {
				((IContentChangeListener)listeners[i]).contentChanged(this);
			}
		}

	}

	protected class ResourceCompareViewer extends DiffTreeViewer {
		public ResourceCompareViewer(Composite parent, CompareConfiguration configuration) {
			super(parent, configuration);
		}
		
		protected void handleOpen(final SelectionEvent event) {
			final BaseCompareNode node = (BaseCompareNode)((TreeItem)event.item).getData();
			CompositeOperation fetchContent = node.getFetcher();
			fetchContent.add(new AbstractActionOperation("Operation.FetchContent") {
				protected void runImpl(IProgressMonitor monitor) throws Exception {
					final Throwable []t = new Throwable[1];
					UIMonitorUtility.getDisplay().syncExec(new Runnable() {
						public void run() {
							try {
								ResourceCompareInput.this.refreshTitles();
								ResourceCompareViewer.super.handleOpen(event);
							} 
							catch (Exception e) {
								t[0] = e;
							}
						}
					});
					if (t[0] != null ){
						this.reportError(t[0]);
					}
				}
			});
			UIMonitorUtility.doTaskNowDefault(fetchContent, true);
		}
		
		protected class LabelProviderWrapper implements ILabelProvider {
			protected Map images;
			protected ILabelProvider baseProvider;
			
			public LabelProviderWrapper(ILabelProvider baseProvider) {
				this.images = new HashMap();
				this.baseProvider = baseProvider;
			}
			
			public void addListener(ILabelProviderListener listener) {
				this.baseProvider.addListener(listener);
			}
			
			public void removeListener(ILabelProviderListener listener) {
				this.baseProvider.removeListener(listener);
			}
			
			public boolean isLabelProperty(Object element, String property) {
				return this.baseProvider.isLabelProperty(element, property);
			}
			
			public String getText(Object element) {
				return this.baseProvider.getText(element);
			}
			
			public Image getImage(Object element) {
				return this.baseProvider.getImage(element);
			}
			
			public void dispose() {
				for (Iterator it = this.images.values().iterator(); it.hasNext(); ) {
					((Image)it.next()).dispose();
				}
				this.baseProvider.dispose();
			}
			
		}
	}
	
	protected class BaseCompareNode extends DiffNode {
		public BaseCompareNode(IDiffContainer parent, int kind) {
			super(parent, kind);
		}
		
		public CompositeOperation getFetcher() {
			ResourceElement left = (ResourceElement)this.getLeft();
			ResourceElement ancestor = (ResourceElement)this.getAncestor();
			ResourceElement right = (ResourceElement)this.getRight();
			CompositeOperation op = new CompositeOperation(SVNTeamUIPlugin.instance().getResource("ResourceCompareInput.Fetch"));
			
			if (left != null && left.getType() != ITypedElement.FOLDER_TYPE) {
				AbstractGetFileContentOperation fetchOp = left.getFetcher();
				if (fetchOp != null) {
					op.add(fetchOp);
				}
			}
			if (ancestor != null && ancestor.getType() != ITypedElement.FOLDER_TYPE) {
				AbstractGetFileContentOperation fetchOp = ancestor.getFetcher();
				if (fetchOp != null) {
					op.add(fetchOp);
				}
			}
			if (right != null && right.getType() != ITypedElement.FOLDER_TYPE) {
				AbstractGetFileContentOperation fetchOp = right.getFetcher();
				if (fetchOp != null) {
					op.add(fetchOp);
				}
			}
			return op;
		}
		
	}
	
}
