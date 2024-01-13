package edu.ucsf.rbvi.spokeApp.internal.utils;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.property.SimpleCyProperty;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsf.rbvi.spokeApp.internal.model.ChoiceCutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.Cutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.DoubleCutoff;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeManager;
import edu.ucsf.rbvi.spokeApp.internal.model.SpokeNetwork;

public class ModelUtils {

	// Namespaces
	public static String SPOKEDB_NAMESPACE = "spoke";
	public static String NAMESPACE_SEPARATOR = "::";
	
	// Network names
	public static String DEFAULT_NAME_SPOKE = "SPOKE network";
	public static String QUERY = "SPOKE query";
	
	// Node information
	public static String NAME = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "name";
	public static String TYPE = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "type";
	public static String ID = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "id";
	public static String IDENTIFIER = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "identifier";
	public static String DESCRIPTION = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "description";
	public static String SYNONYMS = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "synonyms";
	public static String SOURCES = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "sources";
	public static String XREFS = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "xrefs";

	public static String CANONICAL = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "canonical name";
	public static String DISPLAY = "display name";
	public static String FULLNAME = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "full name";
	public static String CV_STYLE = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "chemViz Passthrough";
	public static String ELABEL_STYLE = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "enhancedLabel Passthrough";
	public static String NAMESPACE = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "namespace";
	public static String QUERYTERM = "query term";
	public static String SEQUENCE = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "sequence";
	public static String SMILES = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "smiles";
	public static String SPOKEID = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "database identifier";
	public static String STYLE = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + "SPOKE style";

	// Standard Node Columns
	public String[] standardNodeColumns = {NAME, TYPE, IDENTIFIER, DESCRIPTION, SYNONYMS, SOURCES, XREFS};
	public String[] standardEdgeColumns = {TYPE};

	// Network information
	public static String NET_URI = "uri";

	
	// Create network view size threshold
	// See https://github.com/cytoscape/cytoscape-impl/blob/develop/core-task-impl/
	// src/main/java/org/cytoscape/task/internal/loadnetwork/AbstractLoadNetworkTask.java
	public static int DEF_VIEW_THRESHOLD = 3000;
	public static String VIEW_THRESHOLD = "viewThreshold";
	
	public static int MAX_NODES_STRUCTURE_DISPLAY = 300;
	public static int MAX_NODE_PANELS = 25;
	
	public static boolean haveQueryTerms(CyNetwork network) {
		if (network == null) return false;
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(QUERYTERM, String.class) != null)
				return true;
		}
		return false;
	}

	public static void selectQueryTerms(CyNetwork network) {
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(QUERYTERM, String.class) != null)
				network.getRow(node).set(CyNetwork.SELECTED, true);
			else
				network.getRow(node).set(CyNetwork.SELECTED, false);
		}
	}

	public static String getErrorMessageFromJSON(SpokeManager manager,
			JSONObject object) {
		JSONObject errorMsg = getResultsFromJSON(object, JSONObject.class);
		if (errorMsg.containsKey("Error")) {
			System.out.println("An error occured while retrieving ppi enrichment: " + errorMsg.get("Error"));
		}
		if (errorMsg.containsKey("ErrorMessage")) {
			return (String) errorMsg.get("ErrorMessage");
		}
		return "";
	}
	
	public static CyNetwork createNetworkFromJSON(SpokeNetwork spokeNetwork, 
			JSONObject object, String netName) {
		spokeNetwork.getManager().ignoreAdd();
		CyNetwork network = createNetworkFromJSON(spokeNetwork.getManager(), object,
				netName);
		if (network == null)
			return null;
		spokeNetwork.getManager().addSpokeNetwork(spokeNetwork, network);
		spokeNetwork.getManager().listenToAdd();
		return network;
	}

	public static CyNetwork createNetworkFromJSON(SpokeManager manager, 
			JSONObject object, String netName) {
		JSONArray results = getResultsFromJSON(object, JSONArray.class);
		if (results == null)
			return null;

		if (netName == null) netName = DEFAULT_NAME_SPOKE;

		String defaultName;
		String defaultNameRootNet;
		defaultName = DEFAULT_NAME_SPOKE;
		defaultNameRootNet = DEFAULT_NAME_SPOKE;

		// add user suggested name
		if (netName != null && netName != "") {
			defaultName = defaultName + " - " + netName;
			defaultNameRootNet = defaultNameRootNet + " - " + netName;
			//defaultNameRootNet = defaultNameRootNet + 
		} /*else if (queryTermMap != null && queryTermMap.size() == 1 && queryTermMap.containsKey(ids)) {
			defaultName = defaultName + " - " + queryTermMap.get(ids);
			defaultNameRootNet = defaultNameRootNet + " - " + queryTermMap.get(ids);
		} */

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(defaultName, defaultNameRootNet);

		// Create a map to save the nodes
		Map<String, CyNode> nodeMap = new HashMap<>();

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		getJSON(manager, newNetwork, nodeMap, nodeNameMap, null, results);

		manager.addNetwork(newNetwork);
		return newNetwork;
	}

	public static void expandNetworkFromJSON(SpokeManager manager, SpokeNetwork spokeNetwork, JSONObject object) {
		JSONArray results = getResultsFromJSON(object, JSONArray.class);
		if (results == null)
			return;

		CyNetwork network = spokeNetwork.getNetwork();

		// Create a map to save the nodes
		Map<String, CyNode> nodeMap = new HashMap<>();

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		// Initialize the maps with our existing nodes
		for (CyNode node: network.getNodeList()) {
			Long id = getNodeID(network, node);
			String name = getNodeName(network, node);
			nodeMap.put(id.toString(), node);
			nodeNameMap.put(id.toString(), name);
		}

		// Initialize our edge list to avoid duplicate edges
		Set<Long> currentEdges = new HashSet<>();
		for (CyEdge edge: network.getEdgeList()) {
			if (network.getDefaultEdgeTable().getRow(edge.getSUID()).isSet(ID))
				currentEdges.add(network.getDefaultEdgeTable().getRow(edge.getSUID()).get(ID, Long.class));
		}

		getJSON(manager, network, nodeMap, nodeNameMap, currentEdges, results);

	}

	public static void setNetURI(CyNetwork network, String netURI) {
    createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, NET_URI);
    network.getRow(network).set(NET_URI, netURI);
  }

  public static String getNetURI(CyNetwork network) {
    if (network.getDefaultNetworkTable().getColumn(NET_URI) == null)
      return null;
    return network.getRow(network).get(NET_URI, String.class);
  }

	
	public static List<CyColumn> getGroupColumns(CyNetwork network) {
		Collection<CyColumn> colList = network.getDefaultNodeTable().getColumns();
		colList.remove(network.getDefaultNodeTable().getColumn(CyNetwork.SELECTED));
		colList.remove(network.getDefaultNodeTable().getColumn(CyNetwork.SUID));
		colList.removeAll(network.getDefaultNodeTable().getColumns(ModelUtils.SPOKEDB_NAMESPACE));
		colList.removeAll(network.getDefaultNodeTable().getColumns("target"));
		List<CyColumn> showList = new ArrayList<CyColumn>();
		int numValues = network.getNodeCount();
		for (CyColumn col : colList) {
			Set<?> colValues = new HashSet<>();			 
			if (col.getType().equals(String.class)) {
				colValues = new HashSet<String>(col.getValues(String.class));
			} else if (col.getType().equals(Integer.class)) {
				colValues = new HashSet<Integer>(col.getValues(Integer.class));
			} else if (col.getType().equals(Boolean.class)) {
				colValues = new HashSet<Boolean>(col.getValues(Boolean.class));
			} else if (col.getType().equals(Double.class)) {
				colValues = new HashSet<Double>(col.getValues(Double.class));
			}
			// skip column if it only contains unique values or only one value or unique values for more than half the nodes in the network 
			// filter for empty strings -> maybe enough to put a cutoff here?
			if (colValues.size() < 2 || colValues.size() == numValues || colValues.size() > numValues/2) {
				continue;
			}
			showList.add(col);
		}
		// sort attribute list
		Collections.sort(showList, new Comparator<CyColumn>() {
		    public int compare(CyColumn a, CyColumn b) {
		        return a.getName().compareToIgnoreCase(b.getName());
		    }
		});
		return showList;
	}
	
	private static List<CyNode> getJSON(SpokeManager manager, CyNetwork network,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
			Set<Long> currentEdges, JSONArray json) {

		List<CyNode> newNodes = new ArrayList<>();
		createColumnIfNeeded(network.getDefaultNodeTable(), Long.class, ID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, IDENTIFIER);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, TYPE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, NAME);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, DESCRIPTION);
		createListColumnIfNeeded(network.getDefaultNodeTable(), String.class, SOURCES);
		createListColumnIfNeeded(network.getDefaultNodeTable(), String.class, SYNONYMS);
		createListColumnIfNeeded(network.getDefaultNodeTable(), String.class, XREFS);

		// Edge columns
		createColumnIfNeeded(network.getDefaultEdgeTable(), Long.class, ID);
		createListColumnIfNeeded(network.getDefaultEdgeTable(), String.class, SOURCES);
		createColumnIfNeeded(network.getDefaultEdgeTable(), String.class, TYPE);

		Set<String> columnMap = new HashSet<>();

		// OK, separate nodes and edges
		List<JSONObject> nodes = new ArrayList<>();
		List<JSONObject> edges = new ArrayList<>();

		for (Object e: json) {
			if (e instanceof JSONObject ) {
				JSONObject element = (JSONObject)e;

				JSONObject data = (JSONObject)element.get("data");
				// String type = data.get("neo4j_type");
				// String id = data.get("id"); // This is the internal NEO4J id

				if (data.get("source") != null && data.get("target") != null) {
					// JSONObject properties = data.get("properties");
					edges.add(data);
				} else {
					nodes.add(data);
					// JSONObject properties = data.get("properties");
					// String identifier = properties.get("identifier");
				}
			}
		}

		// Get the nodes
		if (nodes.size() > 0) {
			for (JSONObject nodeObj : nodes) {
				CyNode newNode = createNode(manager, network, nodeObj, nodeMap, nodeNameMap, columnMap);
				if (newNode != null)
					newNodes.add(newNode);
			}
		}
		
		if (edges.size() > 0) {
			for (JSONObject edgeObj : edges) {
				createEdge(network, (JSONObject) edgeObj, nodeMap, nodeNameMap, currentEdges);
			}
		}
		return newNodes;
	}

	public static void createColumnsFromJSON(JSONArray nodes, CyTable table) {
		Map<String, Class<?>> jsonKeysClass = new HashMap<String, Class<?>>();
		Set<String> listKeys = new HashSet<>();
		for (Object nodeObj : nodes) {
			if (nodeObj instanceof JSONObject) {
				JSONObject nodeJSON = (JSONObject) nodeObj;
				for (Object objKey : nodeJSON.keySet()) {
					String key = (String) objKey;
					if (jsonKeysClass.containsKey(key)) {
						continue;
					}
					Object value = nodeJSON.get(key);
					if (value instanceof JSONArray) {
						JSONArray list = (JSONArray) value;
						Object element = list.get(0);
						jsonKeysClass.put(key, element.getClass());
						listKeys.add(key);
					} else {
						jsonKeysClass.put(key, value.getClass());
					}
				}
			}
		}
		List<String> jsonKeysSorted = new ArrayList<String>(jsonKeysClass.keySet());
		Collections.sort(jsonKeysSorted);
		for (String jsonKey : jsonKeysSorted) {
			// String formattedJsonKey = formatForColumnNamespace(jsonKey);
			if (listKeys.contains(jsonKey)) {
				createListColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			} else {
				createColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			}
		}

	}

	public static String getNodeType(CyNetwork network, CyNode node) {
		String type = network.getRow(node).get(TYPE, String.class);
		return type;
	}

	public static Long getNodeID(CyNetwork network, CyNode node) {
		Long id = network.getRow(node).get(ID, Long.class);
		return id;
	}

	public static String getNodeName(CyNetwork network, CyNode node) {
		String name = network.getRow(node).get(CyNetwork.NAME, String.class);
		return name;
	}

	public static void addNodeIDs(Map<String, String> args, Long id, CyNetwork network) {
		String ids = null;
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).isSet(ID)) {
				Long newID = network.getRow(node).get(ID, Long.class);
				if (newID != id) {
					if (ids == null) 
						ids = newID.toString();
					else
						ids += "|"+newID.toString();
				}
			}
		}
		args.put("node_ids", ids);
	}
	
	public static String formatForColumnNamespace(String columnName) {
		String formattedColumnName = columnName;
		if (columnName.contains("::")) {
			if (columnName.startsWith(SPOKEDB_NAMESPACE))
				formattedColumnName = columnName.substring(SPOKEDB_NAMESPACE.length() + 2);
			else
				formattedColumnName = columnName.replaceFirst("::", " ");
		}
		return formattedColumnName;
	}
	
	public static boolean isMergedSpokeNetwork(CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		if (nodeTable.getColumn(TYPE) == null && nodeTable.getColumn(DESCRIPTION) == null)
			return false;
		return true;
	}

	public static boolean isSpokeNetwork(CyNetwork network) {
    // This is a string network only if we have a confidence score in the network table,
    // "@id" column in the node table, and a "score" column in the edge table
    if (network == null || network.getRow(network).get(QUERY, Double.class) == null)
      return false;
    return isMergedSpokeNetwork(network);
  }


	public static String getExisting(CyNetwork network) {
		StringBuilder str = new StringBuilder();
		for (CyNode node : network.getNodeList()) {
			String stringID = network.getRow(node).get(SPOKEID, String.class);
			if (stringID != null && stringID.length() > 0)
				str.append(stringID + "\n");
		}
		return str.toString();
	}

	public static String getSelected(CyNetwork network, View<CyNode> nodeView) {
		StringBuilder selectedStr = new StringBuilder();
		if (nodeView != null) {
			String stringID = network.getRow(nodeView.getModel()).get(SPOKEID, String.class);
			selectedStr.append(stringID + "\n");
		}

		for (CyNode node : network.getNodeList()) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = network.getRow(node).get(SPOKEID, String.class);
				if (stringID != null && stringID.length() > 0)
					selectedStr.append(stringID + "\n");
			}
		}
		return selectedStr.toString();
	}

	private static CyNode createNode(SpokeManager manager, CyNetwork network, JSONObject nodeObj, 
	                                 Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
	                                 Set<String> columnMap) {

		String id = nodeObj.get("id").toString();
		String type = (String) nodeObj.get("neo4j_type");
		String name = "";

		if (nodeMap.containsKey(id))
			return null;

		// System.out.println("Node id = "+id+", stringID = "+stringId+", namespace="+namespace);
		CyNode newNode = network.addNode();
		CyRow row = network.getRow(newNode);

		// row.set(CyRootNetwork.SHARED_NAME, stringId);
		row.set(TYPE, type);
		row.set(ID, (Long)nodeObj.get("id"));

		JSONObject properties = (JSONObject)nodeObj.get("properties");

		for (Object objKey : properties.keySet()) {
			String key = (String) objKey;
			// Look for our "special" columns
			if (key.equals("name")) {
				name = (String)properties.get("name");
				row.set(CyNetwork.NAME, name);
			} else if (key.equals("identifier")) {
				String identifier = properties.get("identifier").toString();
				row.set(IDENTIFIER, identifier);
			} else if (key.equals("description")) {
				row.set(DESCRIPTION, (String) properties.get("description"));
			} else if (key.equals("source") || key.equals("sources")) {
				row.set(SOURCES, makeList(properties.get(objKey)));
			} else if (key.equals("synonyms")) {
				row.set(SYNONYMS, makeList(properties.get(objKey)));
			} else if (key.equals("xrefs")) {
				row.set(XREFS, makeList(properties.get(objKey)));
			} else {
				Object value = properties.get(key);
				String columnName = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + key;
				if (value instanceof JSONArray) {
					if (((JSONArray)value).size() == 0)
						continue;
					Class listType = getListType((JSONArray)(properties.get(objKey)));
					createListColumnIfNeeded(network.getDefaultNodeTable(), listType, columnName);
					row.set(columnName, makeList(properties.get(objKey)));
				} else if (value instanceof String) {
					createColumnIfNeeded(network.getDefaultNodeTable(), String.class, columnName);
					row.set(columnName, (String)properties.get(key));
				} else if (value instanceof Boolean) {
					createColumnIfNeeded(network.getDefaultNodeTable(), Boolean.class, columnName);
					row.set(columnName, (Boolean)properties.get(key));
				} else if (value instanceof Long) {
					createColumnIfNeeded(network.getDefaultNodeTable(), Long.class, columnName);
					row.set(columnName, (Long)properties.get(key));
				} else if (value instanceof Double) {
					createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, columnName);
					row.set(columnName, (Double)properties.get(key));
				}
			}
		}
		// TODO: Fix hack for saving query term for compounds
		nodeMap.put(id, newNode);
		nodeNameMap.put(id, name);
		return newNode;
	}

	private static Class getListType(JSONArray list) {
		if (list.size() == 0) {
			return String.class;
		}
		Object value = list.get(0);
		if (value instanceof JSONArray) {
			throw new RuntimeException("Lists of lists are not supported by converter");
		} else if (value instanceof JSONObject) {
			throw new RuntimeException("Dictionaries not a valid list column type");
		} else if (value instanceof String) {
			return String.class;
		} else if (value instanceof Long) {
			return Long.class;
		} else if (value instanceof Double) {
			return Double.class;
		} else if (value instanceof Boolean) {
			return Boolean.class;
		}
		throw new RuntimeException("Unsupported class in JSON file");
	}

	public static List<?> makeList(Object json) {
		return makeList(json, String.class);
	}

	public static List<?> makeList(Object json, Class listType) {
		if (json instanceof JSONArray) {
			if (listType == String.class) {
				List<String> list = new ArrayList<>();
				for (Object obj: (JSONArray)json) {
					list.add(obj.toString());
				}
				return list;
			} else if (listType == Long.class) {
				List<Long> list = new ArrayList<>();
				for (Object obj: (JSONArray)json) {
					list.add((Long)obj);
				}
				return list;
			} else if (listType == Double.class) {
				List<Double> list = new ArrayList<>();
				for (Object obj: (JSONArray)json) {
					list.add((Double)obj);
				}
				return list;
			} else if (listType == Number.class) {
				List<Number> list = new ArrayList<>();
				for (Object obj: (JSONArray)json) {
					list.add((Number)obj);
				}
				return list;
			} else if (listType == Boolean.class) {
				List<Boolean> list = new ArrayList<>();
				for (Object obj: (JSONArray)json) {
					list.add((Boolean)obj);
				}
				return list;
			}
		} else if (json instanceof JSONObject) {
			return Arrays.asList(json.toString());
		}
		return null;
	}

	public static Map<String,?> makeMapFromJSON(JSONObject json) {
		if (json == null) return null;
		Map<String,Object> newMap = new HashMap<>();
		for (Object key: json.keySet()) {
			Object value = json.get(key);
			if (value instanceof JSONObject) {
				newMap.put(key.toString(), makeMapFromJSON((JSONObject)value));
			} else if (value instanceof Long) {
				newMap.put(key.toString(), value);
			} else if (value instanceof Boolean) {
				newMap.put(key.toString(), value);
			} else if (value instanceof String) {
				newMap.put(key.toString(), value);
			} else if (value instanceof Double) {
				newMap.put(key.toString(), value);
			} else if (value instanceof JSONArray) {
				List<?> list = null;
				if (((JSONArray)value).size() > 0 ) {
					Class listType = getListType((JSONArray) value);
					list = makeList(value, listType);
				} else {
					list = new ArrayList<>();
				}
				newMap.put(key.toString(), list);
			}
		}
		return newMap;
	}

	private static void createEdge(CyNetwork network, JSONObject edgeObj,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap, Set<Long> currentEdges) {
		String source = edgeObj.get("source").toString();
		String target = edgeObj.get("target").toString();
		Long id = (Long)edgeObj.get("id");
		if (currentEdges != null && currentEdges.contains(id))
			return;

		CyNode sourceNode = nodeMap.get(source);
		CyNode targetNode = nodeMap.get(target);
		String interaction = (String) edgeObj.get("neo4j_type");

		CyEdge edge;

		// Don't create an edge if we already have one between these nodes
		edge = network.addEdge(sourceNode, targetNode, false);
		CyRow row = network.getRow(edge);
		row.set(CyNetwork.NAME,
				nodeNameMap.get(source) + " (" + interaction + ") " + nodeNameMap.get(target));
		row.set(CyEdge.INTERACTION, interaction);
		row.set(ID, id);

		if (currentEdges != null)
			currentEdges.add(id);

		JSONObject properties = (JSONObject)edgeObj.get("properties");

		for (Object objKey : properties.keySet()) {
			String key = (String) objKey;
			if (key.equalsIgnoreCase("source") || key.equalsIgnoreCase("sources")) {
				row.set(SOURCES, makeList(properties.get(objKey)));
				return;
			}

			try {
				Object value = properties.get(key);
				String columnName = SPOKEDB_NAMESPACE + NAMESPACE_SEPARATOR + key;
				if (value instanceof JSONArray) {
					Class listType = getListType((JSONArray)(properties.get(objKey)));
					createListColumnIfNeeded(network.getDefaultEdgeTable(), listType, columnName);
					row.set(columnName, makeList(properties.get(objKey),listType));
				} else if (value instanceof String) {
					createColumnIfNeeded(network.getDefaultEdgeTable(), String.class, columnName);
					row.set(columnName, (String)properties.get(key));
				} else if (value instanceof Boolean) {
					createColumnIfNeeded(network.getDefaultEdgeTable(), Boolean.class, columnName);
					row.set(columnName, (Boolean)properties.get(key));
				} else if (value instanceof Long) {
					createColumnIfNeeded(network.getDefaultEdgeTable(), Long.class, columnName);
					row.set(columnName, (Long)properties.get(key));
				} else if (value instanceof Double) {
					createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, columnName);
					row.set(columnName, (Double)properties.get(key));
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Error saving value for "+key+" in edge "+row.get(CyNetwork.NAME, String.class)+": "+e.getMessage());
			}
		}
	}

	public static void createColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			return;

		table.createColumn(columnName, clazz, false);
	}

	public static void replaceColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null) 
			table.deleteColumn(columnName);
		
		table.createColumn(columnName, clazz, false);
	}

	public static void createListColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			return;

		table.createListColumn(columnName, clazz, false);
	}

	public static void replaceListColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			table.deleteColumn(columnName);

		table.createListColumn(columnName, clazz, false);
	}

	public static void deleteColumnIfExisting(CyTable table, String columnName) {
		if (table.getColumn(columnName) != null)
			table.deleteColumn(columnName);		
	}
	
	public static String getName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, CyNetwork.NAME);
	}

	public static String getDisplayName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, DISPLAY);
	}

	public static String getString(CyNetwork network, CyIdentifiable ident, String column) {
		// System.out.println("network = "+network+", ident = "+ident+" column = "+column);
		try {
			if (network.getRow(ident, CyNetwork.DEFAULT_ATTRS) != null)
				return network.getRow(ident, CyNetwork.DEFAULT_ATTRS).get(column, String.class);
		} catch (Exception ex) {
			// ignore
		}
		return null;
	}

	public static <T> T getResultsFromJSON(JSONObject json, Class<? extends T> clazz) {
		if (json == null || !json.containsKey(SpokeManager.RESULT))
			return null;
		
		// System.out.println("json: " + json.toJSONString());

		Object result = json.get(SpokeManager.RESULT);
		if (!clazz.isAssignableFrom(result.getClass()))
			return null;

		return (T) result;
	}

	public static Integer getVersionFromJSON(JSONObject json) {
		if (json == null || !json.containsKey(SpokeManager.APIVERSION))
			return null;
		return (Integer) json.get(SpokeManager.APIVERSION);
	}

	public static Map<String, String> buildArgMap(SpokeManager manager) {
		Map<String, String> args = new HashMap<>();
		args.put("node_filters", listToString(manager.getNodeFilters(), "|"));
		args.put("edge_filters", listToString(manager.getEdgeFilters(), "|"));

		addCutoffArgs(args, manager.getActiveNodeCutoffs());
		addCutoffArgs(args, manager.getActiveEdgeCutoffs());
		addCutoffArgs(args, new ArrayList<Cutoff>(manager.getLimits().values()));
		return args;
	}

	private static void addCutoffArgs(Map<String, String> args, List<Cutoff> cutoffList) {
		for (Cutoff cutoff: cutoffList) {
			switch (cutoff.getType()) {
				case CHOICE:
					args.put(cutoff.getName(), listToString(((ChoiceCutoff)cutoff).getSelected(), "|"));
					break;
				case FLOAT:
					args.put(cutoff.getName(), String.valueOf(((DoubleCutoff)cutoff).getValue()));
					break;
			}
		}
	}

	public static void setStringProperty(CyProperty<Properties> properties, 
	                                     String propertyKey, Object propertyValue) {
		Properties p = properties.getProperties();
		p.setProperty(propertyKey, propertyValue.toString());
	}

	public static boolean hasProperty(CyProperty<Properties> properties, String propertyKey) {
		Properties p = properties.getProperties();
		if (p.getProperty(propertyKey) != null) 
			return true;
		return false;
	}
	public static String getStringProperty(CyProperty<Properties> properties, String propertyKey) {
		Properties p = properties.getProperties();
		if (p.getProperty(propertyKey) != null) 
			return p.getProperty(propertyKey);
		return null;
	}

	public static Double getDoubleProperty(CyProperty<Properties> properties, String propertyKey) {
		String value = ModelUtils.getStringProperty(properties, propertyKey);
		if (value == null) return null;
		return Double.valueOf(value);
	}

	public static Integer getIntegerProperty(CyProperty<Properties> properties, String propertyKey) {
		String value = ModelUtils.getStringProperty(properties, propertyKey);
		if (value == null) return null;
		return Integer.valueOf(value);
	}

	public static Boolean getBooleanProperty(CyProperty<Properties> properties, String propertyKey) {
		String value = ModelUtils.getStringProperty(properties, propertyKey);
		if (value == null) return null;
		return Boolean.valueOf(value);
	}

	public static String listToString(List<?> list, String sep) {
		String str = "";
		if (list == null || list.size() == 0) return str;
		for (int i = 0; i < list.size()-1; i++) {
			str += list.get(i)+sep;
		}
		return str + list.get(list.size()-1).toString();
	}

	public static List<String> stringToList(String string) {
		if (string == null || string.length() == 0) return new ArrayList<String>();
		String [] arr = string.split(",");
		return Arrays.asList(arr);
	}

	public static CyProperty<Properties> getPropertyService(SpokeManager manager,
			SavePolicy policy) {
			String name = "spokeApp";
			if (policy.equals(SavePolicy.SESSION_FILE)) {
				CyProperty<Properties> service = manager.getService(CyProperty.class, "(cyPropertyName="+name+")");
				// Do we already have a session with our properties
				if (service.getSavePolicy().equals(SavePolicy.SESSION_FILE))
					return service;

				// Either we have a null session or our properties aren't in this session
				Properties props = new Properties();
				service = new SimpleCyProperty(name, props, Properties.class, SavePolicy.SESSION_FILE);
				Properties serviceProps = new Properties();
				serviceProps.setProperty("cyPropertyName", service.getName());
				manager.registerAllServices(service, serviceProps);
				return service;
			} else if (policy.equals(SavePolicy.CONFIG_DIR) || policy.equals(SavePolicy.SESSION_FILE_AND_CONFIG_DIR)) {
				CyProperty<Properties> service = new ConfigPropsReader(policy, name);
				Properties serviceProps = new Properties();
				serviceProps.setProperty("cyPropertyName", service.getName());
				manager.registerAllServices(service, serviceProps);
				return service;
		}
		return null;
	}

	public static class ConfigPropsReader extends AbstractConfigDirPropsReader {
		ConfigPropsReader(SavePolicy policy, String name) {
			super(name, "spokeApp.props", policy);
		}
	}

	public static int getViewThreshold(SpokeManager manager) {
		final Properties props = (Properties) manager
				.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)").getProperties();
		final String vts = props.getProperty(VIEW_THRESHOLD);
		int threshold;

		try {
			threshold = Integer.parseInt(vts);
		} catch (Exception e) {
			threshold = DEF_VIEW_THRESHOLD;
		}

		return threshold;
	}

	public static void copyRow(CyTable fromTable, CyTable toTable, CyIdentifiable from, CyIdentifiable to, List<String> columnsCreated) {
		for (CyColumn col: fromTable.getColumns()) {
			// TODO: Is it OK to not check for this?
			//if (!columnsCreated.contains(col.getName()))
			//	continue;
			if (col.getName().equals(CyNetwork.SUID)) 
				continue;
			if (col.getName().equals(CyNetwork.SELECTED)) 
				continue;
			if (from.getClass().equals(CyNode.class) && col.getName().equals(CyRootNetwork.SHARED_NAME)) 
				continue;
			if (col.getName().equals(ModelUtils.QUERYTERM) || col.getName().equals(ModelUtils.DISPLAY) || col.getName().equals(CyNetwork.NAME)) {
				Object v = fromTable.getRow(from.getSUID()).getRaw(col.getName());
				toTable.getRow(to.getSUID()).set(col.getName() + ".copy", v);
				continue;
			}
			// TODO: Is it OK to overwrite interaction type? 
			//if (from.getClass().equals(CyEdge.class) && col.getName().equals(CyRootNetwork.SHARED_INTERACTION)) 
			//	continue;
			//if (from.getClass().equals(CyEdge.class) && col.getName().equals(CyEdge.INTERACTION)) 
			//	continue;
			Object v = fromTable.getRow(from.getSUID()).getRaw(col.getName());
			toTable.getRow(to.getSUID()).set(col.getName(), v);
		}
	}

	public static void copyNodes(CyNetwork fromNetwork, CyNetwork toNetwork, Map<CyNode, CyNode> nodeMap, 
	                             String keyColumn, List<String> toColumns) {
		for (CyNode node: fromNetwork.getNodeList()) {
			String key = fromNetwork.getRow(node).get(keyColumn, String.class);
			// TODO: double-check what happens when key == null
			if (!nodeMap.containsKey(node)) {
				CyNode newNode = toNetwork.addNode();
				nodeMap.put(node, newNode);
				String name = fromNetwork.getRow(node).get(keyColumn, String.class);
				toNetwork.getRow(newNode).set(CyNetwork.NAME, name);
				for (String col: toColumns) {
					toNetwork.getRow(newNode).set(col, key);
				}
			}
		}
	}

	public static List<String> copyColumns(CyTable fromTable, CyTable toTable) {
		List<String> columns = new ArrayList<String>();
		for (CyColumn col: fromTable.getColumns()) {
			String fqn = col.getName();
			// Does that column already exist in our target?
			if (toTable.getColumn(fqn) == null) {
				// No, create it.
				if (col.getType().equals(List.class)) {
					// There is no easy way to handle this, unfortunately...
					// toTable.createListColumn(fqn, col.getListElementType(), col.isImmutable(), (List<?>)col.getDefaultValue());
					if (col.getListElementType().equals(String.class))
						toTable.createListColumn(fqn, String.class, col.isImmutable(), 
						                         (List<String>)col.getDefaultValue());
					else if (col.getListElementType().equals(Long.class))
						toTable.createListColumn(fqn, Long.class, col.isImmutable(), 
						                         (List<Long>)col.getDefaultValue());
					else if (col.getListElementType().equals(Double.class))
						toTable.createListColumn(fqn, Double.class, col.isImmutable(), 
						                         (List<Double>)col.getDefaultValue());
					else if (col.getListElementType().equals(Integer.class))
						toTable.createListColumn(fqn, Integer.class, col.isImmutable(), 
						                         (List<Integer>)col.getDefaultValue());
					else if (col.getListElementType().equals(Boolean.class))
						toTable.createListColumn(fqn, Boolean.class, col.isImmutable(), 
						                         (List<Boolean>)col.getDefaultValue());
				} else {
					toTable.createColumn(fqn, col.getType(), col.isImmutable(), col.getDefaultValue());
					columns.add(fqn);
				}
			} else if (fqn.equals(ModelUtils.QUERYTERM) || fqn.equals(ModelUtils.DISPLAY) || fqn.equals(CyNetwork.NAME)) {
				toTable.createColumn(fqn + ".copy", col.getType(), col.isImmutable(), col.getDefaultValue());
				columns.add(fqn + ".copy");
			}
		}
		return columns;
	}

	public static void copyNodePositions(SpokeManager manager, CyNetwork from, CyNetwork to, 
	                                     Map<CyNode, CyNode> nodeMap, String column) {
		CyNetworkView fromView = getNetworkView(manager, from);
		CyNetworkView toView = getNetworkView(manager, to);
		if (fromView == null || toView == null) return;
		for (View<CyNode> nodeView: fromView.getNodeViews()) {
			// Get the to node
			CyNode fromNode = nodeView.getModel();
			if (!nodeMap.containsKey(fromNode))
				continue;
			View<CyNode> toNodeView = toView.getNodeView(nodeMap.get(fromNode));
			// Copy over the positions
			Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			Double z = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);
			toNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			toNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			if (z != null && z != 0.0)
				toNodeView.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, z);
		}
	}

	public static void copyEdges(CyNetwork fromNetwork, CyNetwork toNetwork, 
	                             Map<CyNode, CyNode> nodeMap, String column) {
		List<String> columnsCreated = copyColumns(fromNetwork.getDefaultEdgeTable(), toNetwork.getDefaultEdgeTable());
		List<CyEdge> edgeList = fromNetwork.getEdgeList();
		for (CyEdge edge: edgeList) {
			CyNode sourceNode = edge.getSource();
			CyNode targetNode = edge.getTarget();
			boolean isDirected = edge.isDirected();

			if (!nodeMap.containsKey(sourceNode) || !nodeMap.containsKey(targetNode))
				continue;

			CyNode newSource = nodeMap.get(sourceNode);
			CyNode newTarget = nodeMap.get(targetNode);

			CyEdge newEdge = toNetwork.addEdge(newSource, newTarget, isDirected);
			copyRow(fromNetwork.getDefaultEdgeTable(), toNetwork.getDefaultEdgeTable(), edge, newEdge, columnsCreated);
		}
	}

	public static CyNetworkView getNetworkView(SpokeManager manager, CyNetwork network) {
		Collection<CyNetworkView> views = 
						manager.getService(CyNetworkViewManager.class).getNetworkViews(network);

		// At some point, figure out a better way to do this
		for (CyNetworkView view: views) {
			return view;
		}
		return null;
	}

	public static void copyNodeAttributes(CyNetwork from, CyNetwork to, 
	                                      Map<CyNode, CyNode> nodeMap, String column) {
		// System.out.println("copyNodeAttributes");
		List<String> columnsCreated = copyColumns(from.getDefaultNodeTable(), to.getDefaultNodeTable());
		for (CyNode node: from.getNodeList()) {
			if (!nodeMap.containsKey(node))
				continue;
			CyNode newNode = nodeMap.get(node);
			copyRow(from.getDefaultNodeTable(), to.getDefaultNodeTable(), node, newNode, columnsCreated);
		}
	}

}
