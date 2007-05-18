/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov - Initial API and implementation
 *    Gabor Liptak - Speedup Pattern's usage
 *******************************************************************************/

package org.eclipse.team.svn.core.utility;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.Team;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.client.ISVNClientWrapper;
import org.eclipse.team.svn.core.client.Info2;
import org.eclipse.team.svn.core.client.NodeKind;
import org.eclipse.team.svn.core.client.Notify2;
import org.eclipse.team.svn.core.client.Revision;
import org.eclipse.team.svn.core.client.Status;
import org.eclipse.team.svn.core.extension.CoreExtensionsManager;
import org.eclipse.team.svn.core.extension.options.IIgnoreRecommendations;
import org.eclipse.team.svn.core.operation.SVNNullProgressMonitor;
import org.eclipse.team.svn.core.operation.file.SVNFileStorage;
import org.eclipse.team.svn.core.resource.IRemoteStorage;
import org.eclipse.team.svn.core.resource.IRepositoryContainer;
import org.eclipse.team.svn.core.resource.IRepositoryFile;
import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.resource.IRepositoryResource;
import org.eclipse.team.svn.core.resource.IRepositoryRoot;
import org.eclipse.team.svn.core.resource.ProxySettings;
import org.eclipse.team.svn.core.resource.SSHSettings;
import org.eclipse.team.svn.core.resource.SSLSettings;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;

/**
 * SVN Utility functions
 * 
 * @author Alexander Gurov
 */
public final class SVNUtility {
	private static String svnFolderName = null;
	
	public static String getStatusText(String status) {
		if (status == null) {
			status = "NotExists";
		}
		return SVNTeamPlugin.instance().getResource("Status." + status);
	}
	
	public static String getOldRoot(String oldUrl, IRepositoryResource []rootChildren) {
		for (int i = 0; i < rootChildren.length; i++) {
			String childName = rootChildren[i].getName();
			int idx = oldUrl.indexOf(childName);
			if (idx > 0 && oldUrl.charAt(idx - 1) == '/' && (oldUrl.endsWith(childName) || oldUrl.charAt(idx + childName.length()) == '/')) {
				return oldUrl.substring(0, idx - 1);
			}
		}
		return null;
	}
	
	public static IRepositoryRoot getTrunkLocation(IRepositoryResource resource) {
		return SVNUtility.getRootLocation(resource, resource.getRepositoryLocation().getTrunkLocation());
	}
	
	public static IRepositoryRoot getBranchesLocation(IRepositoryResource resource) {
		return SVNUtility.getRootLocation(resource, resource.getRepositoryLocation().getBranchesLocation());
	}
	
	public static IRepositoryRoot getTagsLocation(IRepositoryResource resource) {
		return SVNUtility.getRootLocation(resource, resource.getRepositoryLocation().getTagsLocation());
	}
	
	public static IRepositoryContainer getProposedTrunk(IRepositoryLocation location) {
		return location.asRepositoryContainer(SVNUtility.getProposedTrunkLocation(location), false);
	}
	
	public static IRepositoryContainer getProposedBranches(IRepositoryLocation location) {
		return location.asRepositoryContainer(SVNUtility.getProposedBranchesLocation(location), false);
	}
	
	public static IRepositoryContainer getProposedTags(IRepositoryLocation location) {
		return location.asRepositoryContainer(SVNUtility.getProposedTagsLocation(location), false);
	}
	
	public static String getProposedTrunkLocation(IRepositoryLocation location) {
		String baseUrl = location.getUrl();
		return location.isStructureEnabled() ? (baseUrl + "/" + location.getTrunkLocation()) : baseUrl;
	}
	
	public static String getProposedBranchesLocation(IRepositoryLocation location) {
		String baseUrl = location.getUrl();
		return location.isStructureEnabled() ? (baseUrl + "/" + location.getBranchesLocation()) : baseUrl;
	}
	
	public static String getProposedTagsLocation(IRepositoryLocation location) {
		String baseUrl = location.getUrl();
		return location.isStructureEnabled() ? (baseUrl + "/" + location.getTagsLocation()) : baseUrl;
	}
	
	public static IRepositoryRoot []findRoots(String resourceUrl, boolean longestOnly) {
		IPath url = new Path(resourceUrl);
		IRemoteStorage storage = SVNRemoteStorage.instance();
		IRepositoryLocation []locations = storage.getRepositoryLocations();
		ArrayList roots = new ArrayList();
		for (int i = 0; i < locations.length; i++) {
			if (new Path(locations[i].getUrl()).isPrefixOf(url) && // performance optimization: repository root URL detection [if is not cached] requires interaction with a remote host
				new Path(locations[i].getRepositoryRootUrl()).isPrefixOf(url)) {
				SVNUtility.addRepositoryRoot(roots, (IRepositoryRoot)locations[i].asRepositoryContainer(resourceUrl, false).getRoot(), longestOnly);
			}
		}
		IRepositoryRoot []repositoryRoots = (IRepositoryRoot [])roots.toArray(new IRepositoryRoot[roots.size()]);
		if (!longestOnly) {
			FileUtility.sort(repositoryRoots, new Comparator() {
				public int compare(Object o1, Object o2) {
					IRepositoryRoot first = (IRepositoryRoot)o1;
					IRepositoryRoot second = (IRepositoryRoot)o2;
					return second.getUrl().compareTo(first.getUrl());
				}
			});
		}
		return repositoryRoots;
	}
	
	private static void addRepositoryRoot(List container, IRepositoryRoot root, boolean longestOnly) {
		if (longestOnly && container.size() > 0) {
			int cnt = new Path(root.getUrl()).segmentCount();
			int cnt2 = new Path(((IRepositoryRoot)container.get(0)).getUrl()).segmentCount();
			if (cnt > cnt2) {
				container.clear();
				container.add(root);
			}
			else if (cnt == cnt2) {
				container.add(root);
			}
		}
		else {
			container.add(root);
		}
	}
	
	public static synchronized String getSVNFolderName() {
		if (SVNUtility.svnFolderName == null) {
			String name = FileUtility.getEnvironmentVariables().get("SVN_ASP_DOT_NET_HACK") != null ? "_svn" : ".svn";
			SVNUtility.svnFolderName = System.getProperty("javasvn.admindir", name);
		}
		return SVNUtility.svnFolderName;
	}
	
	public static String getResourceParent(IRepositoryResource resource) {
		String parent = "";
		String url = resource.getUrl();
		String rootUrl = resource.getRoot().getUrl();
		if (url.equals(rootUrl)) {
			return "";
		}
		parent = url.substring(rootUrl.length(), url.length() - resource.getName().length() - 1);
		return parent;
	}
	
	public static IRepositoryResource copyOf(IRepositoryResource resource) {
		String url = resource.getUrl();
		return resource instanceof IRepositoryFile ? (IRepositoryResource)resource.asRepositoryFile(url, false) : resource.asRepositoryContainer(url, false);
	}
	
	public static IRepositoryResource []makeResourceSet(IRepositoryResource upPoint, String relativeReference, boolean isFile) {
		String url = SVNUtility.normalizeURL(upPoint.getUrl() + "/" + relativeReference);
		IRepositoryLocation location = upPoint.getRepositoryLocation();
		IRepositoryResource downPoint = isFile ? (IRepositoryResource)location.asRepositoryFile(url, false) : location.asRepositoryContainer(url, false);
		downPoint.setPegRevision(upPoint.getPegRevision());
		downPoint.setSelectedRevision(upPoint.getSelectedRevision());
		return SVNUtility.makeResourceSet(upPoint, downPoint);
	}
	
	public static IRepositoryResource []makeResourceSet(IRepositoryResource upPoint, IRepositoryResource downPoint) {
		ArrayList resourceSet = new ArrayList();
		while (downPoint != null && !downPoint.equals(upPoint)) {
			resourceSet.add(0, downPoint);
			downPoint = downPoint.getParent();
		}
		return (IRepositoryResource [])resourceSet.toArray(new IRepositoryResource[resourceSet.size()]);
	}
	
	public static URL getSVNUrl(String url) throws MalformedURLException {
		return new URL(null, url, new URLStreamHandler() {
            protected URLConnection openConnection(URL u) throws IOException {
                return null;
            }
            
            protected void parseURL(URL u, String spec, int start, int limit) {
            	String protocol = u.getProtocol();
                if (!protocol.equals("file") &&
            		!protocol.equals("svn") &&
                    !protocol.equals("http") &&
                    !protocol.equals("https") &&
                    !protocol.equals("svn+ssh")) {
            		String errMessage = SVNTeamPlugin.instance().getResource("Error.UnknownProtocol");
                    throw new RuntimeException(MessageFormat.format(errMessage, new String[] {protocol}));
                }
                super.parseURL(u, spec, start, limit);
            }
        });
	}
	
	public static String base64Encode(String data) {
		if (data == null) {
			return null;
		}
		return new String(Base64.encode(data.getBytes()));
	}
	
	public static String base64Decode(String encoded) {
		if (encoded == null) {
			return null;
		}
		return new String(Base64.decode(encoded.getBytes()));
	}

	public synchronized static void addSVNNotifyListener(ISVNClientWrapper proxy, Notify2 listener) {
		Notify2Composite composite = (Notify2Composite)proxy.getNotification2();
		if (composite == null) {
			proxy.notification2(composite = new Notify2Composite());
		}
		composite.add(listener);
	}

	public synchronized static void removeSVNNotifyListener(ISVNClientWrapper proxy, Notify2 listener) {
		Notify2Composite composite = (Notify2Composite)proxy.getNotification2();
		if (composite != null) {
			composite.remove(listener);
		}
	}
	
	public static void reorder(Status []statuses, final boolean parent2Child) {
		FileUtility.sort(statuses, new Comparator() {
			public int compare(Object o1, Object o2) {
				String s1 = ((Status)o1).path;
				String s2 = ((Status)o2).path;
				return parent2Child ? s1.compareTo(s2) : s2.compareTo(s1);
			}
			
			public boolean equals(Object obj) {
				return false;
			}
		});
	}
	
	public static String encodeURL(String url) {
		try {
			url = SVNUtility.normalizeURL(url);
			if (url.startsWith("file:")) {
				return url;
			}
			int idx = url.indexOf("://");
			idx = url.indexOf("/", idx + 3);
			if (idx == -1) {
				return url;
			}
			String retVal = url.substring(0, idx);
			StringTokenizer tok = new StringTokenizer(url.substring(idx), "/ ", true);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				if (token.equals("/")) {
					retVal += token;
				}
				else if (token.equals(" ")) {
					retVal += "%20";
				}
				else {
					retVal += URLEncoder.encode(token, "UTF-8");
				}
			}
			return retVal;
		}
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static String decodeURL(String url) {
		try {
			url = SVNUtility.normalizeURL(url);
			if (url.startsWith("file:")) {
				return url;
			}
			int idx = url.indexOf("://");
			idx = url.indexOf("/", idx + 3);
			if (idx == -1) {
				return url;
			}
			String retVal = url.substring(0, idx);
			StringTokenizer tok = new StringTokenizer(url.substring(idx), "/+", true);
			// user name should be never encoded
			idx = retVal.indexOf('@');
			if (idx != -1) {
				String protocol = retVal.substring(0, retVal.indexOf("://") + 3);
				String serverPart = retVal.substring(idx);
				retVal = protocol + URLDecoder.decode(retVal.substring(protocol.length(), idx), "UTF-8") + serverPart;
			}
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				if (token.equals("/") || token.equals("+")) {
					retVal += token;
				}
				else {
					retVal += URLDecoder.decode(token, "UTF-8");
				}
			}
			return retVal;
		}
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static String normalizeURL(String url) {
	    StringTokenizer tokenizer = new StringTokenizer(PatternProvider.replaceAll(url, "([\\\\])+", "/"), "/", false);
	    String retVal = "";
	    while (tokenizer.hasMoreTokens()) {
	        String token = tokenizer.nextToken();
	        retVal += retVal.length() == 0 ? token : ("/" + token);
	    }
	    int idx = retVal.indexOf(':') + 1;
	    return idx == 0 ? retVal : retVal.substring(0, idx) + (retVal.startsWith("file") ? "//" : "/") + retVal.substring(idx);
	}
	
	public static Exception validateRepositoryLocation(IRepositoryLocation location) {
		ISVNClientWrapper proxy = location.acquireSVNProxy();
		try {
			proxy.list(SVNUtility.encodeURL(location.getUrl()), Revision.HEAD, Revision.HEAD, false, false, new SVNNullProgressMonitor());
		} 
		catch (Exception e) {
			return e;
		}
		finally {
			location.releaseSVNProxy(proxy);
			location.dispose();
		}
		return null;
	}
	
	public static void configureProxy(ISVNClientWrapper proxy, IRepositoryLocation location) {
		proxy.username(location.getUsername());
		proxy.password(location.getPassword());
		
		ProxySettings proxySettings = location.getProxySettings();
	    if (proxySettings.isEnabled()) {
	    	proxy.setProxy(proxySettings.getHost(), proxySettings.getPort(), proxySettings.isAuthenticationEnabled() ? proxySettings.getUsername() : "", proxySettings.isAuthenticationEnabled() ? proxySettings.getPassword() : "");
	    }
	    else {
	    	proxy.setProxy(null, -1, null, null);
	    }
	    
	    SSLSettings sslSettings = location.getSSLSettings();
	    if (sslSettings.isAuthenticationEnabled()) {
		    proxy.setClientSSLCertificate(sslSettings.getCertificatePath(), sslSettings.getPassPhrase());
	    }
	    else {
	    	proxy.setClientSSLCertificate(null, null);
	    }
	    
	    SSHSettings sshSettings = location.getSSHSettings();
	    if (!sshSettings.isUseKeyFile()) {
	    	proxy.setSSHCredentials(location.getUsername(), location.getPassword(), sshSettings.getPort());
	    }
	    else if (sshSettings.getPrivateKeyPath().length() > 0) {
	    	proxy.setSSHCredentials(location.getUsername(), sshSettings.getPrivateKeyPath(), sshSettings.getPassPhrase(), sshSettings.getPort());
	    }
	    else {
	    	proxy.setSSHCredentials(null, null, null, -1);
	    }
	}
	
	public static Status getSVNInfoForNotConnected(IResource root) {
		IPath location = root.getLocation();
		if (location == null) {
			return null;
		}
		IPath checkedPath = root.getType() == IResource.FILE ? location.removeLastSegments(1) : location;
		if (!checkedPath.append(SVNUtility.getSVNFolderName()).toFile().exists()) {
			return null;
		}
		ISVNClientWrapper proxy = CoreExtensionsManager.instance().getSVNClientWrapperFactory().newInstance();
		try {
			Status []st = proxy.status(location.toString(), false, false, true, false, new SVNNullProgressMonitor());
			if (st != null && st.length > 0) {
				SVNUtility.reorder(st, true);
				return st[0].url == null ? null : st[0];
			}
			else {
				return null;
			}
		}
		catch (Exception ex) {
			return null;
		}
		finally {
			proxy.dispose();
		}
	}
	
    public static boolean isIgnored(IResource resource) {
		// Ignore WorkspaceRoot, derived and team-private resources and resources from TeamHints 
        if (resource instanceof IWorkspaceRoot || resource.isDerived() || 
        	FileUtility.isSVNInternals(resource) || Team.isIgnoredHint(resource) || SVNUtility.isMergeParts(resource)) {
        	return true;
        }
        try {
        	IIgnoreRecommendations []ignores = CoreExtensionsManager.instance().getIgnoreRecommendations();
        	for (int i = 0; i < ignores.length; i++) {
        		if (ignores[i].isAcceptableNature(resource) && ignores[i].isIgnoreRecommended(resource)) {
        			return true;
        		}
        	}
        }
        catch (Exception ex) {
        	// cannot be correctly processed in the caller context
        }
        return false;
    }

	public static Map splitWorkingCopies(IResource []resources) {
		Map wc2Resources = new HashMap();
		
		for (int i = 0; i < resources.length; i++) {
			IProject wcRoot = resources[i].getProject();

			List wcResources = (List)wc2Resources.get(wcRoot);
			if (wcResources == null) {
				wc2Resources.put(wcRoot, wcResources = new ArrayList());
			}
			wcResources.add(resources[i]);
		}

		return wc2Resources;
	}
	
	public static Map splitWorkingCopies(File []files) {
		Map wc2Resources = new HashMap();
		
		ISVNClientWrapper proxy = CoreExtensionsManager.instance().getSVNClientWrapperFactory().newInstance();
		try {
			Map file2info = new HashMap();
			for (int i = 0; i < files.length; i++) {
				file2info.put(files[i], SVNUtility.getSVNInfo(files[i], proxy));
			}
			
			ArrayList restOfFiles = new ArrayList(Arrays.asList(files));
			while (restOfFiles.size() > 0) {
				File current = (File)restOfFiles.get(0);
				Info2 info = (Info2)file2info.get(current);
				Object []wcRoot = SVNUtility.getWCRoot(proxy, current, info);
				
				List wcResources = (List)wc2Resources.get(wcRoot[0]);
				if (wcResources == null) {
					wc2Resources.put(wcRoot[0], wcResources = new ArrayList());
				}
				
				Path rootPath = new Path(((File)wcRoot[0]).getAbsolutePath());
				Path rootInfoPath = new Path(((Info2)wcRoot[1]).url);
				for (Iterator it = restOfFiles.iterator(); it.hasNext(); ) {
					File checked = (File)it.next();
					if (rootPath.isPrefixOf(new Path(checked.getAbsolutePath()))) {
						Info2 checkedInfo = (Info2)file2info.get(checked);
						if (rootInfoPath.isPrefixOf(new Path(checkedInfo.url))) {
							wcResources.add(checked);
							it.remove();
						}
					}
				}
			}
		}
		finally {
			proxy.dispose();
		}

		return wc2Resources;
	}
	
	private static Object []getWCRoot(ISVNClientWrapper proxy, File node, Info2 info) {
		File oldRoot = node;
		Info2 oldInfo = info;
		
		node = node.getParentFile();
		while (node != null) {
			Info2 rootInfo = SVNUtility.getSVNInfo(node, proxy);
			if (rootInfo != null) {
				if (oldInfo == null) {
					oldInfo = rootInfo;
				}
				else if (!new Path(rootInfo.url).isPrefixOf(new Path(oldInfo.url))) {
					return new Object[] {oldRoot, oldInfo};
				}
				oldRoot = node;
				node = node.getParentFile();
			}
			else if (oldInfo != null) {
				return new Object[] {oldRoot, oldInfo};
			}
		}
		
		if (oldInfo == null) {
			String errMessage = SVNTeamPlugin.instance().getResource("Error.NonSVNPath");
			throw new RuntimeException(MessageFormat.format(errMessage, new String[] {node.getAbsolutePath()}));
		}
		return new Object[] {oldRoot, oldInfo};
	}
	
	public static Info2 getSVNInfo(File root) {
		ISVNClientWrapper proxy = CoreExtensionsManager.instance().getSVNClientWrapperFactory().newInstance();
		try {
			return SVNUtility.getSVNInfo(root, proxy);
		}
		finally {
			proxy.dispose();
		}
	}
	
	public static Info2 getSVNInfo(File root, ISVNClientWrapper proxy) {
		if (root.exists()) {
			File svnMeta = root.isDirectory() ? root : root.getParentFile();
			svnMeta = new File(svnMeta.getAbsolutePath() + "/" + SVNUtility.getSVNFolderName());
			if (svnMeta.exists()) {
				try {
					Info2 []st = proxy.info2(root.getAbsolutePath(), Revision.BASE, null, false, new SVNNullProgressMonitor());
					return st != null && st.length != 0 ? st[0] : null;
				}
				catch (Exception ex) {
					return null;
				}
			}
		}
		return null;
	}
	
	public static String []asURLArray(IRepositoryResource []resources, boolean encode) {
	    String []retVal = new String[resources.length];
		for (int i = 0; i < resources.length; i++) {
			retVal[i] = encode ? SVNUtility.encodeURL(resources[i].getUrl()) : resources[i].getUrl();
		}
		return retVal;
	}
	
	public static Map splitRepositoryLocations(IRepositoryResource []resources) throws Exception {
		Map repository2Resources = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			IRepositoryLocation location = resources[i].getRepositoryLocation();

			List tResources = (List)repository2Resources.get(location);
			if (tResources == null) {
				repository2Resources.put(location, tResources = new ArrayList());
			}
			tResources.add(resources[i]);
		}
		return SVNUtility.combineLocationsByUUID(repository2Resources);
	}
	
	public static Map splitRepositoryLocations(IResource []resources) throws Exception{
	   	Map repository2Resources = new HashMap();	   	
		SVNRemoteStorage storage = SVNRemoteStorage.instance();
		for (int i = 0; i < resources.length; i++) {
		    IRepositoryLocation location = storage.getRepositoryLocation(resources[i]);

			List tResources = (List)repository2Resources.get(location);
			if (tResources == null) {
				repository2Resources.put(location, tResources = new ArrayList());
			}
			tResources.add(resources[i]);
		}
		return SVNUtility.combineLocationsByUUID(repository2Resources);
	}
	
	public static Map splitRepositoryLocations(File []files) throws Exception{
	   	Map repository2Resources = new HashMap();	   	
		for (int i = 0; i < files.length; i++) {
		    IRepositoryResource resource = SVNFileStorage.instance().asRepositoryResource(files[i], false);
		    IRepositoryLocation location = resource.getRepositoryLocation();

			List tResources = (List)repository2Resources.get(location);
			if (tResources == null) {
				repository2Resources.put(location, tResources = new ArrayList());
			}
			tResources.add(files[i]);
		}
		return SVNUtility.combineLocationsByUUID(repository2Resources);
	}
	
	public static int getNodeKind(String path, int kind, boolean ignoreNone) {
		File f = new File(path);
		if (f.exists()) {
			return f.isDirectory() ? NodeKind.dir : NodeKind.file;
		}
		else if (kind == NodeKind.dir) {
			return NodeKind.dir;
		}
		else if (kind == NodeKind.file) {
			return NodeKind.file;
		}
		// ignore files absent in the WC base and WC working. But what is the reason why it is reported ?
		if (ignoreNone) {
			return NodeKind.none;
		}
		String errMessage = SVNTeamPlugin.instance().getResource("Error.UnrecognizedNodeKind");
		throw new RuntimeException(MessageFormat.format(errMessage, new String[] {String.valueOf(kind), path}));
	}
	
	public static IRepositoryResource []shrinkChildNodes(IRepositoryResource []resources) {
		Set roots = new HashSet(Arrays.asList(resources));
		for (int i = 0; i < resources.length; i++) {
			if (SVNUtility.hasRoots(roots, resources[i])) {
				roots.remove(resources[i]);
			}
		}
		return (IRepositoryResource [])roots.toArray(new IRepositoryResource[roots.size()]);
	}
	
	public static IRepositoryResource []getCommonParents(IRepositoryResource []resources) {
		Map byRepositoryRoots = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			IRepositoryResource root = resources[i].getRoot();
			List tmp = (List)byRepositoryRoots.get(root);
			if (tmp == null) {
				byRepositoryRoots.put(root, tmp = new ArrayList());
			}
			tmp.add(resources[i]);
		}
		IRepositoryResource []retVal = new IRepositoryResource[byRepositoryRoots.size()];
		int i = 0;
		for (Iterator it = byRepositoryRoots.values().iterator(); it.hasNext(); i++) {
			List tmp = (List)it.next();
			retVal[i] = SVNUtility.getCommonParent((IRepositoryResource [])tmp.toArray(new IRepositoryResource[tmp.size()]));
		}
		return retVal;
	}
	
	public static Info2 getLocationInfo(IRepositoryLocation location) throws Exception {
		ISVNClientWrapper proxy = location.acquireSVNProxy();
		Info2 []infos = null;
		try {
		    infos = proxy.info2(SVNUtility.encodeURL(location.getUrl()), Revision.HEAD, Revision.HEAD, false, new SVNNullProgressMonitor());
		}
		finally {
		    location.releaseSVNProxy(proxy);
		}
		return infos != null && infos.length > 0 ? infos[0] : null;
	}
	
	public static String getAscendant(IRepositoryResource resource) {
		String pathUpToRoot = SVNUtility.getPathUpToRoot(resource);
		int idx = pathUpToRoot.indexOf('/');
		return idx == -1 ? pathUpToRoot : pathUpToRoot.substring(0, idx);
	}
	
	public static String getDescendant(IRepositoryResource resource) {
		String pathUpToRoot = SVNUtility.getPathUpToRoot(resource);
		int idx = pathUpToRoot.lastIndexOf('/');
		return idx == -1 ? pathUpToRoot : pathUpToRoot.substring(idx + 1);
	}
	
	public static String getPathUpToRoot(IRepositoryResource resource) {
		IRepositoryResource root = resource.getRoot();
		return root == resource ? resource.getName() : resource.getUrl().substring(root.getUrl().length() + 1);
	}

	private static boolean isMergeParts(IResource resource) {
		String ext = resource.getFileExtension();
		return ext != null && ext.matches("r(\\d)+");
	}
	    
	private static Map combineLocationsByUUID(Map repository2Resources) throws Exception{
	    Map locationUtility2Resources = new HashMap();
	    for (Iterator it = repository2Resources.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry entry = (Map.Entry)it.next();
			IRepositoryLocation location = (IRepositoryLocation)entry.getKey();
			List tResources = (List)entry.getValue();
	        RepositoryLocationUtility locationUtility = new RepositoryLocationUtility(location);
		    List tResources2 = (List)locationUtility2Resources.get(locationUtility);
			if (tResources2 == null) {
			    locationUtility2Resources.put(locationUtility, tResources2 = new ArrayList());
			}
			tResources2.addAll(tResources);
		}
	    repository2Resources.clear();		
		for (Iterator it = locationUtility2Resources.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry entry = (Map.Entry)it.next();
			RepositoryLocationUtility locationUtility = (RepositoryLocationUtility)entry.getKey();
			repository2Resources.put(locationUtility.location, (List)entry.getValue());
		}	
	    return repository2Resources;
	}
	
	private static boolean hasRoots(Set roots, IRepositoryResource node) {
		while ((node = node.getParent()) != null) {
			if (roots.contains(node)) {
				return true;
			}
		}
		return false;
	}
	
	private static IRepositoryResource getCommonParent(IRepositoryResource []resources) {
		if (resources == null || resources.length == 0) {
			return null;
		}
		IRepositoryResource base = resources[0].getParent();
		while (base != null) {	// can be null for resources from different repositories
			int startsCnt = 0;
			String baseUrl = base.getUrl();
			for (int i = 0; i < resources.length; i++) {
				if (resources[i].getUrl().startsWith(baseUrl)) {
					startsCnt++;
				}
			}
			if (startsCnt == resources.length) {
				break;
			}
			else {
				base = base.getParent();
			}
		}
		return base;
	}
	
	private static IRepositoryRoot getRootLocation(IRepositoryResource resource, String rootName) {
		IRepositoryLocation location = resource.getRepositoryLocation();
		IRepositoryRoot root = (IRepositoryRoot)resource.getRoot();
		if (!location.isStructureEnabled()) {
			return root;
		}
		
		int rootKind = root.getKind();
		if (rootKind == IRepositoryRoot.KIND_ROOT) {
			return (IRepositoryRoot)root.asRepositoryContainer(rootName, false);
		}
		else if (rootKind == IRepositoryRoot.KIND_LOCATION_ROOT) {
			IRepositoryResource parent = root.getParent();
			if (parent == null) {
				return (IRepositoryRoot)root.asRepositoryContainer(rootName, false);
			}
			IRepositoryRoot tmp = (IRepositoryRoot)parent.getRoot();
			rootKind = tmp.getKind();
			if (rootKind == IRepositoryRoot.KIND_ROOT) {
				return (IRepositoryRoot)root.asRepositoryContainer(rootName, false);
			}
			root = tmp;
		}
		IRepositoryResource rootParent = root.getParent();
		return (IRepositoryRoot)rootParent.asRepositoryContainer(rootName, false);
	}
	
	private SVNUtility() {
		
	}

}
