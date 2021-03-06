/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sergiy Logvin - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.history;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.history.data.AffectedPathsNode;
import org.eclipse.team.svn.ui.utility.OverlayedImageDescriptor;

/**
 * Affected paths label provider
 *
 * @author Sergiy Logvin
 */
public class AffectedPathsLabelProvider extends LabelProvider {
	protected static Image folderIcon;
	protected static Image overlayedFolderIcon;
	protected static Image rootIcon;
	protected static Image rootAdditionIcon;
	protected static Image overlayedRootIcon;
	protected static Image addedFolderIcon;
	protected static Image modifiedFolderIcon;
	protected static Image deletedFolderIcon;
	protected static Image replacedFolderIcon;
	
	protected long currentRevision;
	
	public AffectedPathsLabelProvider() {
		synchronized (AffectedPathsLabelProvider.class) {
			if (AffectedPathsLabelProvider.folderIcon == null) {
				SVNTeamUIPlugin instance = SVNTeamUIPlugin.instance();
				AffectedPathsLabelProvider.folderIcon = instance.getImageDescriptor("icons/views/history/affected_folder.gif").createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.overlayedFolderIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/empty.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.rootIcon = instance.getImageDescriptor("icons/objects/repository-root.gif").createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.rootAdditionIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.rootIcon, instance.getImageDescriptor("icons/overlays/addition.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.overlayedRootIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.rootIcon, instance.getImageDescriptor("icons/overlays/empty.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.addedFolderIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/addition.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.modifiedFolderIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/change.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.deletedFolderIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/deletion.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				AffectedPathsLabelProvider.replacedFolderIcon = (new OverlayedImageDescriptor(AffectedPathsLabelProvider.folderIcon, instance.getImageDescriptor("icons/overlays/replacement.gif"), new Point(22, 16), OverlayedImageDescriptor.RIGHT | OverlayedImageDescriptor.CENTER_V)).createImage(); //$NON-NLS-1$
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.folderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.overlayedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.rootIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.rootAdditionIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.overlayedRootIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.addedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.modifiedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.deletedFolderIcon);
				CompareUI.disposeOnShutdown(AffectedPathsLabelProvider.replacedFolderIcon);
			}
		}
	}
	
	public void setCurrentRevision(long currentRevision) {
		this.currentRevision = currentRevision;
	}
	
	public Image getImage(Object element) {
		if (((AffectedPathsNode)element).getParent() == null) {
			return this.currentRevision == 0 ? AffectedPathsLabelProvider.rootAdditionIcon : AffectedPathsLabelProvider.overlayedRootIcon;
		}
		if (((AffectedPathsNode)element).getStatus() != null) {
			switch (((AffectedPathsNode)element).getStatus()) {
				case ADDED: {
					return AffectedPathsLabelProvider.addedFolderIcon;
				}
				case MODIFIED: {
					return AffectedPathsLabelProvider.modifiedFolderIcon;
				}
				case DELETED: {
					return AffectedPathsLabelProvider.deletedFolderIcon;
				}
				case REPLACED: {
					return AffectedPathsLabelProvider.replacedFolderIcon;
				}
			}
		}
		return AffectedPathsLabelProvider.overlayedFolderIcon;
	}

}
