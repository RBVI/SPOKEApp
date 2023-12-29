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
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.spokeApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.spokeApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;
import edu.ucsf.rbvi.spokeApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.spokeApp.internal.utils.ViewUtils;

public class LoadNeighborhood extends AbstractTask {
	final SpokeNetwork spokeNet;
	final String nodeType;
	final String attribute;
	final String query;
	final String netName;

	public LoadNeighborhood(final SpokeNetwork spokeNet, 
													final String nodeType, final String attribute,
													final String query, final String netName) {
		this.spokeNet = spokeNet;
		this.query = query;
		this.nodeType = nodeType;
		this.attribute = attribute;
		this.netName = netName;
	}

	public void run(TaskMonitor monitor) {
		// make sure the list of resolved IDs is unique
		SpokeManager manager = spokeNet.getManager();

		System.out.println("Sending query to: "+manager.getNetworkURL(nodeType, attribute, query));

		Map<String, String> args = ModelUtils.buildArgMap(manager);

		JSONObject results;
		try {
			results = HttpUtils.postJSON(manager.getNetworkURL(nodeType, attribute, query), args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}


		if (results == null || !results.containsKey(SpokeManager.RESULT))
			return;
		
		CyNetwork network = ModelUtils.createNetworkFromJSON(spokeNet, results, null);

		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"SPOKE returned no results");
			return;
		}

		// Rename network collection to have the same name as network
		EditNetworkTitleTaskFactory editNetworkTitle = (EditNetworkTitleTaskFactory) manager
				.getService(EditNetworkTitleTaskFactory.class);
		insertTasksAfterCurrentTask(editNetworkTitle.createTaskIterator(network,
				network.getRow(network).get(CyNetwork.NAME, String.class)));
		
		ModelUtils.setNetURI(network, manager.getNetworkURL(nodeType, attribute, query));
		spokeNet.setNetwork(network);

		// System.out.println("Results: "+results.toString());
		int viewThreshold = ModelUtils.getViewThreshold(manager);
		int networkSize = network.getNodeList().size() + network.getEdgeList().size();
		// System.out.println("Network size = "+networkSize+" viewThreshold = "+viewThreshold);
		if (networkSize < viewThreshold) {
			// Now style the network
			// TODO:  change style to accomodate STITCH

			CyNetworkView networkView = manager.createNetworkView(network);
			
			ViewUtils.styleNetwork(manager, network, networkView);
			manager.addNetworkView(networkView);
	
			// And lay it out
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
			Object context = alg.createLayoutContext();
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> layoutArgs = new HashMap<>();
			layoutArgs.put("defaultNodeMass", 10.0);
			setter.applyTunables(context, layoutArgs);
			Set<View<CyNode>> nodeViews = new HashSet<>(networkView.getNodeViews());
			insertTasksAfterCurrentTask(alg.createTaskIterator(networkView, context, nodeViews, null));
		} else {
			// ViewUtils.styleNetwork(manager, network, null);
		}
	}

	@ProvidesTitle
	public String getTitle() { return "Loading neighborhood"; }
}
