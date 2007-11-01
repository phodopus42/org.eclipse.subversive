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

package org.eclipse.team.svn.core.client;

/**
 * Enumeration of operation types which could be scheduled for the working copy entries
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL client library
 * is not EPL compatible and we won't to pin plug-in with concrete client implementation. So, the only way to do this is
 * providing our own client interface which will be covered by concrete client implementation.
 * 
 * @author Alexander Gurov
 */
public class ScheduleKind {
	/**
	 * No operation scheduled
	 */
	public static final int NORMAL = 0;

	/**
	 * Will be added to repository on commit
	 */
	public static final int ADD = 1;

	/**
	 * Will be deleted from repository on commit
	 */
	public static final int DELETE = 2;

	/**
	 * Will be replaced in repository on commit
	 */
	public static final int REPLACE = 3;
}
