/*******************************************************************************
 * Copyright (c) 2005-2006 Polarion Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elena Matokhina - Initial API and implementation
 *******************************************************************************/

package org.eclipse.team.svn.ui.verifier;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.team.svn.ui.SVNTeamUIPlugin;

/**
 * Property name verifier
 * 
 * @author Elena Matokhina
 */
public class PropertyNameVerifier extends AbstractFormattedVerifier {
    protected static String ERROR_MESSAGE_LETTER;
    protected static String ERROR_MESSAGE_SYMBOLS;
        
    public PropertyNameVerifier(String fieldName) {
        super(fieldName);
        PropertyNameVerifier.ERROR_MESSAGE_LETTER = SVNTeamUIPlugin.instance().getResource("Verifier.PropertyName.Letter");
        PropertyNameVerifier.ERROR_MESSAGE_LETTER = MessageFormat.format(PropertyNameVerifier.ERROR_MESSAGE_LETTER, new String[] {AbstractFormattedVerifier.FIELD_NAME});
        PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS = SVNTeamUIPlugin.instance().getResource("Verifier.PropertyName.Symbols");
        PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS = MessageFormat.format(PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS, new String[] {AbstractFormattedVerifier.FIELD_NAME});
    }

    protected String getErrorMessageImpl(Control input) {
        String property = this.getText(input);
        if (property.trim().length() == 0) {
            return null;
        }
        Pattern pattern = Pattern.compile("[a-zA-Z].*");
        Matcher matcher = pattern.matcher(property);
        if (!matcher.matches()) {
        	return PropertyNameVerifier.ERROR_MESSAGE_LETTER;
        }
        pattern = Pattern.compile("[a-zA-Z0-9:\\-_.]*");
        if (!pattern.matcher(property).matches()) {
        	return PropertyNameVerifier.ERROR_MESSAGE_SYMBOLS;
        }
        
        return null;
    }

    protected String getWarningMessageImpl(Control input) {
        return null;
    }

}


