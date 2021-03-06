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

package org.eclipse.team.svn.ui.utility;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;
import org.eclipse.team.svn.ui.preferences.SVNTeamPreferences;

/**
 * This storage allow us to handle comment, user name etc. histories in one way
 * 
 * @author Alexander Gurov
 */
public class UserInputHistory extends InputHistory {
    
    protected static final String HISTORY_NAME_BASE = "history."; //$NON-NLS-1$
    protected final static String ENCODING = "UTF-8";
    
    protected int depth;
    protected List history;

    public UserInputHistory(String name) {
        this(name, SVNTeamPreferences.getCommentTemplatesInt(SVNTeamUIPlugin.instance().getPreferenceStore(), SVNTeamPreferences.COMMENT_SAVED_PATHS_COUNT_NAME));
    }

    public UserInputHistory(String name, int depth) {
    	super(name, InputHistory.TYPE_STRING, null);
        this.depth = depth;
        
        this.loadHistoryLines();
        
        if (this.history.size() > this.depth) {
        	ListIterator iter = this.history.listIterator(this.depth);
        	while (iter.hasNext()) {
        		iter.next();
        		iter.remove();
        	}
        	this.saveHistoryLines();
        }
    }
    
    public int getDepth() {
        return this.depth;
    }
    
    public String []getHistory() {
        return (String [])this.history.toArray(new String[this.history.size()]);
    }
    
    public void addLine(String line) {
        if (line == null || line.trim().length() == 0) {
            return;
        }
    	this.history.remove(line);
        this.history.add(0, line);
        if (this.history.size() > this.depth) {
            this.history.remove(this.history.size() - 1);
        }
        this.saveHistoryLines();
    }
    
    public void clear() {
        this.history.clear();
        super.clear();
    }

    protected void loadHistoryLines() {
        this.history = new ArrayList();
        String historyData = (String)this.value;
        if (historyData != null && historyData.length() > 0) {
            String []historyArray = historyData.split(";"); //$NON-NLS-1$
            for (int i = 0; i < historyArray.length; i++) {
            	try {
            		historyArray[i] = new String(Base64.decode(historyArray[i].getBytes(UserInputHistory.ENCODING)), UserInputHistory.ENCODING);
                } catch (UnsupportedEncodingException e) {
                	historyArray[i] = new String(Base64.decode(historyArray[i].getBytes()));
                }
            }
            this.history.addAll(Arrays.asList(historyArray));
        }
    }
    
    protected void saveHistoryLines() {
        String result = ""; //$NON-NLS-1$
        for (Iterator it = this.history.iterator(); it.hasNext(); ) {
            String str = (String)it.next();
            try {
				str = new String(Base64.encode(str.getBytes(UserInputHistory.ENCODING)), UserInputHistory.ENCODING);
			} catch (UnsupportedEncodingException e) {
				str = new String(Base64.encode(str.getBytes()));
			}
            result += result.length() == 0 ? str : (";" + str); //$NON-NLS-1$
        }
        this.setValue(result);
    }
    
}
