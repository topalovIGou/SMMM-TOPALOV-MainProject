/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.examples.customtask;

import org.eclipse.bpmn2.modeler.core.merrimac.clad.AbstractDetailComposite;
import org.eclipse.bpmn2.modeler.core.merrimac.clad.DefaultPropertySection;
import org.eclipse.swt.widgets.Composite;

public class MyTaskPropertySection extends DefaultPropertySection {

	public MyTaskPropertySection() {
		super();
	}

	@Override
	protected AbstractDetailComposite createSectionRoot() {
		// This constructor is used to create the detail composite for use in the Property Viewer.
		return new MyTaskDetailComposite(this);
	}

	@Override
	public AbstractDetailComposite createSectionRoot(Composite parent, int style) {
		// This constructor is used to create the detail composite for use in the popup Property Dialog.
		return new MyTaskDetailComposite(parent, style);
	}
}
