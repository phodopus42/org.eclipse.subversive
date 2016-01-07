/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.core.connector;

import java.io.File;


/**
 * The conflict description container
 * 
 * The JavaHL API's is the only way to interact between SVN and Java-based tools. At the same time JavaHL connector library
 * is not EPL compatible and we won't to pin plug-in with concrete connector implementation. So, the only way to do this is
 * providing our own connector interface which will be covered by concrete connector implementation.
 * 
 * @author Alexander Gurov
 */
public class SVNConflictDescriptor {
	/**
	 * Conflict kind: content or properties
	 */
	public static class Kind {
		/**
		 * Conflicting content
		 */
		public static final int CONTENT = 0;

		/**
		 * Conflicting properties
		 */
		public static final int PROPERTIES = 1;

		/**
		 * @since 1.7 Tree structure conflict
		 */
		public static final int TREE = 2;
	}

	/**
	 * The action in result of which conflict occurs
	 */
	public static class Action {
		/**
		 * Modification of content or properties
		 */
		public static final int MODIFY = 0;

		/**
		 * Adding entry
		 */
		public static final int ADD = 1;

		/**
		 * Deleting entry
		 */
		public static final int DELETE = 2;

		/**
		 * Replacing entry
		 */
		public static final int REPLACE = 3;
	}

	/**
	 * The reason why the conflict occurs
	 */
	public static class Reason {
		/**
		 * The entry is locally modified.
		 */
		public static final int MODIFIED = 0;

		/**
		 * Another entry is in the way.
		 */
		public static final int OBSTRUCTED = 1;

		/**
		 * The entry is locally deleted.
		 */
		public static final int DELETED = 2;

		/**
		 * The entry is missing (deleted from the file system).
		 */
		public static final int MISSING = 3;

		/**
		 * The unversioned entry at the path in the working copy.
		 */
		public static final int UNVERSIONED = 4;
		
	    /**
         * Object is already added or schedule-add.
         * @since New in 1.6.
         */
        public static final int ADDED = 5;
	}

	public static class Operation {
	    /**
	     * none
	     */
	    public static final int NONE = 0;

	    /**
	     * update
	     */
	    public static final int UPDATE = 1;

	    /**
	     * switch 
	     */	   
	    public static final int SWITCHED = 2;

	    /**
	     * merge 
	     */
	    public static final int MERGE = 3;
	}
	
	/**
	 * The conflicted entry path.
	 */
	public final String path;

	/**
	 * The conflict kind (see {@link Kind}).
	 */
	public final int conflictKind;

	/**
	 * The node kind (see {@link SVNEntry.Kind}).
	 */
	public final int nodeKind;

	/**
	 * The conflicting property name.
	 */
	public final String propertyName;

	/**
	 * True if entry is binary.
	 */
	public final boolean isBinary;

	/**
	 * The MIME-type of the entry.
	 */
	public final String mimeType;

	/**
	 * The action in result of which conflict occurs (see {@link Action}).
	 */
	public final int action;

	/**
	 * The reason why the conflict occurs (see {@link Reason}).
	 */
	public final int reason;

	/**
	 * The base revision content path.
	 */
	public final String basePath;

	/**
	 * The repository revision content path.
	 */
	public final String remotePath;

	/**
	 * The local version content path.
	 */
	public final String localPath;

	/**
	 * The auto-merged content path.
	 */
	public final String mergedPath;

	/**
     * @see Operation
     */
	public final int operation;
	
    public final SVNConflictVersion srcLeftVersion;
    
    public final SVNConflictVersion srcRightVersion;
	
	/**
	 * The {@link SVNConflictDescriptor} instance could be initialized only once because all fields are final
	 * 
	 * @param path
	 *            the entry path
	 * @param conflictKind
	 *            the conflict kind
	 * @param nodeKind
	 *            the entry node kind
	 * @param propertyName
	 *            the conflicting property name
	 * @param isBinary
	 *            is entry binary or not
	 * @param mimeType
	 *            the entry MIME-type
	 * @param action
	 *            the action which involves conflict
	 * @param reason
	 *            the conflict reason
	 * @param basePath
	 *            the base version content path
	 * @param remotePath
	 *            the repository version content path
	 * @param localPath
	 *            the local version content path
	 * @param mergedPath
	 *            the auto-merged content path
	 * @param srcLeftVersion
	 * @param srcRightVersion
	 */
	public SVNConflictDescriptor(String path, int conflictKind, int nodeKind, String propertyName, boolean isBinary, String mimeType, int action, int reason, int operation, 
			String basePath, String remotePath, String localPath, String mergedPath, SVNConflictVersion srcLeftVersion, SVNConflictVersion srcRightVersion) {
		this.path = path;
		this.conflictKind = conflictKind;
		this.nodeKind = nodeKind;
		this.propertyName = propertyName;
		this.isBinary = isBinary;
		this.mimeType = mimeType;
		this.action = action;
		this.reason = reason;
		this.operation = operation;
		if (this.path != null && basePath != null && !basePath.startsWith(this.path))
		{
			this.basePath = new File(this.path).getParent() + File.separator + basePath;
		}
		else
		{
			this.basePath = basePath;
		}
		if (this.path != null && remotePath != null && !remotePath.startsWith(this.path))
		{
			this.remotePath = new File(this.path).getParent() + File.separator + remotePath;
		}
		else
		{
			this.remotePath = remotePath;
		}
		if (this.path != null && localPath != null && !localPath.startsWith(this.path))
		{
			this.localPath = new File(this.path).getParent() + File.separator + localPath;
		}
		else
		{
			this.localPath = localPath;
		}
		if (this.path != null && mergedPath != null && !mergedPath.startsWith(this.path))
		{
			this.mergedPath = new File(this.path).getParent() + File.separator + mergedPath;
		}
		else
		{
			this.mergedPath = mergedPath;
		}
		this.srcLeftVersion = srcLeftVersion;
		this.srcRightVersion = srcRightVersion;
	}
	
	/*
	 * Constructor for creating tree conflict descriptor
	 */
	public SVNConflictDescriptor(String path, int action, int reason, int operation, SVNConflictVersion srcLeftVersion, SVNConflictVersion srcRightVersion) {		
		this(path, 0, 0, null, false, null, action, reason, operation, null, null, null, null, srcLeftVersion, srcRightVersion);
	}
	
}