/*************************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.eclipse.bpmn2.modeler.core.validation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesAdapter;
import org.eclipse.bpmn2.modeler.core.builder.BPMN2Nature;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceSetImpl;
import org.eclipse.bpmn2.modeler.core.model.ProxyURIConverterImplExtension;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntimeAdapter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.validation.marker.MarkerUtil;
import org.eclipse.emf.validation.model.EvaluationMode;
import org.eclipse.emf.validation.model.IConstraintStatus;
import org.eclipse.emf.validation.service.IBatchValidator;
import org.eclipse.emf.validation.service.IValidator;
import org.eclipse.emf.validation.service.ModelValidationService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.ValidatorMessage;

public class BPMN2ProjectValidator extends AbstractValidator {

    @Override
    public synchronized ValidationResult validate(ValidationEvent event, ValidationState state, IProgressMonitor monitor) {
    	IResource file = event.getResource();
        if ((event.getKind() & IResourceDelta.REMOVED) != 0 
        		|| file.isDerived(IResource.CHECK_ANCESTORS)
        		|| !(file instanceof IFile)) {
            return new ValidationResult();
        }

        ValidationResult result = null;
    	IFile modelFile = (IFile) file;
    	try {
			modelFile.deleteMarkers(null, true, IProject.DEPTH_INFINITE);

			TargetRuntime runtime = TargetRuntime.getRuntime(new FileEditorInput(modelFile));
	    	Bpmn2ModelerResourceSetImpl rs = new Bpmn2ModelerResourceSetImpl();
	    	runtime.registerExtensionResourceFactory(rs);
			URI modelUri = URI.createPlatformResourceURI(modelFile.getFullPath().toString(), true);
			rs.setURIConverter(new ProxyURIConverterImplExtension(modelUri));
	    	Map<Object,Object> options = new HashMap<Object,Object>();
	    	options.put(Bpmn2ModelerResourceSetImpl.OPTION_PROGRESS_MONITOR, monitor);
	    	rs.setLoadOptions(options);

			Resource resource = rs.createResource(modelUri, Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID);
			TargetRuntimeAdapter.adapt(resource, runtime);

            resource.load(null);
	        result = new ValidationResult();
	        if (resource.getContents().isEmpty()) {
	            ValidatorMessage message = ValidatorMessage.create(Messages.BPMN2ProjectValidator_Invalid_File, modelFile);
	            message.setType(runtime.getProblemMarkerId());
	            result.add(message);
	        } else {
	            IBatchValidator validator = ModelValidationService.getInstance().newValidator(EvaluationMode.BATCH);
	            processStatus(validator.validate(resource.getContents(), monitor), modelFile, result, runtime);
	        }
		} catch (CoreException e1) {
			e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return result;
    }
    
    public static void validate(IResource resource, IProgressMonitor monitor) {
		if (isBPMN2File(resource)) {
			Validator[] validators = ValidationFramework.getDefault().getValidatorsFor(resource);
			for (Validator v : validators) {
				if (BPMN2ProjectValidator.class.getName().equals(v.getValidatorClassname())) {
					v.validate(resource, IResourceDelta.CHANGED, null, monitor);
					break;
				}
			}
		}
    }
    
    public static void validate(IResourceDelta delta, IProgressMonitor monitor) {
		IResource resource = delta.getResource();
		if (isBPMN2File(resource)) {
			Validator[] validators = ValidationFramework.getDefault().getValidatorsFor(resource);
			for (Validator v : validators) {
				if (BPMN2ProjectValidator.class.getName().equals(v.getValidatorClassname())) {
					v.validate(resource, delta.getKind(), null, monitor);
					break;
				}
			}
		}
    }

	/**
	 * Perform a "Live" validation of the given model object. Errors resulting
	 * from a Live validation should be cause to consider the model as being
	 * corrupt and should not allow the Resource to be saved in this corrupt
	 * state, until the issues are resolved.
	 * 
	 * @param object the model object to validate.
	 * @return an IStatus object indicating the result of the validation. A
	 *         status severity of ERROR or higher should be considered reasons
	 *         for believing the model is corrupt.
	 */
    public static IStatus validateLive(EObject object) {
		IValidator<Notification> validator = ModelValidationService.getInstance().newValidator(EvaluationMode.LIVE);
    	Notification n = new ENotificationImpl((InternalEObject) object, 0, null, null, null, false);
		return validator.validate(n);
    }
    
    public static boolean isBPMN2File(IResource resource) {
    	if (resource instanceof IFile) {
	    	try {
	    		IFile file = (IFile) resource;
	    		IContentDescription cd = file.getContentDescription();
	    		if (cd!=null) {
					return Bpmn2ModelerResourceImpl.BPMN2_CONTENT_TYPE_ID.equals(
							cd.getContentType().getId());
	    		}
	    		String ext = file.getFileExtension();
	    		if ("bpmn".equals(ext) || "bpmn2".equals(ext)) //$NON-NLS-1$ //$NON-NLS-2$
	    			return true;
	    		
			} catch (Exception e) {
			}
    	}
    	return false;
    }
    
	public static boolean validateOnSave(Resource resource, IProgressMonitor monitor) {

		boolean needValidation = false;
		URI uri = resource.getURI();
		if (uri.isPlatformResource()) {
			String pathString = uri.toPlatformString(true);
			IPath path = Path.fromOSString(pathString);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			IProject project = file.getProject();
	
			if (project!=null) {
				try {
					// The BPMN2 Project Nature will allow the editor to dynamically
					// reload configuration files (a.k.a. extensions) from ".bpmn2config"
					IProjectNature bpmn2Nature = project.getNature(BPMN2Nature.NATURE_ID);
					Bpmn2Preferences preferences = Bpmn2Preferences.getInstance(project);
					if (bpmn2Nature==null) {
						if (preferences.getCheckProjectNature()) {
							Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
							String title = Messages.BPMN2ProjectValidator_Title;
							String message = NLS.bind(
								Messages.BPMN2ProjectValidator_No_BPMN2_Project_Nature,
								project.getName()
							);
							MessageDialogWithToggle result = MessageDialogWithToggle.open(
									MessageDialog.QUESTION,
									shell,
									title,
									message,
									Messages.BPMN2ProjectValidator_Dont_Ask_Again, // toggle message
									false, // toggle state
									null, // pref store
									null, // pref key
									SWT.NONE);
							if (result.getReturnCode() == IDialogConstants.YES_ID) {
								BPMN2Nature.setBPMN2Nature(project, true);
								needValidation = true;
							}
							if (result.getToggleState()) {
								// don't ask again
								preferences.setCheckProjectNature(false);
							}
						}
					}
					
					// The WST Validation Builder is required to do BPMN2 model validation
					boolean hasWSTBuilder = BPMN2Nature.hasBuilder(project, BPMN2Nature.WST_VALIDATION_BUILDER_ID);
					if (!hasWSTBuilder) {
//						if (preferences.getCheckProjectNature()) 
						{
							Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
							String title = Messages.BPMN2ProjectValidator_Title;
							String message = NLS.bind(
								Messages.BPMN2ProjectValidator_No_WST_Project_Builder,
								project.getName()
							);
							boolean result = MessageDialog.open(
									MessageDialog.QUESTION,
									shell,
									title,
									message,
									SWT.NONE);
							if (result) {
								BPMN2Nature.setBPMN2Nature(project, true);
								needValidation = true;
							}
						}
					}
	
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		
    	return needValidation;
    }
    
    public void processStatus(IStatus status, IResource resource, ValidationResult result, TargetRuntime runtime) {
        if (status.isMultiStatus()) {
            for (IStatus child : status.getChildren()) {
                processStatus(child, resource, result, runtime);
            }
        } else if (!status.isOK()) {
            result.add(createValidationMessage(status, resource, runtime));
        }
    }

    public ValidatorMessage createValidationMessage(IStatus status, IResource resource, TargetRuntime runtime) {
        ValidatorMessage message = ValidatorMessage.create(status.getMessage(), resource);
        switch (status.getSeverity()) {
        case IStatus.INFO:
            message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
            break;
        case IStatus.WARNING:
            message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
            break;
        case IStatus.ERROR:
        case IStatus.CANCEL:
            message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            break;
        }

        if (status instanceof IConstraintStatus) {
            IConstraintStatus ics = (IConstraintStatus) status;
            EObject object = ics.getTarget();
    		ExtendedPropertiesAdapter adapter = ExtendedPropertiesAdapter.adapt(object);
			if (adapter!=null) {
				Object lineNumber = adapter.getProperty(ExtendedPropertiesAdapter.LINE_NUMBER);
				if (lineNumber!=null) {
					message.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				}
			}

            message.setAttribute(EValidator.URI_ATTRIBUTE, EcoreUtil.getURI(object).toString());
            message.setAttribute(MarkerUtil.RULE_ATTRIBUTE, ics.getConstraint().getDescriptor().getId());
            if (ics.getResultLocus().size() > 0) {
                StringBuffer relatedUris = new StringBuffer();
                for (EObject eobject : ics.getResultLocus()) {
                    relatedUris.append(EcoreUtil.getURI(eobject).toString()).append(" "); //$NON-NLS-1$
                }
                relatedUris.deleteCharAt(relatedUris.length() - 1);
                String uris = relatedUris.toString();
                message.setAttribute(EValidator.RELATED_URIS_ATTRIBUTE, uris);
            }
        }

        message.setType(runtime.getProblemMarkerId());

        return message;
    }

    @Override
    public void clean(IProject project, ValidationState state, IProgressMonitor monitor) {
        super.clean(project, state, monitor);
        try {
            project.deleteMarkers(null, false, IProject.DEPTH_INFINITE);
        } catch (CoreException e) {
            Activator.getDefault().getLog().log(e.getStatus());
        }
    }
}
