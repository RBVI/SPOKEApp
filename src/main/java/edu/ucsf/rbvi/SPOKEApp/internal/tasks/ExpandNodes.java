package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.command.util.NodeList;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.edit.EditNetworkTitleTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.spokeApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.spokeApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;
import edu.ucsf.rbvi.spokeApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.spokeApp.internal.utils.ViewUtils;

public class ExpandNodes extends AbstractTask {
	final SpokeManager manager;

	@Tunable(description="Network", context="nogui", gravity=1.0)
 	CyNetwork network = null;

	public NodeList nodeList = new NodeList(null);

 	@Tunable(description="Nodes to expand", context="nogui", gravity=2.0)
 	public NodeList getnodeList() {
		nodeList.setNetwork(network); // Should check for null
		return nodeList;
 	}

	public ExpandNodes(final SpokeManager manager) {
		this.manager = manager;
	}

	public void run(TaskMonitor monitor) {
		List<CyNode> nodes = null;
		if (network == null) network = manager.getCurrentNetwork();
		if (nodeList.getValue() == null || nodeList.getValue().size() == 0) {
			nodes = ModelUtils.getSelectedNodes(network);
		} else {
			nodes = nodeList.getValue();
		}

		for (CyNode node: nodes) {
			Long id = ModelUtils.getNodeID(network, node);
			String nodeType = ModelUtils.getNodeType(network, node);

			// System.out.println("Sending query to: "+manager.getNetworkURL(nodeType, attribute, query));

			Map<String, String> args = ModelUtils.buildArgMap(manager);
			ModelUtils.addNodeIDs(args, id, network);

			JSONObject results;
			try {
				results = HttpUtils.postJSON(manager.getExpandURL(nodeType, id.toString()), args, manager);
			} catch (ConnectionException e) {
				e.printStackTrace();
				monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				return;
			}


			if (results == null || !results.containsKey(SpokeManager.RESULT))
				return;
		
			ModelUtils.expandNetworkFromJSON(manager, manager.getSpokeNetwork(network), results);
		}

		// System.out.println("Results: "+results.toString());
		int viewThreshold = ModelUtils.getViewThreshold(manager);
		int networkSize = network.getNodeList().size() + network.getEdgeList().size();
		// System.out.println("Network size = "+networkSize+" viewThreshold = "+viewThreshold);
		if (networkSize < viewThreshold) {
			CyNetworkView networkView = ModelUtils.getNetworkView(manager, network);
			// Now style the network
			// TODO:  change style to accomodate STITCH
			networkView.updateView();

			// And lay it out
			/*
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
			Object context = alg.createLayoutContext();
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> layoutArgs = new HashMap<>();
			layoutArgs.put("defaultNodeMass", 10.0);
			setter.applyTunables(context, layoutArgs);
			Set<View<CyNode>> nodeViews = new HashSet<>(networkView.getNodeViews());
			insertTasksAfterCurrentTask(alg.createTaskIterator(networkView, context, nodeViews, null));
			*/
		} else {
			// ViewUtils.styleNetwork(manager, network, null);
		}
	}

	@ProvidesTitle
	public String getTitle() { return "Loading neighborhood"; }
}
