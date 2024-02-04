package edu.ucsf.rbvi.spokeApp.internal.tasks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

public class Metagraph extends AbstractTask {
	final SpokeNetwork spokeNet;
	final String netName;

	public Metagraph(final SpokeNetwork spokeNet, 
									 final String netName) {
		this.spokeNet = spokeNet;
		this.netName = netName;
	}

	public void run(TaskMonitor monitor) {
		// make sure the list of resolved IDs is unique
		SpokeManager manager = spokeNet.getManager();

		String url = manager.getMetagraphURL();

		System.out.println("Sending query to: "+url);

		Map<String, String> args = new HashMap<>();

		JSONObject results;
		try {
			results = HttpUtils.postJSON(url, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}


		if (results == null || !results.containsKey(SpokeManager.RESULT))
			return;
		
		CyNetwork network = ModelUtils.createNetworkFromJSON(spokeNet, results, "Metagraph", url);

		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"SPOKE returned no results");
			return;
		}

		// Rename network collection to have the same name as network
		EditNetworkTitleTaskFactory editNetworkTitle = (EditNetworkTitleTaskFactory) manager
				.getService(EditNetworkTitleTaskFactory.class);
		insertTasksAfterCurrentTask(editNetworkTitle.createTaskIterator(network,
				network.getRow(network).get(CyNetwork.NAME, String.class)));
		
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
