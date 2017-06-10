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
package org.eclipse.bpmn2.modeler.core.features.containers.participant;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.bpmn2.modeler.core.features.containers.participant.messages"; //$NON-NLS-1$
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

	public static String CreateParticipantReferenceFeature_Create_Pool_Reference;
	public static String CreateParticipantReferenceFeature_Create_Pool_Reference_In_Main_Diagram;
	public static String DirectEditParticipantFeature_Invalid_Empty;
	public static String DirectEditParticipantFeature_Invalid_Linebreak;
	public static String UpdateParticipantMultiplicityFeature_Participant_Multiplicity_Changed;
}
