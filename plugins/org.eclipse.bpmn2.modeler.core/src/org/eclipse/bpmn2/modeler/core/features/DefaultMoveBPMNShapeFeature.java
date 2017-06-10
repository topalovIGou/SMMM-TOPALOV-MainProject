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
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Group;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent;
import org.eclipse.bpmn2.modeler.core.LifecycleEvent.EventType;
import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.runtime.TargetRuntime;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DiagramEditor;

/**
 * Default Graphiti {@code MoveShapeFeature} class for Shapes.
 * <p>
 */
public class DefaultMoveBPMNShapeFeature extends DefaultMoveShapeFeature {

	/** The shape's location before the move. */
	protected ILocation preMoveLoc;
	protected PictogramElement[] selectedShapes = null;
	
	/**
	 * Instantiates a new default MoveShapeFature.
	 *
	 * @param fp the Feature Provider
	 */
	public DefaultMoveBPMNShapeFeature(IFeatureProvider fp) {
		super(fp);
		selectedShapes = fp.getDiagramTypeProvider().getDiagramBehavior().getDiagramContainer().getSelectedPictogramElements();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature#canMoveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	public boolean canMoveShape(IMoveShapeContext context) {
		if (Graphiti.getPeService().getProperty(context.getShape(), RoutingNet.LANE)!=null) {
			return false;
		}
		if (FeatureSupport.isLabelShape(context.getPictogramElement()))
			return false;
		ContainerShape targetContainer = context.getTargetContainer();
		if (FeatureSupport.isLabelShape(targetContainer))
			return false; // can't move a shape into a label
		
		if (BusinessObjectUtil.getFirstBaseElement(targetContainer) instanceof Group) {
			// can't move ANYTHING into a Group
			return false;
		}
		if (Graphiti.getPeService().getProperty(targetContainer, RoutingNet.LANE)!=null) {
			int x = context.getX();
			int y = context.getY();
			ILocation loc = Graphiti.getPeService().getLocationRelativeToDiagram(targetContainer);
			((MoveShapeContext)context).setX(x + loc.getX());
			((MoveShapeContext)context).setY(y + loc.getY());
			((MoveShapeContext)context).setSourceContainer(targetContainer.getContainer());
			((MoveShapeContext)context).setTargetContainer(targetContainer.getContainer());
		}
		
		boolean doit = true;
//		context.getSourceContainer() != null && context.getSourceContainer().equals(context.getTargetContainer());
		TargetRuntime rt = TargetRuntime.getRuntime(context.getPictogramElement());
		LifecycleEvent event = new LifecycleEvent(EventType.PICTOGRAMELEMENT_CAN_MOVE,
				getFeatureProvider(), context, context.getPictogramElement(), rt);
		event.doit = doit;
		LifecycleEvent.notify(event);
		return event.doit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature#moveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public void moveShape(IMoveShapeContext context) {
		TargetRuntime rt = TargetRuntime.getRuntime(context.getPictogramElement());
		LifecycleEvent.notify(new LifecycleEvent(EventType.PICTOGRAMELEMENT_PRE_MOVE,
				getFeatureProvider(), context, context.getPictogramElement(), rt));
		
		super.moveShape(context);
		
		LifecycleEvent.notify(new LifecycleEvent(EventType.PICTOGRAMELEMENT_POST_MOVE,
				getFeatureProvider(), context, context.getPictogramElement(), rt));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature#preMoveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	protected void preMoveShape(IMoveShapeContext context) {
		preMoveLoc = Graphiti.getLayoutService().getLocationRelativeToDiagram(context.getShape());
		super.preMoveShape(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.DefaultMoveShapeFeature#postMoveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	protected void postMoveShape(IMoveShapeContext context) {
		Shape shape = context.getShape();
		Point p = null;
		
		ILocation postMoveLoc = Graphiti.getPeService().getLocationRelativeToDiagram(shape);
		int deltaX = postMoveLoc.getX()-preMoveLoc.getX();
		int deltaY = postMoveLoc.getY()-preMoveLoc.getY();
		/*
		 * If the shape being moved has a Label and the Label is
		 * {@link org.eclipse.bpmn2.modeler.core.preferences.ShapeStyle.LabelPosition.MOVABLE}
		 * then adjust the Label position by the move offset.
		 */
		if (!FeatureSupport.isLabelShape(shape)) {
			p = GraphicsUtil.createPoint(deltaX, deltaY);
			DIUtils.updateDIShape(shape);
			FeatureSupport.updateLabel(getFeatureProvider(), shape, p);
		}
		
		
		if (shape instanceof ContainerShape) {
			PictogramElement pe = (PictogramElement) ((ContainerShape)shape).eContainer();
			if (BusinessObjectUtil.containsElementOfType(pe, SubProcess.class)) {
				layoutPictogramElement(pe);
			}
		}

		// if multiple shapes are being moved, only update the connections
		// after all of them have been moved. This is to avoid relocating
		// connections unnecessarily as each shape is moved.
		if (shape==selectedShapes[ selectedShapes.length-1 ]) {
			List<AnchorContainer> movedShapes = new ArrayList<AnchorContainer>();
			for (int i=0; i<selectedShapes.length; ++i) {
				movedShapes.add((AnchorContainer)selectedShapes[i]);
			}
			FeatureSupport.updateConnections(getFeatureProvider(), movedShapes);
		}
		
		// Handle the case where a shape was moved such that it now collides
		// with an existing connection
		for (Connection connection : getDiagram().getConnections()) {
			if (!FeatureSupport.getConnections(shape).contains(connection) &&
					GraphicsUtil.intersects(shape, connection)) {
				FeatureSupport.updateConnection(getFeatureProvider(), connection);
			}
		}

		FeatureSupport.updateCategoryValues(getFeatureProvider(), shape);
		
		for (Anchor a : shape.getAnchors()) {
			for (Connection c : a.getIncomingConnections()) {
				FeatureSupport.updateCategoryValues(getFeatureProvider(), c);
			}
			for (Connection c : a.getOutgoingConnections()) {
				FeatureSupport.updateCategoryValues(getFeatureProvider(), c);
			}
		}
	}
	
	protected DiagramEditor getDiagramEditor() {
		return (DiagramEditor)getFeatureProvider().getDiagramTypeProvider().getDiagramBehavior().getDiagramContainer();
	}
}