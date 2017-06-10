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
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.help.IHelpContexts;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.JBPM5RuntimeExtension;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "bpmn". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class NewJbpmProcessWizard extends Wizard implements INewWizard {
	private NewJbpmProcessWizardPage1 page;
	private ISelection selection;
	private String processName;
	private String processId;
	private String packageName;
	private boolean isSetJbpmRuntime;

	/**
	 * Constructor for NewJbpmProcessWizard.
	 */
	public NewJbpmProcessWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new NewJbpmProcessWizardPage1(selection);
		addPage(page);
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), IHelpContexts.JBPM_New_File_Wizard);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		processName = page.getProcessName();
		processId = page.getProcessId();
		packageName = page.getPackageName();
		isSetJbpmRuntime = page.isSetJbpmRuntime();
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), Messages.NewJbpmProcessWizard_Error_Title, realException.getMessage());
			Activator.logError(e);
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private void doFinish(
		String containerName,
		String fileName,
		IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask(NLS.bind(Messages.NewJbpmProcessWizard_Monitor_Title,fileName), 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException(NLS.bind(Messages.NewJbpmProcessWizard_Error_No_Container,containerName));
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		try {
			InputStream stream = openContentStream();
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		
		if (isSetJbpmRuntime) {
			monitor.setTaskName(Messages.NewJbpmProcessWizard_Configuring_Project_Message);
			Bpmn2Preferences prefs = Bpmn2Preferences.getInstance(container.getProject());
			prefs.useProjectPreferences();
			TargetRuntime rt = TargetRuntime.getRuntime(JBPM5RuntimeExtension.JBPM5_RUNTIME_ID);
			prefs.setRuntime(rt);
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				throwCoreException(e.getMessage());
			}
		}
		monitor.worked(1);
		monitor.setTaskName(Messages.NewJbpmProcessWizard_Opening_File_Message);
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}
	
	/**
	 * We will initialize file contents with a sample text.
	 */

	private InputStream openContentStream() {
		String contents =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+  //$NON-NLS-1$
			"<bpmn2:definitions\n"+ //$NON-NLS-1$
			"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+ //$NON-NLS-1$
			"	xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"+ //$NON-NLS-1$
			"	xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n"+ //$NON-NLS-1$
			"	xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"\n"+ //$NON-NLS-1$
			"	xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n"+ //$NON-NLS-1$
			"	xmlns:tns=\"http://www.jboss.org/drools\"\n"+ //$NON-NLS-1$
			"	xmlns=\"http://www.jboss.org/drools\"\n"+ //$NON-NLS-1$
			"	xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd http://www.jboss.org/drools drools.xsd http://www.bpsim.org/schemas/1.0 bpsim.xsd\"\n"+ //$NON-NLS-1$
			"	id=\"Definition\"\n"+ //$NON-NLS-1$
			"	expressionLanguage=\"http://www.mvel.org/2.0\"\n"+ //$NON-NLS-1$
			"	targetNamespace=\"http://www.jboss.org/drools\"\n"+ //$NON-NLS-1$
			"	typeLanguage=\"http://www.java.com/javaTypes\">\n"+ //$NON-NLS-1$
			"\n"+ //$NON-NLS-1$
			"  <bpmn2:process processType=\"Private\" isExecutable=\"true\""+ //$NON-NLS-1$
			" id=\""+processId+"\""+ //$NON-NLS-1$ //$NON-NLS-2$
			" name=\""+processName+"\""+ //$NON-NLS-1$ //$NON-NLS-2$
			" tns:packageName=\""+packageName+"\""+ //$NON-NLS-1$ //$NON-NLS-2$
			" >\n"+ //$NON-NLS-1$
			"    <bpmn2:startEvent id=\"StartEvent_1\" name=\"StartProcess\"/>\n"+ //$NON-NLS-1$
			"  </bpmn2:process>\n"+ //$NON-NLS-1$
			"\n"+ //$NON-NLS-1$
			"  <bpmndi:BPMNDiagram>\n"+ //$NON-NLS-1$
			"    <bpmndi:BPMNPlane bpmnElement=\""+processId+"\" >\n"+ //$NON-NLS-1$ //$NON-NLS-2$
			"      <bpmndi:BPMNShape bpmnElement=\"StartEvent_1\" >\n"+ //$NON-NLS-1$
			"        <dc:Bounds x=\"45\" y=\"45\" />\n"+ //$NON-NLS-1$
			"      </bpmndi:BPMNShape>\n"+ //$NON-NLS-1$
			"      </bpmndi:BPMNPlane>\n"+ //$NON-NLS-1$
			"  </bpmndi:BPMNDiagram>\n"+ //$NON-NLS-1$
			"\n"+ //$NON-NLS-1$
			"</bpmn2:definitions>\n"; //$NON-NLS-1$
		
		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5", IStatus.OK, message, null); //$NON-NLS-1$
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
