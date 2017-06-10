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

package org.eclipse.bpmn2.modeler.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.modeler.core.adapters.AdapterRegistry;
import org.eclipse.bpmn2.modeler.core.adapters.INamespaceMap;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.xsd.XSDNamedComponent;
import org.eclipse.xsd.XSDSchema;

/**
 * @author Bob Brodt
 *
 */
public class NamespaceUtil {

	public static Map<String,String> getXMLNSPrefixMap(Resource resource) {
		if (resource!=null) {
			EList<EObject> contents = resource.getContents();
			if (!contents.isEmpty() && contents.get(0) instanceof DocumentRoot) {
				return ((DocumentRoot)contents.get(0)).getXMLNSPrefixMap();
			}
		}
		return null;
	}

	public static String getNamespaceForPrefix(Resource resource, String prefix) {
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null)
			return map.get(prefix);
		return null;
	}
	
	public static String getPrefixForNamespace(Resource resource, String namespace) {
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			for (Entry<String, String> e : map.entrySet()) {
				String value = e.getValue();
				if (value!=null && value.equals(namespace))
					return e.getKey();
			}
		}
		return null;
	}
	
	public static String normalizeQName(Resource resource, QName qname) {
		String localPart = qname.getLocalPart();
		String namespace = qname.getNamespaceURI();
		String prefix = getPrefixForNamespace(resource, namespace);
		if (prefix!=null && !prefix.isEmpty()) {
			return prefix + ":" + localPart; //$NON-NLS-1$
		}
		prefix = qname.getPrefix();
		if (prefix!=null && !prefix.isEmpty()) {
			return prefix + ":" + localPart; //$NON-NLS-1$
		}
		return localPart;
	}
	
	public static boolean hasNamespace(Resource resource, String namespace) {
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			for (Entry<String, String> e : map.entrySet()) {
				String value = e.getValue();
				if (value!=null && value.equals(namespace))
					return true;
			}
		}
		return false;
	}
	
	public static boolean hasPrefix(Resource resource, String prefix) {
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			return map.containsKey(prefix);
		}
		return false;
	}
	
	public static String addNamespace(Resource resource, String namespace) {
		if (hasNamespace(resource,namespace))
			return null;
		// generate a prefix
		String prefix = null;
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			prefix = createUniquePrefix(map, "ns"); //$NON-NLS-1$
			addNamespace(resource, prefix, namespace);
		}
		return prefix;
	}
	
	public static String createUniquePrefix(Map<String,String> map, String prefix) {
		if (map!=null) {
			if (map.containsKey(prefix)) {
				int index = 1;
				while (map.containsKey(prefix+index))
					++index;
				prefix = prefix + index;
			}
		}
		return prefix;
	}
	
	public static String addNamespace(final Resource resource, String prefix, final String namespace) {
		final Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			if (prefix==null) {
				prefix = "ns"; //$NON-NLS-1$
			}
			map.put(prefix, namespace);
			return prefix;
		}
		return null;
	}
	
	/**
	 * Remove the namespace prefix mapping for a given namespace.
	 * 
	 * @param object - any EObject in the BPMN2 Resource 
	 * @param namespace - the namespace to be removed
	 * @return the namespace prefix if the mapping was successfully removed
	 * or null otherwise
	 */
	public static String removeNamespace(final Resource resource, final String namespace) {
		final Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			String prefix = null;
			for (Entry<String, String> e : map.entrySet()) {
				String value = e.getValue();
				if (value!=null && value.equals(namespace)) {
					prefix = e.getKey();
					break;
				}
			}
			if (prefix!=null && map.containsKey(prefix)) {
				TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(resource);
				if (domain != null) {
					final String p = prefix;
					domain.getCommandStack().execute(new RecordingCommand(domain) {
						@Override
						protected void doExecute() {
							map.remove(p);
						}
					});
				}
				return prefix;
			}
		}
		return null;
	}
	
	/**
	 * Remove the namespace prefix mapping for a given prefix.
	 * 
	 * @param object - any EObject in the BPMN2 Resource 
	 * @param prefix - the namespace prefix to be removed
	 * @return the namespace if the mapping was successfully removed
	 * or null otherwise
	 */
	public static String removeNamespaceForPrefix(final Resource resource, final String prefix) {
		final Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null && map.containsKey(prefix)) {
			TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(resource);
			if (domain != null) {
				String ns = map.get(prefix);
				domain.getCommandStack().execute(new RecordingCommand(domain) {
					@Override
					protected void doExecute() {
						map.remove(prefix);
					}
				});
				return ns;
			}
		}
		return null;
	}

	/**
	 * @param eObject
	 * @return the namespace map for the given object.
	 */
	
	@SuppressWarnings("unchecked")
	static public INamespaceMap<String, String> getNamespaceMap(EObject eObject) {
	
		if (eObject == null) {
			throw new NullPointerException(
					"eObject cannot be null in getNamespaceMap()"); //$NON-NLS-1$
		}
	
		INamespaceMap<String, String> nsMap = null;
		// Bug 120110 - this eObject may not have a namespace map, but its
		// ancestors might, so keep searching until we find one or until
		// we run out of ancestors.
		while (nsMap==null && eObject!=null) {
			nsMap = AdapterRegistry.INSTANCE.adapt(
				eObject, INamespaceMap.class);
			if (nsMap==null)
				eObject = eObject.eContainer();
		}
		
		if (nsMap == null) {
			throw new IllegalStateException(
					"INamespaceMap cannot be attached to an eObject"); //$NON-NLS-1$
		}
	
		return nsMap;
	}

	public static String getNamespacePrefix(EObject eObject, String namespace) {
	
		for (EObject context = eObject; context != null; context = context
				.eContainer()) {
			List<String> pfxList = getNamespaceMap(context).getReverse(
					namespace);
			if (pfxList.size() > 0) {
				return pfxList.get(0);
			}
		}
		return null;
	}
	
	///////
	
	public static List<String> getAllPrefixesForNamespace(Resource resource, String namespace) {
		List<String> result = new ArrayList<String>();
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			for (Entry<String, String> e : map.entrySet()) {
				String value = e.getValue();
				if (value!=null && value.equals(namespace))
					result.add(e.getKey());
			}
		}
		return result;
	}
	
	
	public static String getNamespaceForObject(Object object) {
		String namespace = null;
		// find the root container for this EObject
		if (object instanceof EObject) {
			EObject root = (EObject)object;
			while ((namespace==null ||namespace.isEmpty()) && root!=null) {
				if (root instanceof Definition) {
					namespace = ((Definition)root).getTargetNamespace();
				}
				else if (root instanceof XSDNamedComponent) {
					namespace = ((XSDNamedComponent)root).getTargetNamespace();
				}
				else if (root instanceof Definitions) {
					namespace = ((Definitions)root).getTargetNamespace();
				}
				else if (root instanceof Message) {
					namespace = ((Message)root).getQName().getNamespaceURI();
				}
				root = root.eContainer();
			}
		}
		else if (object instanceof IType) {
			namespace = "http://www.java.com/java"; //$NON-NLS-1$
		}
		return (namespace == null || namespace.isEmpty()) ? null : namespace;
	}
	
	public static String getQualifier(Object object) {
		String qualifier = null;
		// find the root container for this EObject
		if (object instanceof EObject) {
			EObject root = (EObject)object;
			while (qualifier==null && root!=null) {
				if (root instanceof Definition) {
					qualifier = "wsdl"; //$NON-NLS-1$
				}
				else if (root instanceof XSDSchema) {
					qualifier = "xsd"; //$NON-NLS-1$
				}
				else if (root instanceof Definitions) {
					qualifier = "bpmn2"; //$NON-NLS-1$
				}
				root = root.eContainer();
			}
		}
		else if (object instanceof IType) {
			qualifier = "java"; //$NON-NLS-1$
		}
		return qualifier;
	}
	
	public static String getPrefixForObject(Resource resource, Object object) {
		String prefix = ""; //$NON-NLS-1$
		String namespace = getNamespaceForObject(object);
		String qualifier = getQualifier(object);
		if (namespace!=null) {
			for (String s : getAllPrefixesForNamespace(resource, namespace)) {
				if (s.endsWith("."+qualifier)) //$NON-NLS-1$
					return s;
			}
		}
		Map<String,String> map = getXMLNSPrefixMap(resource);
		if (map!=null) {
			prefix = createUniquePrefix(map, "ns"); //$NON-NLS-1$
		}
		if (prefix!=null) {
			return prefix + "." + qualifier; //$NON-NLS-1$
		}
		return prefix;
	}
	
	public static String addNamespaceForObject(Resource resource, Object object) {
		String namespace = getNamespaceForObject(object);
		String prefix = getPrefixForObject(resource, object);
		if (namespace==null || hasPrefix(resource,prefix))
			return null;
		
		// generate a prefix
		addNamespace(resource, prefix, namespace);
		return namespace;
	}
}
