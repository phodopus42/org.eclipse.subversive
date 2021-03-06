/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local.management;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;
import org.eclipse.team.svn.core.operation.IUnprotectedOperation;
import org.eclipse.team.svn.core.operation.SVNResourceRuleFactory;
import org.eclipse.team.svn.core.utility.FileUtility;

/**
 * Disconnect operation implementation
 * 
 * @author Alexander Gurov
 */
public class DisconnectOperation extends AbstractActionOperation {
	protected IProject []projects;
	protected boolean dropSVNFolders;

	public DisconnectOperation(IProject []projects, boolean dropSVNFolders) {
		super("Operation_Disconnect", SVNMessages.class); //$NON-NLS-1$
		this.projects = projects;
		this.dropSVNFolders = dropSVNFolders;
	}

	public ISchedulingRule getSchedulingRule() {
		HashSet<ISchedulingRule> rules = new HashSet<ISchedulingRule>();
		for (int i = 0; i < this.projects.length; i++) {
			rules.add(SVNResourceRuleFactory.INSTANCE.modifyRule(this.projects[i]));
		}
		return new MultiRule(rules.toArray(new ISchedulingRule[rules.size()]));
	}
	
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		for (int i = 0; i < this.projects.length; i++) {
			final IProject current = this.projects[i];
			
			this.protectStep(new IUnprotectedOperation() {
				public void run(IProgressMonitor monitor) throws Exception {
					if (RepositoryProvider.isShared(current)) { // it seems sometimes projects could be unmapped prior to running this code, for example by an outside activity (see bug #403385)
						RepositoryProvider.unmap(current);
					}
					if (DisconnectOperation.this.dropSVNFolders) {
						FileUtility.removeSVNMetaInformation(current, null);
					}
				}
			}, monitor, this.projects.length);
		}
	}
	
}
