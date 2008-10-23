/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Igor Burilo - Bug 245509: Improve extract log
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNTeamPlugin;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;

/**
 * Performs initialization of extract operations log file.
 * 
 * @author Alexander Gurov
 */
public class InitExtractLogOperation extends AbstractActionOperation {
	public static final String COMPLETE_LOG_NAME = "/changes.log";
	
	protected HashMap<String, List<String>> extractParticipants;
	protected String logPath;
	
	public InitExtractLogOperation(String logPath) {
		super("Operation.InitExtractLog");
		this.logPath = logPath;
		this.extractParticipants = new HashMap<String, List<String>>();
	}

	protected void runImpl(IProgressMonitor monitor) throws Exception {
		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
		String date = formatter.format(new Date());
		this.logImpl("");
		this.logImpl(date);
		this.logImpl("===============================================================================");
	}

	public void log(String participant, String status) {
		if (this.extractParticipants.get(status) == null) {
			this.extractParticipants.put(status, new ArrayList<String>());
		}
		this.extractParticipants.get(status).add(participant);
	}
	
	public void flushLog() {
		HashMap<String, List<String>> sortedParticipants = new HashMap<String, List<String>>();
		for (String status : this.extractParticipants.keySet()) {
			String [] participants = this.extractParticipants.get(status).toArray(new String [0]);
			Arrays.sort(participants);
			ArrayList<String> participantsToLog = new ArrayList<String>();
			for (int i = 0; i < participants.length; i++) {
				if (status.equals(IStateFilter.ST_DELETED)) {
					boolean parentIsAlreadyLogged = false;
					for (String logged : participantsToLog) {
						if (participants[i].startsWith(logged + "\\")) {
							parentIsAlreadyLogged = true;
							break;
						}
					}
					if (!parentIsAlreadyLogged) {
						participantsToLog.add(participants[i]);
					}
				}
				else if (i + 1 >= participants.length || !participants[i + 1].startsWith(participants[i] + "\\")) {
					participantsToLog.add(participants[i]);
				}
			}
			sortedParticipants.put(status, participantsToLog);
		}
		for (String status : sortedParticipants.keySet()) {
			for (String participant : sortedParticipants.get(status)) {
				this.logImpl(SVNTeamPlugin.instance().getResource("Console.Status." + status) + " " + participant);
			}
			this.logImpl("");
		}
		this.extractParticipants.clear();
	}

	private void logImpl(String line) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(this.logPath + InitExtractLogOperation.COMPLETE_LOG_NAME, true);
			writer.write(line);
			writer.write(System.getProperty("line.separator"));
		}
		catch (IOException ex) {
			//ignore
		}
		finally {
			if (writer != null) {
				try {writer.close();} catch (Exception ex) {}
			}
		}
	}

}