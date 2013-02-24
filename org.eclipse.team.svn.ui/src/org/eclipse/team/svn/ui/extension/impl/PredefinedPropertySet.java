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

package org.eclipse.team.svn.ui.extension.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.SVNUIMessages;
import org.eclipse.team.svn.ui.extension.factory.IPredefinedPropertySet;
import org.eclipse.team.svn.ui.extension.factory.PredefinedProperty;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;
import org.eclipse.team.svn.ui.preferences.SVNTeamPropsPreferencePage;

/**
 * IPropertyProvider implementation
 *
 * @author Sergiy Logvin
 */
public class PredefinedPropertySet implements IPredefinedPropertySet {
	
	protected static Map<String, PredefinedProperty> properties = null;
	
	public List<PredefinedProperty> getPredefinedProperties() {
		PredefinedPropertySet.init();
		return new ArrayList<PredefinedProperty>(PredefinedPropertySet.properties.values());
	}
	
	public PredefinedProperty getPredefinedProperty(String name) {
		return PredefinedPropertySet.properties.get(name);
	}
	
	public Map<String, String> getPredefinedPropertiesRegexps() {
		HashMap<String, String> regexpmap = new HashMap<String, String>();
		for (PredefinedProperty property : this.getPredefinedProperties()) {
			regexpmap.put(property.name, property.validationRegexp);
		}
		return regexpmap;
	}
	
	protected static void init() {
		if (PredefinedPropertySet.properties != null) {
			return;
		}
		PredefinedPropertySet.properties = new LinkedHashMap<String, PredefinedProperty>();
		
		PredefinedPropertySet.registerProperty(new PredefinedProperty(SVNUIMessages.AbstractPropertyEditPanel_svn_description, PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:eol-style", SVNUIMessages.Property_SVN_EOL, "", "((native)|(LF)|(CR)|(CRLF))", PredefinedProperty.TYPE_FILE));	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:executable", SVNUIMessages.Property_SVN_Executable, "", null, PredefinedProperty.TYPE_FILE)); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:externals", SVNUIMessages.Property_SVN_Externals, "", null, PredefinedProperty.TYPE_FOLDER)); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:ignore", SVNUIMessages.Property_SVN_Ignore, "", "([^\\\\/\\:])+", PredefinedProperty.TYPE_FOLDER)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:keywords", SVNUIMessages.Property_SVN_Keywords, "", "((Date)|(Revision)|(Author)|(HeadURL)|(Id)|(LastChangedDate)|(Rev)|(LastChangedRevision)|(LastChangedBy)|(URL)|(\\s))+", PredefinedProperty.TYPE_FILE)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:mime-type", SVNUIMessages.Property_SVN_Mimetype, "", null, PredefinedProperty.TYPE_FILE)); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:mergeinfo", SVNUIMessages.Property_SVN_Mergeinfo, "")); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:needs-lock", SVNUIMessages.Property_SVN_NeedsLock, "", null, PredefinedProperty.TYPE_FILE)); //$NON-NLS-1$ //$NON-NLS-2$
		
		PredefinedPropertySet.registerProperty(new PredefinedProperty(SVNUIMessages.PropertyEditPanel_bugtraq_description, PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:url", SVNUIMessages.Property_Bugtraq_URL, "%BUGID%", "((http:\\/\\/)|(https:\\/\\/))(\\S+)?((\\%BUGID\\%))(\\S+)?")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:logregex", SVNUIMessages.Property_Bugtraq_LogRegex, "")); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:label", SVNUIMessages.Property_Bugtraq_Label, "")); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:message", SVNUIMessages.Property_Bugtraq_Message, "%BUGID%")); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:number", SVNUIMessages.Property_Bugtraq_Number, "", "((true)|(false))")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:warnifnoissue", SVNUIMessages.Property_Bugtraq_WarnIfNoIssue, "", "((true)|(false))")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("bugtraq:append", SVNUIMessages.Property_Bugtraq_Append, "", "((true)|(false))")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		PredefinedPropertySet.registerProperty(new PredefinedProperty(SVNUIMessages.PropertyEditPanel_tsvn_description, PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		PredefinedPropertySet.registerProperty(new PredefinedProperty("tsvn:logtemplate", SVNUIMessages.Property_TSVN_LogTemplate, "")); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("tsvn:logwidthmarker", SVNUIMessages.Property_TSVN_LogWidthMarker, "", "(\\d+)")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("tsvn:logminsize", SVNUIMessages.Property_TSVN_LogMinSize, "", "(\\d+)")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("tsvn:lockmsgminsize", SVNUIMessages.Property_TSVN_LockMsgMinSize, "", "(\\d+)")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("tsvn:logfilelistenglish", SVNUIMessages.Property_TSVN_LogFileListEnglish, "", "((true)|(false))")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("tsvn:projectlanguage", SVNUIMessages.Property_TSVN_ProjectLanguage, "")); //$NON-NLS-1$ //$NON-NLS-2$
		
		PredefinedPropertySet.registerProperty(new PredefinedProperty(SVNUIMessages.AbstractPropertyEditPanel_revprop_description, PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_REVISION | PredefinedProperty.TYPE_COMMON));
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:log", SVNUIMessages.Property_SVN_Log, "", null, PredefinedProperty.TYPE_REVISION)); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:author", SVNUIMessages.Property_SVN_Author, "", null, PredefinedProperty.TYPE_REVISION)); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:date", SVNUIMessages.Property_SVN_Date, "", null, PredefinedProperty.TYPE_REVISION)); //$NON-NLS-1$ //$NON-NLS-2$
		PredefinedPropertySet.registerProperty(new PredefinedProperty("svn:autoversioned", SVNUIMessages.Property_SVN_Autoversioned, "", null, PredefinedProperty.TYPE_REVISION)); //$NON-NLS-1$ //$NON-NLS-2$
		
		PredefinedPropertySet.registerProperty(new PredefinedProperty(SVNUIMessages.AbstractPropertyEditPanel_custom_description, PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON));
		PredefinedProperty []customProps = SVNTeamPropsPreferencePage.loadCustomProperties(SVNTeamPreferences.getCustomPropertiesList(SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.CUSTOM_PROPERTIES_LIST_NAME));
		if (customProps.length > 0) {
			PredefinedPropertySet.registerProperties(customProps);
		}
		else {
			PredefinedPropertySet.registerProperty(new PredefinedProperty("    " + SVNUIMessages.AbstractPropertyEditPanel_custom_hint, PredefinedProperty.TYPE_GROUP | PredefinedProperty.TYPE_COMMON)); //$NON-NLS-1$
		}
	}
	
	protected static void registerProperties(PredefinedProperty []properties) {
		for (PredefinedProperty property : properties) {
			PredefinedPropertySet.registerProperty(property);
		}
	}
	
	protected static void registerProperty(PredefinedProperty property) {
		PredefinedPropertySet.properties.put(property.name, property);
	}
	
}
