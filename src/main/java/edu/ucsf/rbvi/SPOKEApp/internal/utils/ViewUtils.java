package edu.ucsf.rbvi.spokeApp.internal.utils;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;

public class ViewUtils {
	public static String STYLE_NAME_SIMPLE = "SPOKE";
	public static String STYLE_NAME = "SPOKE style";
	public static String STYLE_NAME_NAMESPACES = "SPOKE style v1.5";

	public static CyNetworkView styleNetwork(SpokeManager manager, CyNetwork network,
	                                         CyNetworkView netView) {
		VisualStyle spokeStyle = createStyle(manager, network);

		updateColorMap(manager, spokeStyle, network);
		updateNodeStyle(manager, netView, network.getNodeList());
		
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		vmm.setCurrentVisualStyle(spokeStyle);

		if (netView != null) {
			vmm.setVisualStyle(spokeStyle, netView);
			manager.getService(CyNetworkViewManager.class).addNetworkView(netView);
			manager.getService(CyApplicationManager.class).setCurrentNetworkView(netView);
			netView.updateView();
		}
		
		return netView;
	}

	public static void reapplyStyle(SpokeManager manager, CyNetworkView view) {
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		style.apply(view);
	}

	public static void updateNodeStyle(SpokeManager manager, 
	                                   CyNetworkView view, List<CyNode> nodes) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		for (CyNode node: nodes) {
			if (view.getNodeView(node) != null)
				style.apply(view.getModel().getRow(node), view.getNodeView(node));
		}
		// style.apply(view);
	}

	public static void updateEdgeStyle(SpokeManager manager, CyNetworkView view, List<CyEdge> edges) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = vmm.getVisualStyle(view);
		for (CyEdge edge: edges) {
			if (view.getEdgeView(edge) != null)
			style.apply(view.getModel().getRow(edge), view.getEdgeView(edge));
		}
		// style.apply(view);
	}

	public static VisualStyle createStyle(SpokeManager manager, CyNetwork network) {
		String styleName = getStyleName(manager, network);

		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		for (VisualStyle style: vmm.getAllVisualStyles()) {
			if (style.getTitle().equals(styleName)) {
				return style;
			}
		}

		VisualStyleFactory vsf = manager.getService(VisualStyleFactory.class);

		VisualStyle spokeStyle = vsf.createVisualStyle(vmm.getCurrentVisualStyle());
		spokeStyle.setTitle(styleName);

		// Set the default node size
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_WIDTH, 45.0);
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_HEIGHT, 45.0);

		// Set the shape to an ellipse
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);

		// And set the color to white
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.LIGHT_GRAY);

		// And set the edge color to blue
		spokeStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new Color(31,41,61));

		// And set the label color to black
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);

		// And set the label size to 12
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 12);

		// And set the node border width to zero
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 4.0);

		// And set the node border color to black
		spokeStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.BLACK);

		// Lock node width and height
		for(VisualPropertyDependency<?> vpd: spokeStyle.getAllVisualPropertyDependencies()) {
			if (vpd.getIdString().equals("nodeSizeLocked"))
				vpd.setDependency(false);
		}

		// Get all of the factories we'll need
		VisualMappingFunctionFactory discreteFactory = 
	                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughFactory = 
		                 manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();

		// Set up the passthrough mapping for the glass style

		// Set colors for edges based on the edge type
		//{
		//	DiscreteMapping<String, Color> dMapping = 
		//		(DiscreteMapping) discreteFactory.createVisualMappingFunction(CyEdge.INTERACTION, String.class,
		//														BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
		//	dMapping.putMapValue("pp", new Color(31,41,61));
		//	dMapping.putMapValue("ppp", new Color(170, 41, 74));
		//	spokeStyle.addVisualMappingFunction(dMapping);
		//}

		// Set up a passthrough for chemViz
		{
			VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
			PassthroughMapping pMapping = 
				(PassthroughMapping) passthroughFactory.createVisualMappingFunction(ModelUtils.CV_STYLE, 
																																						String.class, customGraphics);
			spokeStyle.addVisualMappingFunction(pMapping);
		}

			// Set colors for nodes based on the node type
			//{
			//	DiscreteMapping<String, Color> dMapping = 
			//		(DiscreteMapping) discreteFactory.createVisualMappingFunction(CyEdge.INTERACTION, String.class,
			//														BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
			//	dMapping.putMapValue("pp", new Color(31,41,61));
			//	dMapping.putMapValue("pc", new Color(31,41,61));
			//	dMapping.putMapValue("cc", new Color(31,41,61));
			//	dMapping.putMapValue("ppp", new Color(170, 41, 74));
			//	dMapping.putMapValue("ppc", new Color(170, 41, 74));
			//	spokeStyle.addVisualMappingFunction(dMapping);
			//}

		vmm.addVisualStyle(spokeStyle);
		return spokeStyle;
	}

	public static void updateChemVizPassthrough(SpokeManager manager, CyNetworkView view) {
		VisualStyle spokeStyle = getStyle(manager, view);

		VisualMappingFunctionFactory passthroughFactory = 
            manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();

		VisualProperty customGraphics = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
		PassthroughMapping pMapping = 
			(PassthroughMapping) passthroughFactory.createVisualMappingFunction(ModelUtils.CV_STYLE, 
																																					String.class, customGraphics);
		spokeStyle.addVisualMappingFunction(pMapping);
	}

	private static void updateColorMap(SpokeManager manager, VisualStyle style, CyNetwork network) {
		// Build the color list
		DiscreteMapping<String,Color> dMapping = getStringNodeColorMapping(manager, network);		
		style.addVisualMappingFunction(dMapping);
	}
	
	private static DiscreteMapping<String, Color> getStringNodeColorMapping(SpokeManager manager,
			CyNetwork network) {
		VisualMappingFunctionFactory discreteFactory = manager
				.getService(VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		if(discreteFactory == null) {
			return null;
		}
		
		DiscreteMapping<String, Color> dMapping = (DiscreteMapping) discreteFactory
				.createVisualMappingFunction(ModelUtils.TYPE, String.class,
						BasicVisualLexicon.NODE_FILL_COLOR);

		List<String> nodeTypes = manager.getNodeTypes();
		Collections.sort(nodeTypes);
		for (String type: nodeTypes) {
			Color c = manager.getNodeFillColor(type);
			dMapping.putMapValue(type, c);
		}
		
		return dMapping;
	}
	
	private static <K, V> boolean sameVisualMappingFunction(CyNetwork network,
			VisualMappingFunction<K, V> vmf, DiscreteMapping<String, Color> stringMapping) {
		if(!(vmf instanceof DiscreteMapping<?, ?>)) {
			return false;
		}
		
		if(!vmf.getMappingColumnName().equals(stringMapping.getMappingColumnName())) {
			return false;
		}
		
		if(!vmf.getMappingColumnType().equals(stringMapping.getMappingColumnType())) {
			return false;
		}
		
		for(CyNode node : network.getNodeList()) {
			V vmfMappedValue = vmf.getMappedValue(network.getRow(node));
			Color stringMappedValue = stringMapping.getMappedValue(network.getRow(node));
			
			if(vmfMappedValue == null && stringMappedValue != null) {
				return false;
			} else if(vmfMappedValue != null && !vmfMappedValue.equals(stringMappedValue)) {
				return false;
			}
		}
		
		return true;
	}

	public static Color getRandomColor() {
		Random rand = new Random();
		float r = rand.nextFloat();
		float g = rand.nextFloat();
		float b = rand.nextFloat();
		return new Color(r, g, b);
	}
	
	public static void updateNodeColors(SpokeManager manager, CyNetwork net, CyNetworkView view,
			List<String> speciesList) {
		// manager.flushEvents();
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);

		VisualStyle style = null;
	 	if (view != null)
			style	= vmm.getVisualStyle(view);
		else {
			String styleName = getStyleName(manager, net);
			for (VisualStyle s: vmm.getAllVisualStyles()) {
				if (s.getTitle().equals(styleName)) {
					style = s;
					break;
				}
			}
		}

		// Worst case -- can't find a style, so er just bail
		if (style == null)
			return;

		// TODO: [N] Is this the right way to do it? 
		// Why do we need this?
		if (!style.getTitle().startsWith(STYLE_NAME_SIMPLE)) {
			VisualStyleFactory vsf = manager.getService(VisualStyleFactory.class);

			VisualStyle spokeStyle = vsf.createVisualStyle(vmm.getCurrentVisualStyle());
			spokeStyle.setTitle(STYLE_NAME_SIMPLE + style.getTitle());
			vmm.addVisualStyle(spokeStyle);
			style = spokeStyle;
		}

		if (view != null) {
			vmm.setVisualStyle(style, view);
			style.apply(view);
		}
		vmm.setCurrentVisualStyle(style);
	}

	public static void updatePieCharts(SpokeManager manager, VisualStyle spokeStyle,
			CyNetwork net, boolean show) {

		VisualMappingFunctionFactory passthroughFactory = manager
				.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualLexicon lex = manager.getService(RenderingEngineManager.class)
				.getDefaultVisualLexicon();
	}
	
	public static void highlight(SpokeManager manager, CyNetworkView view, List<CyNode> nodes) {
		CyNetwork net = view.getModel();

		List<CyEdge> edgeList = new ArrayList<CyEdge>();
		List<CyNode> nodeList = new ArrayList<CyNode>();
	 	for (CyNode node: nodes) {
			edgeList.addAll(net.getAdjacentEdgeList(node, CyEdge.Type.ANY));
			nodeList.addAll(net.getNeighborList(node, CyEdge.Type.ANY));
		}


		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();
		VisualProperty customGraphics1 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		VisualProperty customGraphics2 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
		VisualProperty customGraphics3 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3");

		CyCustomGraphics cg = new EmptyCustomGraphics();

		// Override our current style through overrides
		for (View<CyNode> nv: view.getNodeViews()) {
			if (nodeList.contains(nv.getModel()) || nodes.contains(nv.getModel())) {
				nv.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 255);
			} else {
				nv.setLockedValue(customGraphics1, cg);
				nv.setLockedValue(customGraphics2, cg);
				nv.setLockedValue(customGraphics3, cg);
				nv.setLockedValue(BasicVisualLexicon.NODE_TRANSPARENCY, 20);
			}
		}
		for (View<CyEdge> ev: view.getEdgeViews()) {
			if (edgeList.contains(ev.getModel())) {
				ev.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 255);
			} else {
				ev.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 20);
			}
		}
	}

	public static void clearHighlight(SpokeManager manager, CyNetworkView view) {
		// if (node == null) return;
		// View<CyNode> nodeView = view.getNodeView(node);
		if (view == null) return;

		VisualLexicon lex = manager.getService(RenderingEngineManager.class).getDefaultVisualLexicon();
		VisualProperty customGraphics1 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_1");
		VisualProperty customGraphics2 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_2");
		VisualProperty customGraphics3 = lex.lookup(CyNode.class, "NODE_CUSTOMGRAPHICS_3");

		for (View<CyNode> nv: view.getNodeViews()) {
			nv.clearValueLock(customGraphics1);
			nv.clearValueLock(customGraphics2);
			nv.clearValueLock(customGraphics3);
			nv.clearValueLock(BasicVisualLexicon.NODE_TRANSPARENCY);
		}

		for (View<CyEdge> ev: view.getEdgeViews()) {
			ev.clearValueLock(BasicVisualLexicon.EDGE_TRANSPARENCY);
		}
	}

	public static void hideSingletons(CyNetworkView view, boolean show) {
		CyNetwork net = view.getModel();
		for (View<CyNode> nv: view.getNodeViews()) {
			CyNode node = nv.getModel();
			List<CyEdge> edges = net.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			if (edges != null && edges.size() > 0) continue;
			if (!show)
				nv.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
			else
				nv.clearValueLock(BasicVisualLexicon.NODE_VISIBLE);
		}
	}

	public static VisualStyle getStyle(SpokeManager manager, CyNetworkView view) {
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		VisualStyle style = null;
	 	if (view != null)
			style	= vmm.getVisualStyle(view);
		else {
			String styleName = getStyleName(manager, view.getModel());
			for (VisualStyle s: vmm.getAllVisualStyles()) {
				if (s.getTitle().equals(styleName)) {
					style = s;
					break;
				}
			}
		}

		return style;
	}

	private static String getStyleName(SpokeManager manager, CyNetwork network) {
		String networkName = manager.getNetworkName(network);
		String styleName = STYLE_NAME_SIMPLE;
		if (networkName.startsWith("SPOKE Network")) {
			String[] parts = networkName.split("_");
			if (parts.length == 1) {
				String[] parts2 = networkName.split(" - ");
				if (parts2.length == 2)
					styleName = styleName +" - "+parts2[1];
			} else if (parts.length == 2)
				styleName = styleName + "_"+parts[1];
		}
		return styleName;
	}
}

