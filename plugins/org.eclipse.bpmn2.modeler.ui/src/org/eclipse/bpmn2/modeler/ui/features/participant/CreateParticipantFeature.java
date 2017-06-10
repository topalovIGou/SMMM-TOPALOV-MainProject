/******************************************************************************* 
 * Copyright (c) 2011, 2012 Red Hat, Inc. 
 *  All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.features.participant;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;

public class CreateParticipantFeature extends AbstractBpmn2CreateFeature<Participant> {
	
	public CreateParticipantFeature(IFeatureProvider fp) {
	    super(fp);
    }

	@Override
    public boolean canCreate(ICreateContext context) {
		if (context.getTargetContainer() instanceof Diagram) {
			BPMNDiagram bpmnDiagram = BusinessObjectUtil.getFirstElementOfType(context.getTargetContainer(), BPMNDiagram.class);
			BaseElement bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
			if (bpmnElement instanceof Process) {
				for (Participant p : ModelUtil.getAllObjectsOfType(bpmnElement.eResource(), Participant.class)) {
					if (p.getProcessRef() == bpmnElement && FeatureSupport.hasBpmnDiagram(bpmnElement)) {
						return false;
					}
				}
				return true;
			}
			if (bpmnElement instanceof Collaboration || bpmnElement==null)
				return true;
		}
		return false;
    }

	@Override
	public Object[] create(ICreateContext context) {
		Participant participant = createBusinessObject(context);
		participant.setName(Messages.CreateParticipantFeature_Default_Pool_Name + ModelUtil.getIDNumber(participant.getId()));
		PictogramElement pe = addGraphicalRepresentation(context, participant);
		return new Object[] { participant, pe };
	}
	
	@Override
	public String getCreateImageId() {
	    return ImageProvider.IMG_16_PARTICIPANT;
	}
	
	@Override
	public String getCreateLargeImageId() {
	    return getCreateImageId(); // FIXME
	}

	/* (non-Javadoc)
	 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateFeature#getBusinessObjectClass()
	 */
	@Override
	public EClass getBusinessObjectClass() {
		return Bpmn2Package.eINSTANCE.getParticipant();
	}
}
