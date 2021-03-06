/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Thomas Champagne - Bug 217561 : additional date formats for label decorations
 *******************************************************************************/

package org.eclipse.team.svn.ui.history.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.connector.SVNLogEntry;
import org.eclipse.team.svn.core.connector.SVNRevision;
import org.eclipse.team.svn.core.utility.FileUtility;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.utility.DateFormatter;

/**
 * SVN log entry UI node
 * 
 * @author Alexander Gurov
 */
public class SVNLogNode extends AbstractLogNode {
	protected SVNLogEntry entry;
	
	public SVNLogNode(SVNLogEntry entry, ILogNode parent) {
		super(parent);
		this.entry = entry;
	}
	
	public Object getAdapter(Class adapter) {
		if (adapter.equals(SVNLogEntry.class)) {
			return this.entry;
		}
		return null;
	}

	public ILogNode[] getChildren() {
		SVNLogEntry []entries = this.entry.getChildren();
		ILogNode []children = new ILogNode[entries.length];
		for (int i = 0; i < entries.length; i++) {
			children[i] = new SVNLogNode(entries[i], this);
		}
		return children;
	}

	public ImageDescriptor getImageDescriptor() {
		return SVNTeamUIPlugin.instance().getImageDescriptor(this.parent instanceof SVNLogNode ? "icons/objects/repository-gray.gif" : "icons/objects/repository.gif"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public boolean requiresBoldFont(long currentRevision) {
		return currentRevision != SVNRevision.INVALID_REVISION_NUMBER && this.entry.revision == currentRevision;
	}
	
	public String getLabel(int columnIndex, int labelType, long currentRevision) {
		switch (columnIndex) {
			case ILogNode.COLUMN_REVISION: {
				String retVal = String.valueOf(this.entry.revision);
				if (currentRevision == this.entry.revision) {
					retVal = "*" + retVal; //$NON-NLS-1$
				}
				return retVal;
			}
			case ILogNode.COLUMN_DATE: {
				return this.entry.date == 0 ? SVNMessages.SVNInfo_NoDate : DateFormatter.formatDate(this.entry.date);
			}
			case ILogNode.COLUMN_CHANGES: {
				return String.valueOf(this.entry.changedPaths != null ? this.entry.changedPaths.length : 0);
			}
			case ILogNode.COLUMN_AUTHOR: {
				return this.entry.author == null || this.entry.author.length() == 0 ? SVNMessages.SVNInfo_NoAuthor : this.entry.author;
			}
			case ILogNode.COLUMN_COMMENT: {
				String retVal = this.entry.message;
				if (retVal == null || retVal.length() == 0) {
					return SVNMessages.SVNInfo_NoComment;
				}
				if (labelType == ILogNode.LABEL_TRIM) {
					return FileUtility.formatMultilineText(retVal);
				}
				else if (labelType == ILogNode.LABEL_FLAT) {
					return AbstractLogNode.flattenMultiLineText(retVal, " "); //$NON-NLS-1$
				}
				return retVal;
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	public Object getEntity() {
		return this.entry;
	}

	public int getType() {
		return ILogNode.TYPE_SVN;
	}

	public boolean hasChildren() {
		return this.entry.hasChildren();
	}
	
	public String getAuthor() {
		return this.entry.author;
	}

	public int getChangesCount() {
		return this.entry.changedPaths == null ? 0 : this.entry.changedPaths.length;
	}

	public String getComment() {
		return this.entry.message;
	}

	public long getRevision() {
		return this.entry.revision;
	}

	public long getTimeStamp() {
		return this.entry.date;
	}
	
	public int hashCode() {
		return (int)this.entry.revision;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof SVNLogNode) {
			return this.entry.revision == ((SVNLogNode)obj).entry.revision;
		}
		return false;
	}
	
}
