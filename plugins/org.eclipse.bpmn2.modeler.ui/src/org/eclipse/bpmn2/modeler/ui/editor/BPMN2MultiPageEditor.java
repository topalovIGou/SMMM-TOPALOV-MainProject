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

package org.eclipse.bpmn2.modeler.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.modeler.core.adapters.ExtendedPropertiesProvider;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.Bpmn2DiagramEditorInput;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;

/**
 * This class implements a multi-page version of the BPMN2 Modeler (BPMN2Editor class).
 * To revert back to the original, single-page version simply change the editor extension
 * point in plugin.xml (see comments there).
 * 
 * This is still in the experimental phase and currently only supports a single diagram
 * per .bpmn file. An optional second page, which displays the XML source, can be created
 * from the context menu. The source view is not yet synchronized to the design view and
 * can only show the XML as of the last "Save" i.e. the current state of the file on disk,
 * not the in-memory model. Design/Source view synchronization will be implemented in a
 * future version, but direct editing of the XML will not be supported - it will remain
 * "view only".
 * 
 * Future versions will support multiple diagrams per .bpmn file with the ability to add
 * and remove pages containing different diagram types. It should be possible for the user
 * to create a single file that contains a mix of Process, Collaboration and Choreography
 * diagrams. Whether or not these types of files are actually deployable and/or executable
 * is another story ;)
 */
public class BPMN2MultiPageEditor extends MultiPageEditorPart implements IGotoMarker {

	DesignEditor designEditor;
	SourceViewer sourceViewer;
	private CTabFolder tabFolder;
	private int defaultTabHeight;
	private List<BPMNDiagram> bpmnDiagrams = new ArrayList<BPMNDiagram>();
	private List<PictogramElement[]> currentSelections = new ArrayList<PictogramElement[]>();
	private int currentPageIndex = -1;
	
	public BPMN2MultiPageEditor() {
		super();
	}

	@Override
	protected IEditorSite createSite(IEditorPart editor) {
		if (editor instanceof DesignEditor)
			return new DesignEditorSite(this, editor);
		return new MultiPageEditorSite(this, editor);
	}

	@Override
	public String getTitle() {
		if (designEditor!=null)
			return designEditor.getTitle();
		return super.getTitle();
	}

	@Override
	public String getPartName() {
		if (designEditor!=null)
			return designEditor.getPartName();
		return super.getPartName();
	}

	@Override
    public void setInput(IEditorInput input) {
    	super.setInput(input);
    	Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
		    	setTitleToolTip( getTitleToolTip() );
			}
    	});
    }
	
	@Override
	public Object getAdapter(Class required) {
		if (required==DiagramEditor.class) {
			return designEditor;
		}
		return super.getAdapter(required);
	}
	
    /**
     * Method declared on IEditorPart.
     * 
     * @param marker Marker to look for
     */
    @Override
    public void gotoMarker(IMarker marker) {
        if (getActivePage() < 0) {
            setActivePage(0);
        }
        IDE.gotoMarker(getEditor(getActivePage()), marker);
    }
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		tabFolder = (CTabFolder)getContainer();
		tabFolder.addCTabFolder2Listener( new CTabFolder2Listener() {

			@Override
			public void close(CTabFolderEvent event) {
				if (event.item.getData() == sourceViewer)
					removeSourceViewer();
			}

			@Override
			public void minimize(CTabFolderEvent event) {
			}

			@Override
			public void maximize(CTabFolderEvent event) {
			}

			@Override
			public void restore(CTabFolderEvent event) {
			}

			@Override
			public void showList(CTabFolderEvent event) {
			}
			
		});
		tabFolder.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int pageIndex = tabFolder.getSelectionIndex();
				if (pageIndex>=0 && pageIndex<bpmnDiagrams.size() && designEditor!=null) {
					BPMNDiagram bpmnDiagram = bpmnDiagrams.get(pageIndex);
					designEditor.selectBpmnDiagram(bpmnDiagram);
				}
			}
		});
		
		// defer editor layout until all pages have been created
		tabFolder.setLayoutDeferred(true);
		
		createDesignEditor();
		
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				setActivePage(0);
				designEditor.selectBpmnDiagram(bpmnDiagrams.get(0));
				tabFolder.setLayoutDeferred(false);
				tabFolder.setTabPosition(SWT.TOP);
				updateTabs();
			}
		});
	}

	protected void createDesignEditor() {
		if (designEditor==null) {
			designEditor = new DesignEditor(this, this);
			
			try {
				int pageIndex = tabFolder.getItemCount();
				if (sourceViewer!=null)
					--pageIndex;
				addPage(pageIndex, designEditor, BPMN2MultiPageEditor.this.getEditorInput());
				defaultTabHeight = tabFolder.getTabHeight();
				setPageText(pageIndex,ExtendedPropertiesProvider.getTextValue( designEditor.getBpmnDiagram() ));

				defaultTabHeight = tabFolder.getTabHeight();

				updateTabs();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public DesignEditor getDesignEditor() {
		return designEditor;
	}
	
	protected void addDesignPage(final BPMNDiagram bpmnDiagram) {
		createDesignEditor();
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
		
					int pageIndex = tabFolder.getItemCount();
					if (sourceViewer!=null)
						--pageIndex;
					Bpmn2DiagramEditorInput input = (Bpmn2DiagramEditorInput)designEditor.getEditorInput();
					input.setBpmnDiagram(bpmnDiagram);
					addPage(pageIndex, designEditor, input);
					CTabItem oldItem = tabFolder.getItem(pageIndex-1);
					CTabItem newItem = tabFolder.getItem(pageIndex);
					newItem.setControl( oldItem.getControl() );
					BaseElement bpmnElement = bpmnDiagram.getPlane().getBpmnElement();
					String name = ModelUtil.getName(bpmnElement);
					if (name==null)
						name = ExtendedPropertiesProvider.getTextValue(bpmnDiagram);
					setPageText(pageIndex,name);
		
					setActivePage(pageIndex);
					updateTabs();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void showDesignPage(final BPMNDiagram bpmnDiagram) {
		final int pageIndex = bpmnDiagrams.indexOf(bpmnDiagram);
		if (pageIndex>=0) {
			if (getDesignEditor().getBpmnDiagram()!=bpmnDiagram) {
				setActivePage(pageIndex);
			}
		}
		else {
			designEditor.showDesignPage(bpmnDiagram);
		}
	}
	
	protected void removeDesignPage(final BPMNDiagram bpmnDiagram) {
		final int pageIndex = bpmnDiagrams.indexOf(bpmnDiagram);
		if (pageIndex>0) {
			// go back to "Design" page - the only page that can't be removed
			Display.getCurrent().asyncExec( new Runnable() {
				@Override
				public void run() {
					setActivePage(0);
					
					IEditorPart editor = getEditor(pageIndex);
					if (editor instanceof DesignEditor) {
						((DesignEditor)editor).deleteBpmnDiagram(bpmnDiagram);
					}
					
					// need to manage this ourselves so that the CTabFolder doesn't
					// dispose our editor site (a child of the CTabItem.control)
					tabFolder.getItem(pageIndex).setControl(null);
					
					removePage(pageIndex);
					
					tabFolder.getSelection().getControl().setVisible(true);
				}
			});
		}
	}

	public int getDesignPageCount() {
		int count = getPageCount();
		if (sourceViewer!=null)
			--count;
		return count;
	}

	protected void createSourceViewer() {
		if (sourceViewer==null) {
			sourceViewer = new SourceViewer(this);

			try {
				int pageIndex = tabFolder.getItemCount();
				FileEditorInput input = new FileEditorInput(designEditor.getModelFile());
				addPage(pageIndex, sourceViewer, input);
				tabFolder.getItem(pageIndex).setShowClose(true);
				
				setPageText(pageIndex,Messages.BPMN2MultiPageEditor_Source_Tab);
				setActivePage(pageIndex);

				updateTabs();
			}
			catch (Exception e) {
				e.printStackTrace();
				if (sourceViewer!=null)
					sourceViewer.dispose();
			}
		}
	}
	
	public SourceViewer getSourceViewer() {
		return sourceViewer;
	}

	public void removeSourceViewer() {
		// there will only be one source page and it will always be the last page in the tab folder
		if (sourceViewer!=null) {
			int pageIndex = tabFolder.getItemCount() - 1;
			if (pageIndex>0) {
				removePage(pageIndex);
				sourceViewer = null;
			}
		}
	}

	public void addPage(int pageIndex, IEditorPart editor, IEditorInput input)
			throws PartInitException {
		super.addPage(pageIndex,editor,input);
		if (editor instanceof DesignEditor) {
			bpmnDiagrams.add(pageIndex,((DesignEditor)editor).getBpmnDiagram());
			currentSelections.add(new PictogramElement[]{});
		}
	}
	
	@Override
	public void removePage(int pageIndex) {
		Object page = tabFolder.getItem(pageIndex).getData();
		super.removePage(pageIndex);
		updateTabs();
		if (page instanceof DesignEditor) {
			bpmnDiagrams.remove(pageIndex);
			currentSelections.remove(pageIndex);
		}
	}

	@Override
	protected void pageChange(final int newPageIndex) {
		if (currentPageIndex>=0 && currentPageIndex<tabFolder.getItemCount()) {
			IEditorPart editor = getEditor(currentPageIndex);
			if (editor instanceof DesignEditor) {
				final DesignEditor de = (DesignEditor) editor;
				PictogramElement selections[] = de.getSelectedPictogramElements();
				currentSelections.set(currentPageIndex, selections);
			}
		}
		currentPageIndex = newPageIndex;
		
		super.pageChange(newPageIndex);

		IEditorPart editor = getEditor(newPageIndex);
		if (editor instanceof DesignEditor) {
			final DesignEditor de = (DesignEditor) editor;
			BPMNDiagram bpmnDiagram = bpmnDiagrams.get(newPageIndex);
			final PictogramElement selections[] = currentSelections.get(newPageIndex);
			de.pageChange(bpmnDiagram);
			
			if (selections!=null) {
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						de.selectPictogramElements(selections);
					}
				});
			}
		}
	}

	public int getPageCount() {
		return tabFolder.getItemCount();
	}
	
	public CTabItem getTabItem(int pageIndex) {
		return tabFolder.getItem(pageIndex);
	}
	
	public BPMNDiagram getBpmnDiagram(int i) {
		if (i>=0 && i<bpmnDiagrams.size()) {
			return bpmnDiagrams.get(i);
		}
		return null;
	}
	
	private void updateTabs() {
		if (!tabFolder.getLayoutDeferred()) {
			if (tabFolder.getItemCount()==1) {
				tabFolder.setTabHeight(0);
			}
			else
				tabFolder.setTabHeight(defaultTabHeight);
		}
		tabFolder.layout();
	}
	
	public CTabFolder getTabFolder() {
		return tabFolder;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		designEditor.doSave(monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		IEditorPart activeEditor = getActiveEditor();
		activeEditor.doSaveAs();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		
		/* 
		 * Depending upon the active page in multipage editor, call the saveAsAllowed. 
		 * It helps to see whether a particular editor allows 'save as' feature 
		 */
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor!=null)
			return activeEditor.isSaveAsAllowed();
		return false;
	}

	@Override
	public void dispose() {
		designEditor.dispose();
		if (sourceViewer!=null)
			sourceViewer.dispose();
	}
	
	@Override
	public IEditorPart getActiveEditor() {
		return super.getActiveEditor();
	}
}
