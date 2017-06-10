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
 * @author Bob Brodt
 ******************************************************************************/


package org.eclipse.bpmn2.modeler.ui.property.events;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractListComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.BooleanObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.swt.widgets.Composite;

public class CommonEventDetailComposite extends DefaultDetailComposite {

	protected AbstractListComposite inputTable;
	protected AbstractListComposite outputTable;
	protected EventDefinitionsListComposite eventsTable;

	public CommonEventDetailComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * @param section
	 */
	public CommonEventDetailComposite(AbstractBpmn2PropertySection section) {
		super(section);
	}

	@Override
	public void cleanBindings() {
		super.cleanBindings();
		inputTable = null;
		outputTable = null;
		eventsTable = null;
	}
	
	protected void bindAttribute(Composite parent, EObject object, EAttribute attribute, String label) {

		if (isModelObjectEnabled(object.eClass(), attribute)) {
			if (object instanceof BoundaryEvent && attribute==Bpmn2Package.eINSTANCE.getBoundaryEvent_CancelActivity()) {
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=417207
				// the Cancel Activity checkbox should always be TRUE
				// if the Boundary Event contains a Error Event Definition,
				// and should be hidden when it contains a Compensate Event Definition.
				final BoundaryEvent be = (BoundaryEvent) object;

				if (label==null)
					label = getBusinessObjectDelegate().getLabel(be, attribute);
				
				final ObjectEditor editor = new BooleanObjectEditor(this,be,attribute) {
					public boolean setValue(final Object result) {
						if (hasEventDefinition(be,ErrorEventDefinition.class)) {
							setCancel(be,true);
							this.setVisible(true);
						}
						else if (hasEventDefinition(be,CompensateEventDefinition.class)) {
							setCancel(be,false);
							this.setVisible(false);
						}
						else if (result instanceof Boolean) {
							setCancel(be,(Boolean)result);
							this.setVisible(true);
						}
						return true;
					}

					@Override
					public void notifyChanged(Notification notification) {
						super.notifyChanged(notification);
						if (notification.getEventType() == -1) {
							boolean cancel = be.isCancelActivity();
							if (hasEventDefinition(be,ErrorEventDefinition.class)) {
								if (cancel!=true) {
									setCancel(be,true);
									this.setVisible(true);
								}
							}
							else if (hasEventDefinition(be,CompensateEventDefinition.class)) {
								if (cancel!=false) {
									setCancel(be,false);
									this.setVisible(false);
								}
							}
							else {
								if (cancel!=button.getSelection()) {
									setCancel(be,button.getSelection());
									this.setVisible(true);
								}
							}
						}
					}
				};
				
				editor.createControl(getAttributesParent(),label);
			}
			else
				super.bindAttribute(parent, object, attribute, label);
		}
	}

	private void setCancel(final BoundaryEvent be, final boolean cancel) {
		final TransactionalEditingDomain domain = getDiagramEditor().getEditingDomain();
		domain.getCommandStack().execute(new RecordingCommand(domain) {
			@Override
			protected void doExecute() {
				be.setCancelActivity(cancel);
			}
		});
	}
	
	private boolean hasEventDefinition(BoundaryEvent be, Class clazz) {
		for (EventDefinition ed : be.getEventDefinitions()) {
			if (clazz.isInstance(ed))
				return true;
		}
		return false;
	}

	@Override
	public AbstractPropertiesProvider getPropertiesProvider(final EObject object) {
		if (propertiesProvider==null) {
			propertiesProvider = new AbstractPropertiesProvider(object) {
				@Override
				public String[] getProperties() {
					List<String> result = new ArrayList<String>();
					if (object instanceof StartEvent && object.eContainer() instanceof SubProcess) {
						SubProcess sp = (SubProcess)object.eContainer();
						if (sp.isTriggeredByEvent()) {
							result.add("isInterrupting"); //$NON-NLS-1$
						}
					}
					if (object instanceof CatchEvent) {
						CatchEvent ce = (CatchEvent)object;
						if (ce.getEventDefinitions().size()>1)
							result.add("parallelMultiple"); //$NON-NLS-1$
					}
					if (object instanceof BoundaryEvent) {
						boolean add = true;
						BoundaryEvent be = (BoundaryEvent)object;
						for (EventDefinition ed : be.getEventDefinitions()) {
							if (ed instanceof ErrorEventDefinition || ed instanceof CompensateEventDefinition) {
								add = false;
								break;
							}
						}
						if (add)
							result.add("cancelActivity"); //$NON-NLS-1$
					}
					result.add("eventDefinitions"); //$NON-NLS-1$
					result.add("dataInputs"); //$NON-NLS-1$
					result.add("dataOutputs"); //$NON-NLS-1$
					result.add("properties"); //$NON-NLS-1$
					return result.toArray(new String[result.size()]); 
				}
			};
		}
		return propertiesProvider;
	}

	@Override
	protected AbstractListComposite bindList(EObject object, EStructuralFeature feature, EClass listItemClass) {
		if (object instanceof CatchEvent || object instanceof ThrowEvent) {
			if (isModelObjectEnabled(object.eClass(), feature)) {
				if ("eventDefinitions".equals(feature.getName())) { //$NON-NLS-1$
					eventsTable = new EventDefinitionsListComposite(this, (Event)object);
					eventsTable.bindList(object, feature);
					eventsTable.setTitle(Messages.CommonEventDetailComposite_Event_Definition_Title);
					return eventsTable;
				}
				if ("dataInputs".equals(feature.getName())) { //$NON-NLS-1$
					if (object instanceof ThrowEvent) {
						ThrowEvent throwEvent = (ThrowEvent)object;
						inputTable = new DataInputsListComposite(this, throwEvent);
						inputTable.bindList(object, feature);
						inputTable.setTitle(Messages.CommonEventDetailComposite_Input_Parameters_Title);//Messages.CommonEventDetailComposite_Input_Parameters_Title);
						return inputTable;
					}
				}
				if ("dataOutputs".equals(feature.getName())) { //$NON-NLS-1$
					if (object instanceof CatchEvent) {
						CatchEvent catchEvent = (CatchEvent)object;
						outputTable = new DataOutputsListComposite(this, catchEvent);
						outputTable.bindList(catchEvent, feature);
						outputTable.setTitle(Messages.CommonEventDetailComposite_Output_Parameters_Title);//Messages.CommonEventDetailComposite_Output_Parameters_Title);
						return outputTable;
					}
				}
				if ("properties".equals(feature.getName())) { //$NON-NLS-1$
					return super.bindList(object, feature, listItemClass);
				}
			}
			return null;
		}
		return super.bindList(object, feature, listItemClass);
	}
	
}