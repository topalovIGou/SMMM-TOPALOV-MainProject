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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.launching.JavaRuntime;

public class JavaProjectClassLoader {
	private IJavaProject javaProject;
	private URLClassLoader classLoader = null;

	public JavaProjectClassLoader(IJavaProject javaProject) {
		if (javaProject == null || !javaProject.exists())
			throw new IllegalArgumentException("Invalid javaProject"); //$NON-NLS-1$
		this.javaProject = javaProject;
	}
	
	public JavaProjectClassLoader(IProject project) {
		if (project==null || !project.exists())
			throw new IllegalArgumentException("Invalid javaProject"); //$NON-NLS-1$
		javaProject = JavaCore.create(project);
	}
	
	public IType findClass(String className, IProject project) {
		try {
		    IJavaProject javaProject = JavaCore.create(project);
		    return javaProject.findType(className);
		} catch (Exception e) {
		}
		return null;
	}
	
	public List<IType> findClasses(String classNamePattern) {
		final List<IType> results = new ArrayList<IType>();
		if (classNamePattern.endsWith(".java")) { //$NON-NLS-1$
			classNamePattern = classNamePattern.substring(0,classNamePattern.lastIndexOf(".")); //$NON-NLS-1$
		}
		// find exact matches first
		findClasses(classNamePattern, results);
		return results;
	}
	
	public void findClasses(String classNamePattern, final List<IType> results) {
		SearchEngine searchEngine = new SearchEngine();
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope((IJavaElement[]) new IJavaProject[] {javaProject});
        char[] packageName = null;
        char[] typeName = null;
        int index = classNamePattern.lastIndexOf('.');
        int packageMatch = SearchPattern.R_EXACT_MATCH;
        int typeMatch = SearchPattern.R_PREFIX_MATCH;

        if (index == -1) {
            // There is no package qualification
            // Perform the search only on the type name
            typeName = classNamePattern.toCharArray();
        } else if ((index + 1) == classNamePattern.length()) {
            // There is a package qualification and the last character is a
            // dot
            // Perform the search for all types under the given package
            // Pattern for all types
            typeName = "".toCharArray(); //$NON-NLS-1$
            // Package name without the trailing dot
            packageName = classNamePattern.substring(0, index).toCharArray();
        } else {
            // There is a package qualification, followed by a dot, and 
            // a type fragment
            // Type name without the package qualification
            typeName = classNamePattern.substring(index + 1).toCharArray();
            // Package name without the trailing dot
            packageName = classNamePattern.substring(0, index).toCharArray();
        }

        try {
            TypeNameMatchRequestor req = new TypeNameMatchRequestor() {
                public void acceptTypeNameMatch(TypeNameMatch match) {
                    results.add(match.getType());
                }
            };
            // Note:  Do not use the search() method, its performance is
            // bad compared to the searchAllTypeNames() method
            searchEngine.searchAllTypeNames(packageName, packageMatch, typeName,
                    typeMatch, IJavaSearchConstants.CLASS_AND_INTERFACE, scope, req,
                    IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
        } catch (CoreException e) {
        }
	}
	
	public Class<?> loadClass(String name) {
		getClassLoader();
		if (classLoader != null) {
			try {
				return classLoader.loadClass(name);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	private URLClassLoader getClassLoader() {
		if (classLoader==null) {
			try {
				String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
				List<URL> urlList = new ArrayList<URL>();
				for (int i = 0; i < classPathEntries.length; i++) {
					String entry = classPathEntries[i];
					IPath path = new Path(entry);
					URL url = path.toFile().toURI().toURL();
					urlList.add(url);
				}
				ClassLoader parentClassLoader = JavaProjectClassLoader.class.getClassLoader();
				URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
				classLoader = new URLClassLoader(urls, parentClassLoader);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return classLoader;
	}
}