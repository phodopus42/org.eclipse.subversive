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

package org.eclipse.team.svn.tests.core;

import org.eclipse.team.svn.core.operation.IActionOperation;
import org.eclipse.team.svn.core.operation.local.InfoOperation;

/**
 * InfoOperation test
 *
 * @author Sergiy Logvin
 */
public abstract class InfoOperationTest extends AbstractOperationTestCase {
	protected IActionOperation getOperation() {
	    return new InfoOperation(this.getFirstProject().getFile("maven.xml"));
	}
	
}
