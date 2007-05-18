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

package org.eclipse.team.svn.core.svnstorage;

import org.eclipse.team.svn.core.client.Revision;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;

/**
 * Basic implementation of IRepositoryRoot
 * 
 * @author Alexander Gurov
 */
public class SVNRepositoryRoot extends SVNRepositoryRootBase {
	private static final long serialVersionUID = 8314649604979930219L;
	
	public SVNRepositoryRoot(IRepositoryLocation location) {
		super(location, location.getRepositoryRootUrl(), Revision.HEAD);
	}

	public IRepositoryResource getParent() {
		return null;
	}
	
	public IRepositoryResource getRoot() {
		return this;
	}

	public int getKind() {
		return IRepositoryRoot.KIND_ROOT;
	}
	
}
