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

package org.eclipse.team.svn.ui.event;

/**
 * Resource state listener interface
 * 
 * @author Sergiy Logvin
 */
public interface IResourceSelectionChangeListener {
	
	public void resourcesSelectionChanged(ResourceSelectionChangedEvent event);
	
}
