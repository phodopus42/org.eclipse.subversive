/*******************************************************************************
 * Copyright (c) 2005-2008 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Burilo - Initial API and implementation
 *******************************************************************************/
package org.eclipse.team.svn.revision.graph.operation;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.revision.graph.cache.RepositoryCacheInfo;
import org.eclipse.team.svn.revision.graph.cache.RepositoryCache;

/**
 * Fetch revisions after last processed revision
 * 
 * @author Igor Burilo
 */
public class FetchNewRevisionsOperation extends BaseFetchOperation {
	
	public FetchNewRevisionsOperation(IRepositoryResource resource, CheckRepositoryConnectionOperation checkConnectionOp, RepositoryCache repositoryCache) {
		super("Operation_FetchNewRevisions", resource, checkConnectionOp, repositoryCache); //$NON-NLS-1$
	}

	@Override
	protected void prepareData(IProgressMonitor monitor) throws Exception {
		RepositoryCacheInfo cacheInfo = this.repositoryCache.getCacheInfo();
		
		this.startRevision = cacheInfo.getLastProcessedRevision() + 1;
		this.endRevision = this.checkConnectionOp.getLastRepositoryRevision();
		
		this.canRun = this.checkConnectionOp.getLastRepositoryRevision() > cacheInfo.getLastProcessedRevision();
		if (this.canRun) {
			cacheInfo.setSkippedRevisions(this.startRevision, this.endRevision);
			cacheInfo.setLastProcessedRevision(this.endRevision);
			cacheInfo.save();	
			
			this.repositoryCache.expandRevisionsCount(this.endRevision);
		}
	}
		
}