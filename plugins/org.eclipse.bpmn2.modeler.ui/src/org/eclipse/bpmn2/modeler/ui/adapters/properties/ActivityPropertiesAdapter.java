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

package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.AdHocSubProcess;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.MultiInstanceLoopCharacteristics;
import org.eclipse.bpmn2.StandardLoopCharacteristics;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerFactory;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Bob Brodt
 *
 */
public class ActivityPropertiesAdapter<T extends Activity> extends ExtendedPropertiesAdapter<T> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public ActivityPropertiesAdapter(AdapterFactory adapterFactory, T object) {
		super(adapterFactory, object);
		
		EStructuralFeature feature = Bpmn2Package.eINSTANCE.getActivity_LoopCharacteristics();
    	setProperty(feature, UI_CAN_CREATE_NEW, Boolean.FALSE);
    	setProperty(feature, UI_CAN_EDIT, Boolean.FALSE);
		setFeatureDescriptor(feature,
			new FeatureDescriptor<T>(this,object,feature) {
				@Override
				protected void internalSet(T object, EStructuralFeature feature, Object value, int index) {
					if (value instanceof String) {
						if ("MultiInstanceLoopCharacteristics".equals(value)) { //$NON-NLS-1$
							MultiInstanceLoopCharacteristics lc = Bpmn2ModelerFactory.createObject(getResource(), MultiInstanceLoopCharacteristics.class);
							value = lc;
						}
						else if ("StandardLoopCharacteristics".equals(value)) { //$NON-NLS-1$
							StandardLoopCharacteristics lc = Bpmn2ModelerFactory.createObject(getResource(), StandardLoopCharacteristics.class);
							value = lc;
						}
					}
					super.internalSet(object, feature, value, index);
				}
			}
		);

		feature = Bpmn2Package.eINSTANCE.getActivity_Properties();
		setFeatureDescriptor(feature,
			new FeatureDescriptor<T>(this,object,feature) {
				@Override
				public EObject createFeature(Resource resource, EClass eclass) {
					return PropertyPropertiesAdapter.createProperty(object.getProperties());
				}
			}
		);
		
		feature = Bpmn2Package.eINSTANCE.getActivity_IsForCompensation();
		setFeatureDescriptor(feature,
			new FeatureDescriptor<T>(this,object,feature) {
				@Override
				protected void internalSet(T object, EStructuralFeature feature, Object value, int index) {
					if (value instanceof Boolean) {
						if (!(Boolean)value) {
							// This Activity is no longer being used for Compensation.
							// If any Compensation Boundary Events reference this
							// Activity, set their Activity References to null.
							TreeIterator<EObject> iter = ModelUtil.getDefinitions(object).eAllContents();
							while (iter.hasNext()) {
								EObject o = iter.next();
								if (o instanceof CompensateEventDefinition) {
									CompensateEventDefinition ced = (CompensateEventDefinition) o;
									if (ced.getActivityRef() == object) {
										ExtendedPropertiesProvider.setValue(ced, Bpmn2Package.eINSTANCE.getCompensateEventDefinition_ActivityRef(), null);
									}
								}
							}
						}
					}
					super.internalSet(object, feature, value, index);
				}
			}
		);
		
		if (object instanceof AdHocSubProcess) {
			feature = Bpmn2Package.eINSTANCE.getAdHocSubProcess_CompletionCondition();
			setFeatureDescriptor(feature,
					new FeatureDescriptor<T>(this,object,feature) {
						@Override
						public String getLabel() {
							return Messages.ActivityPropertiesAdapter_CompletionCondition_Label;
						}
					}
				);
		}
	}

}
