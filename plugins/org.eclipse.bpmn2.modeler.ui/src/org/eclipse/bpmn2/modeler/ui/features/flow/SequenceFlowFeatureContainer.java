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
 * @author Innar Made
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.features.flow;

import java.util.Iterator;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.ComplexGateway;
import org.eclipse.bpmn2.ExclusiveGateway;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GatewayDirection;
import org.eclipse.bpmn2.InclusiveGateway;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.modeler.core.Activator;
import org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2UpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.BaseElementConnectionFeatureContainer;
import org.eclipse.bpmn2.modeler.core.features.DefaultLayoutBPMNConnectionFeature;
import org.eclipse.bpmn2.modeler.core.features.MultiUpdateFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractAddFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractCreateFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractReconnectFlowFeature;
import org.eclipse.bpmn2.modeler.core.features.label.UpdateLabelFeature;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.bpmn2.modeler.ui.ImageProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.IReconnectionContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.ReconnectionContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.graphiti.util.IColorConstant;

public class SequenceFlowFeatureContainer extends BaseElementConnectionFeatureContainer {

	private static final String IS_DEFAULT_FLOW_PROPERTY = "is.default.flow"; //$NON-NLS-1$
	private static final String IS_CONDITIONAL_FLOW_PROPERTY = "is.conditional.flow"; //$NON-NLS-1$
	private static final String DEFAULT_MARKER_PROPERTY = "default.marker"; //$NON-NLS-1$
	private static final String CONDITIONAL_MARKER_PROPERTY = "conditional.marker"; //$NON-NLS-1$

	@Override
	public boolean canApplyTo(Object o) {
		return super.canApplyTo(o) && o instanceof SequenceFlow;
	}

	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddSequenceFlowFeature(fp);
	}

	@Override
	public ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider fp) {
		return new CreateSequenceFlowFeature(fp);
	}

	@Override
	public ILayoutFeature getLayoutFeature(IFeatureProvider fp) {
		return new DefaultLayoutBPMNConnectionFeature(fp);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		MultiUpdateFeature multiUpdate = new MultiUpdateFeature(fp);
		multiUpdate.addFeature(new UpdateDefaultSequenceFlowFeature(fp));
		multiUpdate.addFeature(new UpdateConditionalSequenceFlowFeature(fp));
		multiUpdate.addFeature(new UpdateLabelFeature(fp));
		return multiUpdate;
	}

	@Override
	public IReconnectionFeature getReconnectionFeature(IFeatureProvider fp) {
		return new ReconnectSequenceFlowFeature(fp);
	}

	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return null;
	}

	public static class AddSequenceFlowFeature extends AbstractAddFlowFeature<SequenceFlow> {
		public AddSequenceFlowFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		protected Polyline createConnectionLine(Connection connection) {
			IPeService peService = Graphiti.getPeService();
			IGaService gaService = Graphiti.getGaService();
			BaseElement be = BusinessObjectUtil.getFirstBaseElement(connection);

			Polyline connectionLine = super.createConnectionLine(connection);
			connectionLine.setLineStyle(LineStyle.SOLID);
			connectionLine.setLineWidth(2);

			int w = 5;
			int l = 15;
			
			ConnectionDecorator decorator = peService.createConnectionDecorator(connection, false,
					1.0, true);

			Polyline arrowhead = gaService.createPolygon(decorator, new int[] { -l, w, 0, 0, -l, -w, -l, w });
			StyleUtil.applyStyle(arrowhead, be);
			
			return connectionLine;
		}

		@Override
		protected void decorateConnection(IAddConnectionContext context, Connection connection, SequenceFlow businessObject) {
			setDefaultSequenceFlow(connection);
			setConditionalSequenceFlow(connection);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2AddFeature#getBusinessObjectType()
		 */
		@Override
		public Class getBusinessObjectType() {
			return SequenceFlow.class;
		}
	}

	public static class CreateSequenceFlowFeature extends AbstractCreateFlowFeature<SequenceFlow, FlowNode, FlowNode> {

		public CreateSequenceFlowFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		protected String getStencilImageId() {
			return ImageProvider.IMG_16_SEQUENCE_FLOW;
		}

		@Override
		protected Class<FlowNode> getSourceClass() {
			return FlowNode.class;
		}

		@Override
		protected Class<FlowNode> getTargetClass() {
			return FlowNode.class;
		}

		@Override
		public boolean canCreate(ICreateConnectionContext context) {
			if (!super.canCreate(context))
				return false;
			
			FlowNode source = getSourceBo(context);
			FlowNode target = getTargetBo(context);
			if (source==null || target==null)
				return false;
			if (source==target)
				return true;
			
			if (source instanceof SubProcess) {
				if (((SubProcess)source).isTriggeredByEvent())
					return false;
			}
			
			if (target instanceof SubProcess) {
				if (((SubProcess)target).isTriggeredByEvent())
					return false;
			}
			
			if (target instanceof StartEvent)
				return false;
			
			EObject sourceContainer = source.eContainer();
			while (sourceContainer!=null) {
				if (sourceContainer instanceof Process || sourceContainer instanceof SubProcess)
					break;
				sourceContainer = sourceContainer.eContainer();
			}
			EObject targetContainer = target.eContainer();
			while (targetContainer!=null) {
				if (targetContainer instanceof Process || targetContainer instanceof SubProcess)
					break;
				targetContainer = targetContainer.eContainer();
			}
			return sourceContainer==targetContainer;
		}
		
		@Override
		public boolean isAvailable(IContext context) {
			if (context instanceof ICreateConnectionContext) {
				ICreateConnectionContext ccc = (ICreateConnectionContext) context;
				FlowNode source = getSourceBo(ccc);
				if (source instanceof SubProcess) {
					SubProcess subProcess = (SubProcess) source;
					if (subProcess.isTriggeredByEvent())
						return false;
				}
			}
			return super.isAvailable(context);
		}

		@Override
		public Connection create(ICreateConnectionContext context) {
			Connection connection = super.create(context);
			FlowNode source = getSourceBo(context);
			FlowNode target = getTargetBo(context);
			if (source instanceof Gateway) {
				// update the gateway direction
				Gateway gw = (Gateway) source;
				if (gw.getOutgoing().size()>1) {
					if (gw.getIncoming().size()>1) {
						gw.eUnset(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
					}
					else {
						// must be a Diverging gateway
						gw.setGatewayDirection(GatewayDirection.DIVERGING);
					}
				}
			}
			if (target instanceof Gateway) {
				// update the gateway direction
				Gateway gw = (Gateway) target;
				if (gw.getIncoming().size()>1) {
					if (gw.getOutgoing().size()>1) {
						gw.eUnset(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
					}
					else {
						// must be a Converging gateway
						gw.setGatewayDirection(GatewayDirection.CONVERGING);
					}
				}
			}
			return connection;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.bpmn2.modeler.core.features.AbstractBpmn2CreateConnectionFeature#getBusinessObjectClass()
		 */
		@Override
		public EClass getBusinessObjectClass() {
			return Bpmn2Package.eINSTANCE.getSequenceFlow();
		}
	}

	private static Color manageColor(PictogramElement element, IColorConstant colorConstant) {
		IPeService peService = Graphiti.getPeService();
		Diagram diagram = peService.getDiagramForPictogramElement(element);
		return Graphiti.getGaService().manageColor(diagram, colorConstant);
	}
	
	private static void setDefaultSequenceFlow(Connection connection) {
		IPeService peService = Graphiti.getPeService();
		SequenceFlow flow = BusinessObjectUtil.getFirstElementOfType(connection, SequenceFlow.class);
		SequenceFlow defaultFlow = getDefaultFlow(flow.getSourceRef());
		boolean isDefault = defaultFlow == null ? false : defaultFlow.equals(flow);

		Tuple<ConnectionDecorator, ConnectionDecorator> decorators = getConnectionDecorators(connection);
		ConnectionDecorator def = decorators.getFirst();
		ConnectionDecorator cond = decorators.getSecond();

		if (isDefault) {
			if (cond != null) {
				peService.deletePictogramElement(cond);
			}
			def = createDefaultConnectionDecorator(connection);
			GraphicsAlgorithm ga = def.getGraphicsAlgorithm();
//			ga.setForeground(manageColor(connection,StyleUtil.CLASS_FOREGROUND));
		} else {
			if (def != null) {
				peService.deletePictogramElement(def);
			}
			if (flow.getConditionExpression() != null && flow.getSourceRef() instanceof Activity) {
				cond = createConditionalConnectionDecorator(connection);
				GraphicsAlgorithm ga = cond.getGraphicsAlgorithm();
				ga.setFilled(true);
//				ga.setForeground(manageColor(connection,StyleUtil.CLASS_FOREGROUND));
				ga.setBackground(manageColor(connection,IColorConstant.WHITE));
			}
		}

		FeatureSupport.setPropertyValue(connection, IS_DEFAULT_FLOW_PROPERTY,
				Boolean.toString(isDefault));

	}
	
	public static class UpdateDefaultSequenceFlowFeature extends AbstractBpmn2UpdateFeature {

		public UpdateDefaultSequenceFlowFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canUpdate(IUpdateContext context) {
			SequenceFlow flow = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
					SequenceFlow.class);
			boolean canUpdate = flow != null && isDefaultAttributeSupported(flow.getSourceRef());
			return canUpdate;
		}

		@Override
		public IReason updateNeeded(IUpdateContext context) {
			IPeService peService = Graphiti.getPeService();
			String property = FeatureSupport.getPropertyValue(context.getPictogramElement(), IS_DEFAULT_FLOW_PROPERTY);
			if (property == null || !canUpdate(context)) {
				return Reason.createFalseReason();
			}
			SequenceFlow flow = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
					SequenceFlow.class);
			SequenceFlow defaultFlow = getDefaultFlow(flow.getSourceRef());
			boolean isDefault = defaultFlow == null ? false : defaultFlow.equals(flow);
			boolean changed = isDefault != new Boolean(property);
			return changed ? Reason.createTrueReason(Messages.SequenceFlowFeatureContainer_Default_Changed) : Reason.createFalseReason();
		}

		@Override
		public boolean update(IUpdateContext context) {
			Connection connection = (Connection) context.getPictogramElement();
			setDefaultSequenceFlow(connection);
			return true;
		}
	}

	private static void setConditionalSequenceFlow(Connection connection) {
		IPeService peService = Graphiti.getPeService();
		SequenceFlow flow = BusinessObjectUtil.getFirstElementOfType(connection, SequenceFlow.class);

		Tuple<ConnectionDecorator, ConnectionDecorator> decorators = getConnectionDecorators(connection);
		ConnectionDecorator def = decorators.getFirst();
		ConnectionDecorator cond = decorators.getSecond();

		if (flow.getConditionExpression() != null && flow.getSourceRef() instanceof Activity && def == null) {
			ConnectionDecorator decorator = createConditionalConnectionDecorator(connection);
			GraphicsAlgorithm ga = decorator.getGraphicsAlgorithm();
			ga.setFilled(true);
//			ga.setForeground(manageColor(connection, StyleUtil.CLASS_FOREGROUND));
			ga.setBackground(manageColor(connection, IColorConstant.WHITE));
		} else if (cond != null) {
			peService.deletePictogramElement(cond);
		}

		FeatureSupport.setPropertyValue(connection, IS_CONDITIONAL_FLOW_PROPERTY,
				Boolean.toString(flow.getConditionExpression() != null));
	}
	
	public static class UpdateConditionalSequenceFlowFeature extends AbstractBpmn2UpdateFeature {

		public UpdateConditionalSequenceFlowFeature(IFeatureProvider fp) {
			super(fp);
		}

		@Override
		public boolean canUpdate(IUpdateContext context) {
			SequenceFlow flow = BusinessObjectUtil.getFirstElementOfType(context.getPictogramElement(),
					SequenceFlow.class);
			boolean canUpdate = flow != null && flow.getSourceRef() instanceof Activity;
			return canUpdate;
		}

		@Override
		public IReason updateNeeded(IUpdateContext context) {
			// NOTE: if this method returns "true" the very first time it's called by the refresh
			// framework, the connection line will be drawn as a red dotted line, so make sure
			// all properties have been correctly set to their initial values in the AddFeature!
			// see https://issues.jboss.org/browse/JBPM-3102
			IPeService peService = Graphiti.getPeService();
			PictogramElement pe = context.getPictogramElement();
			if (pe instanceof Connection) {
				Connection connection = (Connection) pe;
				SequenceFlow flow = BusinessObjectUtil.getFirstElementOfType(connection, SequenceFlow.class);
				String property = FeatureSupport.getPropertyValue(connection, IS_CONDITIONAL_FLOW_PROPERTY);
				if (property == null || !canUpdate(context)) {
					return Reason.createFalseReason();
				}
				boolean changed = flow.getConditionExpression() != null != new Boolean(property);
				return changed ? Reason.createTrueReason(Messages.SequenceFlowFeatureContainer_Conditional_Changed) : Reason.createFalseReason();
			}
			return Reason.createFalseReason();
		}

		@Override
		public boolean update(IUpdateContext context) {
			Connection connection = (Connection) context.getPictogramElement();
			setConditionalSequenceFlow(connection);
			return true;
		}
	}
	
	public static class ReconnectSequenceFlowFeature extends AbstractReconnectFlowFeature {

		public ReconnectSequenceFlowFeature(IFeatureProvider fp) {
			super(fp);
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean canReconnect(IReconnectionContext context) {
			if (!super.canReconnect(context))
				return false;
			if (context.getReconnectType().equals(ReconnectionContext.RECONNECT_TARGET)) {
				EObject o = BusinessObjectUtil.getBusinessObjectForPictogramElement(context.getTargetPictogramElement());
				if (o instanceof CatchEvent)
					return false;
			}
			return true;
		}
		
		@Override
		protected Class<? extends EObject> getTargetClass() {
			return FlowNode.class;
		}

		@Override
		protected Class<? extends EObject> getSourceClass() {
			return FlowNode.class;
		}

	}
	
	private static boolean isDefaultAttributeSupported(FlowNode node) {
		if (node instanceof Activity) {
			return true;
		}
		if (node instanceof ExclusiveGateway || node instanceof InclusiveGateway || node instanceof ComplexGateway) {
			return true;
		}
		return false;
	}

	private static SequenceFlow getDefaultFlow(FlowNode node) {
		if (isDefaultAttributeSupported(node)) {
			try {
				return (SequenceFlow) node.getClass().getMethod("getDefault").invoke(node); //$NON-NLS-1$
			} catch (Exception e) {
				Activator.logError(e);
			}
		}
		return null;
	}

	private static Tuple<ConnectionDecorator, ConnectionDecorator> getConnectionDecorators(Connection connection) {
		IPeService peService = Graphiti.getPeService();

		ConnectionDecorator defaultDecorator = null;
		ConnectionDecorator conditionalDecorator = null;

		Iterator<ConnectionDecorator> iterator = connection.getConnectionDecorators().iterator();
		while (iterator.hasNext()) {
			ConnectionDecorator connectionDecorator = iterator.next();
			String defProp = FeatureSupport.getPropertyValue(connectionDecorator, DEFAULT_MARKER_PROPERTY);
			if (defProp != null && new Boolean(defProp)) {
				defaultDecorator = connectionDecorator;
				continue;
			}
			String condProp = FeatureSupport.getPropertyValue(connectionDecorator, CONDITIONAL_MARKER_PROPERTY);
			if (condProp != null && new Boolean(condProp)) {
				conditionalDecorator = connectionDecorator;
			}
		}

		return new Tuple<ConnectionDecorator, ConnectionDecorator>(defaultDecorator, conditionalDecorator);
	}

	private static ConnectionDecorator createDefaultConnectionDecorator(Connection connection) {
		ConnectionDecorator marker = Graphiti.getPeService().createConnectionDecorator(connection, false, 0.0, true);
		Polyline line = Graphiti.getGaService().createPolyline(marker, new int[] { -6, 6, -12, -6 });
		line.setLineWidth(2);
		FeatureSupport.setPropertyValue(marker, DEFAULT_MARKER_PROPERTY, Boolean.toString(true));
		return marker;
	}

	private static ConnectionDecorator createConditionalConnectionDecorator(Connection connection) {
		ConnectionDecorator marker = Graphiti.getPeService().createConnectionDecorator(connection, false, 0.0, true);
		Graphiti.getGaService().createPolygon(marker, new int[] { -15, 0, -7, 5, 0, 0, -7, -5 });
		FeatureSupport.setPropertyValue(marker, CONDITIONAL_MARKER_PROPERTY, Boolean.toString(true));
		return marker;
	}
}