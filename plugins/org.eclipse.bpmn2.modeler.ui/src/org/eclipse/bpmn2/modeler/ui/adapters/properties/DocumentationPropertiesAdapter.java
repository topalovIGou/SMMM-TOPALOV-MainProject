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

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Documentation;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.adapters.FeatureDescriptor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;

/**
 * @author Bob Brodt
 *
 */
public class DocumentationPropertiesAdapter extends ExtendedPropertiesAdapter<Documentation> {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public DocumentationPropertiesAdapter(AdapterFactory adapterFactory, Documentation object) {
		super(adapterFactory, object);

    	EStructuralFeature feature = Bpmn2Package.eINSTANCE.getDocumentation_Text();
    	setFeatureDescriptor(feature,
			new FeatureDescriptor<Documentation>(this,object,feature) {
    		
    			@Override
    	   		protected void internalSet(Documentation documentation, EStructuralFeature feature, Object value, int index) {
    				String text = value==null ? "" : value.toString(); //$NON-NLS-1$
    				Object b = DocumentationPropertiesAdapter.this.getProperty(feature, "CDATA"); //$NON-NLS-1$
    				if (b !=null)
    					setTextCDATA(documentation, text);
    				else
    					documentation.setText(text);
    			}

				@Override
				public boolean isMultiLine() {
					// formal expression body is always a multiline text field
					return true;
				}
				
				private void setTextCDATA(Documentation documentation, String text) {
					documentation.getMixed().clear();
					FeatureMap.Entry cdata = FeatureMapUtil.createCDATAEntry(text);
					documentation.getMixed().add(cdata);

				}
			}
    	);
    	
    	// By default, Documentation.text is serialized as CDATA instead of an XML attribute value.
    	setProperty(feature, "CDATA", Boolean.TRUE); //$NON-NLS-1$
	}

}
