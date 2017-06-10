/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features.choreography;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.core.features.choreography.messages"; //$NON-NLS-1$
	public static String ChoreographyUtil_Undefined_Message;
	public static String UpdateChoreographyInitiatingParticipantFeature_Initiating_Participant_Changed;
	public static String UpdateChoreographyMessageLinkFeature_Message_Link_Visible_Changed;
	public static String UpdateChoreographyMessageLinkFeature_Participants_Changed;
	public static String UpdateChoreographyParticipantBandsFeature_Participant_Name_Changed;
	public static String UpdateChoreographyParticipantRefsFeature_Parkticipants_Changed;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
