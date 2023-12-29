package edu.ucsf.rbvi.spokeApp.internal.model;

import java.awt.Color;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.color.PaletteProvider;
import org.cytoscape.util.color.PaletteProviderManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;

import static org.cytoscape.work.ServiceProperties.NODE_ADD_MENU;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

// import org.jcolorbrewer.ColorBrewer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.spokeApp.internal.io.HttpUtils;

import edu.ucsf.rbvi.spokeApp.internal.tasks.ExpandNodeTaskFactory;
import edu.ucsf.rbvi.spokeApp.internal.tasks.SpokeSearchTaskFactory;

import edu.ucsf.rbvi.spokeApp.internal.utils.ModelUtils;


public class SpokeManager implements NetworkAddedListener, SessionLoadedListener, NetworkAboutToBeDestroyedListener, SetCurrentNetworkListener {
	final CyServiceRegistrar registrar;
	final CyEventHelper cyEventHelper;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	final TaskManager<?,?> dialogTaskManager;
	final SynchronousTaskManager<?> synchronousTaskManager;
	final CommandExecutorTaskFactory commandExecutorTaskFactory;
	final AvailableCommands availableCommands;
	final Palette brewerPalette;

	private Map<CyNetwork, SpokeNetwork> spokeNetworkMap;

	// These are various default values that are saved and restored from
	// the network table
	// TODO: move all of these to SpokeNetwork?
	public static String SPOKE_URL = "https://spoke.rbvi.ucsf.edu/api/";
	public static String APIVERSION = "v1";
  public static String RESULT = "QueryResult";
  public static String TYPES = "types2";
  public static String SEARCH = "search";

	public static String CallerIdentity = "spoke_app";

	private CyNetwork newNetwork = null;
	private boolean ignore = false;
 	private CyProperty<Properties> sessionProperties;
  private CyProperty<Properties> configProps;
	private Map<String, ?> node_types;
	private Map<String, ?> edge_types;
	private Map<String, Cutoff> node_cutoffs;
	private Map<String, Cutoff> edge_cutoffs;
	private Map<String, Cutoff> limits;


	public SpokeManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;

		// Get our task managers
		dialogTaskManager = registrar.getService(TaskManager.class);
		synchronousTaskManager = registrar.getService(SynchronousTaskManager.class);
		availableCommands = registrar.getService(AvailableCommands.class);
		commandExecutorTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);
		cyEventHelper = registrar.getService(CyEventHelper.class);
		spokeNetworkMap = new HashMap<>();

		PaletteProviderManager pm = registrar.getService(PaletteProviderManager.class);
		PaletteProvider brewerProvider = pm.getPaletteProvider("ColorBrewer");
		brewerPalette = brewerProvider.getPalette("Paired colors");

		// Get the node, edge, and filter information
		getTypes();
	}


	public String adaptNetworkName(String name) {
    CyNetworkManager netMgr = registrar.getService(CyNetworkManager.class);
    Set<CyNetwork> nets = netMgr.getNetworkSet();
    Set<CyNetwork> allNets = new HashSet<CyNetwork>(nets);
    for (CyNetwork net : nets) {
      allNets.add(((CySubNetwork)net).getRootNetwork());
    }
    // See if this name is already taken by a network or a network collection (root network)
    int index = -1;
    boolean match = false;
    for (CyNetwork net: allNets) {
      String netName = net.getRow(net).get(CyNetwork.NAME, String.class);
      if (netName.equals(name)) {
        match = true;
      } else if (netName.startsWith(name)) {
        String subname = netName.substring(name.length());
        if (subname.startsWith(" - ")) {
          try {
            int v = Integer.parseInt(subname.substring(3));
            if (v >= index)
              index = v+1;
          } catch (NumberFormatException e) {}
        }
      }
    }
    if (match && index < 0) {
      name = name + " - 1";
    } else if (index > 0) {
      name = name + " - " + index;
    }
    return name;
  }


	void registerSearchFactories() {
		// Register our Network search factories
		{
			SpokeSearchTaskFactory spokeSearch = new SpokeSearchTaskFactory(this);
			Properties propsSearch = new Properties();
			registrar.registerService(spokeSearch, NetworkSearchTaskFactory.class, propsSearch);
		}

		{
			Properties props = new Properties();
			props.put(NODE_ADD_MENU, "SPOKE");
			props.put(TITLE, "Expand node");
			props.put(PREFERRED_MENU, "SPOKE");
			ExpandNodeTaskFactory expand = new ExpandNodeTaskFactory(this);
			registrar.registerService(expand, NodeViewTaskFactory.class, props);
		}

	}

	public CyNetwork createNetwork(String name, String rootNetName) {
		CyNetwork network = registrar.getService(CyNetworkFactory.class).createNetwork();
		network.getRow(network).set(CyNetwork.NAME, adaptNetworkName(name));
		CyNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, adaptNetworkName(rootNetName));
		return network;
	}

	public void addSpokeNetwork(SpokeNetwork spokeNet, CyNetwork network) {
		spokeNetworkMap.put(network, spokeNet);
		spokeNet.setNetwork(network);
    newNetwork = network; // Do this in case we don't have a "current" network
	}

	public SpokeNetwork getSpokeNetwork(CyNetwork network) {
		if (spokeNetworkMap.containsKey(network))
			return spokeNetworkMap.get(network);
		return null;
	}

	public List<SpokeNetwork> getSpokeNetworks() {
		return new ArrayList<>(spokeNetworkMap.values());
	}

	public String getNetworkName(CyNetwork net) {
		return net.getRow(net).get(CyNetwork.NAME, String.class);
	}

	public String getRootNetworkName(CyNetwork net) {
		CyRootNetwork rootNet = ((CySubNetwork)net).getRootNetwork();
		return rootNet.getRow(rootNet).get(CyNetwork.NAME, String.class);
	}

	public CyNetworkView createNetworkView(CyNetwork network) {
		CyNetworkView view = registrar.getService(CyNetworkViewFactory.class)
		                                          .createNetworkView(network);
		return view;
	}

	public void addNetworkView(CyNetworkView view) {
		registrar.getService(CyNetworkViewManager.class)
		                                          .addNetworkView(view);
	}

	public void addNetwork(CyNetwork network) {
		registrar.getService(CyNetworkManager.class).addNetwork(network);
		registrar.getService(CyApplicationManager.class).setCurrentNetwork(network);
	}
	
	public CyNetwork getCurrentNetwork() {
		CyNetwork network = registrar.getService(CyApplicationManager.class).getCurrentNetwork();
    if (network != null) return network;
    return newNetwork;
	}

	public CyNetworkView getCurrentNetworkView() {
		return registrar.getService(CyApplicationManager.class).getCurrentNetworkView();
	}

	public void flushEvents() {
		cyEventHelper.flushPayloadEvents();
	}

	public void execute(TaskIterator iterator) {
		execute(iterator, false);
	}

	public void execute(TaskIterator iterator, TaskObserver observer) {
		execute(iterator, observer, false);
	}

	public void execute(TaskIterator iterator, boolean synchronous) {
		if (synchronous) {
			synchronousTaskManager.execute(iterator);
		} else {
			dialogTaskManager.execute(iterator);
		}
	}

	public void execute(TaskIterator iterator, TaskObserver observer, boolean synchronous) {
		if (synchronous) {
			synchronousTaskManager.execute(iterator, observer);
		} else {
			dialogTaskManager.execute(iterator, observer);
		}
	}

	public void executeCommand(String namespace, String command, 
	                           Map<String, Object> args, TaskObserver observer) {
		TaskIterator ti = commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
		execute(ti, true);
	}

	public TaskIterator getCommandTaskIterator(String namespace, String command, 
	                           Map<String, Object> args, TaskObserver observer) {
		return commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
	}

	public String getNetworkURL(String nodeType, String attribute, String query) {
    return SPOKE_URL+"/"+APIVERSION+"/neighborhood/"+nodeType+"/"+attribute+"/"+query;
  }

	public String getExpandURL(String nodeType, String node_id) {
    return SPOKE_URL+"/"+APIVERSION+"/expand/"+nodeType+"/"+node_id;
  }


	public void info(String info) {
		logger.info(info);
	}

	public void warn(String warn) {
		logger.warn(warn);
	}

	public void error(String error) {
		logger.error(error);
	}

	public void critical(String criticalError) {
		logger.error(criticalError);
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, "<html><p style=\"width:200px;\">" + criticalError + "</p></html>", "Critical spokeApp error", JOptionPane.ERROR_MESSAGE);
					}
				}
			);
	}

	public void ignoreAdd() {
    ignore = true;
  }

  public void listenToAdd() {
    ignore = false;
  }


	public void processNewNetwork(CyNetwork network) {
    // This is a string network only if we have a confidence score in the network table,
    // "@id", "species", "canonical name", and "sequence" columns in the node table, and
    // a "score" column in the edge table
    boolean foundSpokeNet = false;
    if (ModelUtils.isSpokeNetwork(network)) {
      SpokeNetwork spokeNet = new SpokeNetwork(this);
      addSpokeNetwork(spokeNet, network);
      foundSpokeNet = true;
    }
  }

	
	public void handleEvent(SetCurrentNetworkEvent event) {
		CyNetwork network = event.getNetwork();
		if (ignore || network == null || getSpokeNetwork(network) != null) return;
		
		processNewNetwork(network);
	}
	
	public void handleEvent(NetworkAddedEvent nae) {
		CyNetwork network = nae.getNetwork();
		if (ignore) return;

		processNewNetwork(network);
	}

	public void handleEvent(SessionLoadedEvent arg0) {
		// Get any properties we stored in the session
		sessionProperties = ModelUtils.getPropertyService(this, SavePolicy.SESSION_FILE);

		// Create string networks for any networks loaded by string
		Set<CyNetwork> networks = arg0.getLoadedSession().getNetworks();
		if (networks.size() == 0)
			return;
		for (CyNetwork network: networks) {
			if (ModelUtils.isSpokeNetwork(network)) {
				SpokeNetwork stringNet = new SpokeNetwork(this);
				addSpokeNetwork(stringNet, network);
			}
		}

	}

	public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
		CyNetwork network = e.getNetwork();
		// delete enrichment tables
		CyTableManager tableManager = getService(CyTableManager.class);
		// TODO: are we sure that we don't need to reload?
		// reloadEnrichmentPanel();
		// remove as string network
		if (spokeNetworkMap.containsKey(network))
			spokeNetworkMap.remove(network);
	}

	public <T> T getService(Class<? extends T> clazz, String filter) {
		return registrar.getService(clazz, filter);
	}

	public <T> T getService(Class<? extends T> clazz) {
		return registrar.getService(clazz);
	}

	public void registerService(Object service, Class<?> clazz, Properties props) {
		registrar.registerService(service, clazz, props);
	}

	public void registerAllServices(CyProperty<Properties> service, Properties props) {
		registrar.registerAllServices(service, props);
	}

	public void unregisterService(Object service, Class<?> clazz) {
		registrar.unregisterService(service, clazz);
	}

	public void setVersion(String version) {
		String v = version.replace('.', '_');
		SpokeManager.CallerIdentity = "spoke_app_v"+v;
	}

	/* Information about types and cutoffs */

	// Get all of the types information
	public void getTypes() {
		Map<String, String> args = new HashMap<>();
		final SpokeManager manager = this;

		// We want to run this in the backround
		Executors.newCachedThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				String URI = SPOKE_URL+APIVERSION+"/"+TYPES;
				JSONObject types = null;
				try {
					types = ModelUtils.getResultsFromJSON(HttpUtils.getJSON(URI, args, manager, 100000), JSONObject.class);
					node_types = ModelUtils.makeMapFromJSON((JSONObject)types.get("node_types"));
					edge_types = ModelUtils.makeMapFromJSON((JSONObject)types.get("edge_types"));
					node_cutoffs = createCutoffMap((JSONObject)types.get("node_cutoffs"));
					edge_cutoffs = createCutoffMap((JSONObject)types.get("edge_cutoffs"));

					limits = createLimits();

					registerSearchFactories();

				} catch (ConnectionException e) {
					error("Unable to get types from SPOKE: "+e.getMessage());
					return;
				} catch (SocketTimeoutException e) {
					error("SPOKE not responding: "+e.getMessage());
					return;
				}
			}
		});
	}

	public Map<String, Cutoff> getNodeCutoffs() {
		return node_cutoffs;
	}

	public List<Cutoff> getActiveNodeCutoffs() {
		return getActiveCutoffs(node_cutoffs);
	}

	public List<Cutoff> getActiveEdgeCutoffs() {
		return getActiveCutoffs(edge_cutoffs);
	}

	private List<Cutoff> getActiveCutoffs(Map<String, Cutoff> cutoffList) {
		List<Cutoff> cutoffs = new ArrayList<>();
		for (Cutoff cutoff: cutoffList.values()) {
			if (cutoff.isActive())
				cutoffs.add(cutoff);
		}
		return cutoffs;
	}

	public Map<String, Cutoff> getEdgeCutoffs() {
		return edge_cutoffs;
	}

	public Map<String, Cutoff> getLimits() {
		return limits;
	}

	public List<String> getNodeTypes() {
		if (node_types == null)
			return null;
		List<String> types =  new ArrayList<String>(node_types.keySet());
		Collections.sort(types);
		return types;
	}

	public List<String> getEdgeTypes() {
		if (edge_types == null)
			return null;
		List<String> types =  new ArrayList<String>(edge_types.keySet());
		Collections.sort(types);
		return types;
	}

	public List<String> getEdgeNames() {
		if (edge_types == null)
			return null;
		List<String> names = new ArrayList<>();
		for (String type: edge_types.keySet()) {
			names.add(getEdgeName(type));
		}
		Collections.sort(names);
		return names;
	}

	public List<String> getNodeFilters() {
		List<String> list = new ArrayList<>();
		for (String nodeType: node_types.keySet()) {
			if (getNodeSkip(nodeType))
				continue;
			list.add(nodeType);
		}
		return list;
	}

	public List<String> getEdgeFilters() {
		List<String> list = new ArrayList<>();
		for (String edgeType: edge_types.keySet()) {
			if (getEdgeSkip(edgeType))
				continue;
			list.add(edgeType);
		}
		return list;
	}

	public String getEdgeName(String type) {
		return getStringTypeValue(edge_types, type, "name");
	}

	public String getEdgeType(String type) {
		return getStringTypeValue(edge_types, type, "edgeType");
	}

	public String getEdgeFromNode(String type) {
		return getStringTypeValue(edge_types, type, "fromNode");
	}

	public String getEdgeToNode(String type) {
		return getStringTypeValue(edge_types, type, "toNode");
	}

	public Boolean getEdgeSkip(String type) {
		Object s = getTypeValue(edge_types, type, "skip");
		if (s == null) return null;
		Boolean skip = (Boolean) s;
		return skip;
	}

	public void setEdgeSkip(String type, boolean skip) {
		Map<String, Object> tMap = getTypeMap(edge_types, type);
		tMap.put("skip", skip);
	}

	public Boolean getNodeSkip(String type) {
		Object s = getTypeValue(node_types, type, "skip");
		if (s == null) return null;
		Boolean skip = (Boolean) s;
		return skip;
	}

	public void setNodeSkip(String type, boolean skip) {
		Map<String, Object> tMap = getTypeMap(node_types, type);
		tMap.put("skip", skip);
	}

	public Color getNodeFillColor(String type) {
		Object s = getTypeValue(node_types, type, "background-color");
		if (s == null) return null;
		String sColor = (String) s;
		return Color.decode(sColor.toUpperCase());
	}

	public String getStringTypeValue(Map<String, ?> typeMap, String type, String key) {
		Object s = getTypeValue(typeMap, type, key);
		if (s == null) return null;
		String strVal = (String) s;
		return strVal;
	}

	public Object getTypeValue(Map<String, ?> typeMap, String type, String key) {
		Map<String, Object> tMap = getTypeMap(typeMap, type);
		if (tMap == null) return null;

		return tMap.get(key);
	}

	public Map<String, Object> getTypeMap(Map<String, ?> typeMap, String type) {
		if (typeMap == null) return null;
		Object t = typeMap.get(type);
		if (t == null) return null;
		if (t instanceof Map)
			return (Map<String, Object>)t;
		return null;
	}

	public List<String> getSearchResults(String nodeType, String query) {
		String URI = SPOKE_URL+APIVERSION+"/"+SEARCH+"/"+nodeType+"/"+query;
		JSONArray options = null;
		Map<String, String> args = new HashMap<>();

		try {
			List<String> result = new ArrayList<>();
			options = ModelUtils.getResultsFromJSON(HttpUtils.getJSON(URI, args, this, 100000), JSONArray.class);
			for (int i = 0; i < options.size(); i++) {
				JSONObject res = (JSONObject)options.get(i);
				String name = (String)res.get("name");
				String id = (String)res.get("identifier");
				String desc = (String)res.get("description");
				result.add(name+"\t("+id+")\t"+desc);
			}
			return result;
		} catch (ConnectionException e) {
			error("Unable to get search results from SPOKE: "+e.getMessage());
			return null;
		} catch (SocketTimeoutException e) {
			error("SPOKE not responding: "+e.getMessage());
			return null;
		}
	}

	private Map<String, Cutoff> createCutoffMap(JSONObject cutoffs) {
		Map<String, Cutoff> map = new HashMap<>();
		for (Object key: cutoffs.keySet()) {
			JSONObject value = (JSONObject)cutoffs.get(key);
			map.put((String)key, Cutoff.createCutoff((String)key, value));
		}
		return map;
	}

	private Map<String, Cutoff> createLimits() {
		Map<String, Cutoff> map = new HashMap<>();
		map.put("path", new DoubleCutoff("depth", "Maximum path length <= ", 1, 10, 1, 1));
		map.put("sea", new DoubleCutoff("sea_cutoff", "SEA p-value cutoff <= 1e-", 1, 10, 10, 1));
		return map;
	}

}
