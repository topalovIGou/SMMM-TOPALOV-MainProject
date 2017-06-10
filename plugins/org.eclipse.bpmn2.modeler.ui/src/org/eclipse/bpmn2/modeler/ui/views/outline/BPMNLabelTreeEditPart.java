/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 * All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.views.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.di.BPMNLabel;
import org.eclipse.dd.dc.Bounds;

public class BPMNLabelTreeEditPart extends AbstractGraphicsTreeEditPart {

	public BPMNLabelTreeEditPart(DiagramTreeEditPart dep, BPMNLabel BPMNLabel) {
		super(dep, BPMNLabel);
	}

	public BPMNLabel getBPMNLabel() {
		return (BPMNLabel) getModel();
	}

	// ======================= overwriteable behaviour ========================

	/**
	 * Creates the EditPolicies of this EditPart. Subclasses often overwrite
	 * this method to change the behaviour of the editpart.
	 */
	@Override
	protected void createEditPolicies() {
	}

	@Override
	protected List<Object> getModelChildren() {
		List<Object> retList = new ArrayList<Object>();
		BPMNLabel bpmnLabel = getBPMNLabel();
		// TODO
		return retList;
	}
	
	@Override
	protected String getText() {
		BPMNLabel bpmnLabel = getBPMNLabel();
		if (bpmnLabel.getBounds()!=null) {
			Bounds b = bpmnLabel.getBounds();
			return "Label: x="+b.getX()+", y="+b.getY()+" w="+b.getWidth()+" h="+b.getHeight(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		return "Label"; //$NON-NLS-1$
	}
}