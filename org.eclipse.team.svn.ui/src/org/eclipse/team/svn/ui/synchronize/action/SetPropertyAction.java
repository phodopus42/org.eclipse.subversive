/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexei Goncharov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.synchronize.action;

import java.util.Iterator;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * Set property action implementation for Synchronize view
 * 
 * @author Alexei Goncharov
 */
public class SetPropertyAction extends AbstractSynchronizeModelAction {
	
	protected SetPropertyActionHelper actionHelper;
	
	public SetPropertyAction(String text, ISynchronizePageConfiguration configuration) {
		super(text, configuration);
		this.actionHelper = new SetPropertyActionHelper(this, configuration);
	}

	protected boolean needsToSaveDirtyEditors() {
		return false;
	}
	
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		for (Iterator<?> it = selection.iterator(); it.hasNext(); ) {
			ISynchronizeModelElement element = (ISynchronizeModelElement)it.next();
			if (IStateFilter.SF_VERSIONED.accept(SVNRemoteStorage.instance().asLocalResource(element.getResource()))) {
				return true;
			}
		}
	    return false;
	}
	
	protected IActionOperation getOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return this.actionHelper.getOperation();
	}

}
