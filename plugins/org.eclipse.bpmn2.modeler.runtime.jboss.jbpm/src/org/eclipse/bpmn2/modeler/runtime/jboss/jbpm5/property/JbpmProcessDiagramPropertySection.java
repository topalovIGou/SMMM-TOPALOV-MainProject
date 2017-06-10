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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property;

import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractBpmn2PropertySection;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.JavaPackageNameObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.NCNameObjectEditor;
import org.eclipse.bpmn2.modeler.core.merrimac.dialogs.ObjectEditor;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.ProcessDiagramDetailComposite;
import org.eclipse.bpmn2.modeler.ui.property.diagrams.ProcessDiagramPropertySection;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.widgets.Composite;

/**
 * This is an empty tab section which simply exists to hide the "Basic" tab
 * defined the editor UI plugin.
 * 
 * @author Bob Brodt
 *
 */
public class JbpmProcessDiagramPropertySection extends ProcessDiagramPropertySection {

	public JbpmProcessDiagramPropertySection() {
		super();
	}

	@Override
	protected AbstractDetailComposite createSectionRoot() {
		return new JbpmProcessDiagramDetailComposite(this);
	}

	@Override
	public AbstractDetailComposite createSectionRoot(Composite parent, int style) {
		return new JbpmProcessDiagramDetailComposite(parent,style);
	}

	public class JbpmProcessDiagramDetailComposite extends ProcessDiagramDetailComposite {
		
		public JbpmProcessDiagramDetailComposite(AbstractBpmn2PropertySection section) {
			super(section);
		}

		public JbpmProcessDiagramDetailComposite(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		protected void bindAttribute(Composite parent, EObject object, EAttribute attribute, String label) {
			if (label==null)
				label = getBusinessObjectDelegate().getLabel(object, attribute);
			if ("id".equals(attribute.getName())) { //$NON-NLS-1$
				ObjectEditor editor = new NCNameObjectEditor(this,object,attribute);
				editor.createControl(parent,label);
			}
			else if ("packageName".equals(attribute.getName())) { //$NON-NLS-1$
				ObjectEditor editor = new JavaPackageNameObjectEditor(this,object,attribute);
				editor.createControl(parent,label);
			}
			else
				super.bindAttribute(parent, object, attribute, label);
		}

		protected boolean isModelObjectEnabled(String className, String featureName) {
			if ("id".equals(featureName)) //$NON-NLS-1$
					return true;
			return super.isModelObjectEnabled(className,featureName);
		}
	};
}
