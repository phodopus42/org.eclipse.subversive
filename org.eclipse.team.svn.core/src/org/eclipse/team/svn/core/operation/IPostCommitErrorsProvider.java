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

package org.eclipse.team.svn.core.operation;

import org.eclipse.team.svn.core.connector.SVNCommitStatus;


/**
 * This interface allows to return all the post-commit hook's errors
 * 
 * @author Alexander Gurov
 */
public interface IPostCommitErrorsProvider {
	public SVNCommitStatus []getPostCommitErrors();
}
