// ActivePaths.java:  a plugin for CytoscapeWindow,
// which uses VERA & SAM expression data
// to propose active gene regulatory paths
//------------------------------------------------------------------------------
// $Revision: 11526 $
// $Date: 2007-09-05 14:14:24 -0700 (Wed, 05 Sep 2007) $
// $Author: rmkelley $
//------------------------------------------------------------------------------
package csplugins.jActiveModules;
//------------------------------------------------------------------------------

import org.cytoscape.model.CyNode;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import csplugins.jActiveModules.data.ActivePathFinderParameters;
import csplugins.jActiveModules.rest.models.ActiveModuleAnnealResult;
import csplugins.jActiveModules.rest.models.ActiveModuleResult;
import csplugins.jActiveModules.rest.models.Module;
//import csplugins.jActiveModules.util.Scaler;
import csplugins.jActiveModules.util.ScalerFactory;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
//import cytoscape.data.Semantics;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;

import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.model.CyRow;
import java.util.Collection;
//import java.io.File;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import csplugins.jActiveModules.ServicesUtil;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;


//-----------------------------------------------------------------------------------
public class ActivePaths extends AbstractTask implements ActivePathViewer, ObservableTask {

	private static final Logger logger = LoggerFactory.getLogger(ActivePaths.class);

	protected boolean showTable = true;
	protected boolean hideOthers = true;
	protected boolean randomize = false;

	protected JMenuBar menubar;
	protected JMenu expressionConditionsMenu;
	protected String currentCondition = "none";
	protected Component[] activePaths;
	
	protected String[] attrNames;
	protected static boolean activePathsFindingIsAvailable;
	protected JButton activePathToolbarButton;
	protected JFrame mainFrame;
	protected CyNetwork cyNetwork;
	protected String titleForCurrentSelection;
	protected ActivePathFinderParameters apfParams;
	protected static double MIN_SIG = 0.0000000000001;
	protected static double MAX_SIG = 1 - MIN_SIG;

	protected static int resultsCount = 1;
	protected ActiveModulesUI parentUI;
	
	private static int MAX_NETWORK_VIEWS =5; // = PropUtil.getInt(CytoscapeInit.getProperties(), "moduleNetworkViewCreationThreshold", 5);
	private static int runCount = 0;	
	private static final String VS_OVERVIEW_NAME = "jActiveModules Overview Style";
	private static final String VS_MODULE_NAME = "jActiveModules Module Style";
	private static  VisualStyle overviewVS = null;
	private static  VisualStyle moduleVS = null;
	
	// This is common prefix for all finders.
	private static final String MODULE_FINDER_PREFIX = "jActiveModules.";
	private static final String EDGE_SCORE = MODULE_FINDER_PREFIX + "overlapScore";
	
	private static final String NODE_SCORE = MODULE_FINDER_PREFIX + "activepathScore";

	private ActiveModuleResult activeModuleResult;
	
	static {
		if (overviewVS == null && moduleVS == null){
			// Create visualStyles based on the definition in property files
			InputStream is = ActiveModulesUI.class.getResourceAsStream("/jActiveModules_VS.props");
			ServicesUtil.loadVizmapFileTaskFactory.loadStyles(is);
		}
		populateVS();
	}
	
	private static void populateVS(){
		
		Iterator<VisualStyle> it = ServicesUtil.visualMappingManagerRef.getAllVisualStyles().iterator();
		while (it.hasNext()){
			VisualStyle vs = it.next();
			if (vs.getTitle().equalsIgnoreCase(VS_OVERVIEW_NAME)){
				overviewVS = vs;
			}
			if (vs.getTitle().equalsIgnoreCase(VS_MODULE_NAME)){
				moduleVS = vs;
			}
		}
	}
	
	
	private static boolean eventFired = false;
	
	private static CyLayoutAlgorithm layoutAlgorithm = ServicesUtil.cyLayoutsServiceRef.getLayout("force-directed");
	
	// ----------------------------------------------------------------
	public ActivePaths(CyNetwork cyNetwork, ActivePathFinderParameters apfParams, ActiveModulesUI parentUI) {
		
		this.activeModuleResult = new ActiveModuleResult();
		
		this.apfParams = apfParams;
		
		try {			
			MAX_NETWORK_VIEWS = new Integer(ServicesUtil.cytoscapePropertiesServiceRef.getProperties().getProperty("moduleNetworkViewCreationThreshold")).intValue();			
		}
		catch (Exception e){
			MAX_NETWORK_VIEWS = 5;
		}

		//if (!eventFired){
		//	this.cyEventHelperService.fireEvent();
		//}

		if (cyNetwork == null || cyNetwork.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}

		attrNames = (String[])apfParams.getExpressionAttributes().toArray(new String[0]);
		Arrays.sort(attrNames);
		if (attrNames.length == 0) {
			throw new RuntimeException("No expression data selected!");
		}
//		menubar = ServicesUtil.cySwingApplicationServiceRef.getJMenuBar();
//		mainFrame = ServicesUtil.cySwingApplicationServiceRef.getJFrame();
		this.cyNetwork = cyNetwork;
		this.parentUI = parentUI;
		
	} // ctor

	// --------------------------------------------------------------
	protected void setShowTable(boolean showTable) {
		this.showTable = showTable;
	}
	protected void clearActivePaths() {
		this.activePaths = null;
	}

	public void run(TaskMonitor taskMonitor) {
		
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("Searching active paths....");
		
	    System.gc();
		//long start = System.currentTimeMillis();
		HashMap expressionMap = generateExpressionMap();

		Vector<Component> activePathsVect= new Vector<Component>();

		// run the path finding algorithm
		final ActivePathsFinder apf =
			new ActivePathsFinder(expressionMap, attrNames, cyNetwork, apfParams,
					      randomize ? null : mainFrame, parentUI, activePathsVect);
		
		if (apfParams.getGreedySearch() == false) {
			
		}
		//activePaths = apf.findActivePaths();
		   
		ActivePathsTaskFactory factory = new ActivePathsTaskFactory(apf);
		ServicesUtil.synchronousTaskManagerServiceRef.execute(factory.createTaskIterator());
				
		activePaths = new Component[activePathsVect.size()];
		for (int i=0; i< activePathsVect.size(); i++){
			activePaths[i] = activePathsVect.get(i);
		}				
		
		
		// for debug only
//		System.out.println("activePaths.length ="+ activePaths.length);
//		for (int i=0; i<activePaths.length; i++){
//
//			Component aComp = activePaths[i];
//			System.out.println("\tactivePaths["+i+"].getDisplayNodes().size() ="+aComp.getDisplayNodes().size() + 
//					"\tDisplayScores ="+aComp.getScore());
//		}

		taskMonitor.setProgress(0.4);
		taskMonitor.setStatusMessage("Active paths found....");
		taskMonitor.setStatusMessage("Create subnetwork for each path ...");

		// create nested networks
		//1 . create subnetwork for each path
		CyNetwork[] subnetworks = createSubnetworks();
		
		for (int i=0; i<subnetworks.length; i++){
			ServicesUtil.cyNetworkManagerServiceRef.addNetwork(subnetworks[i]);
		}

		taskMonitor.setStatusMessage("Create an overview network for all nested network...");

		//2. create an overview network for all nested network
		final CyNetwork overview = ServicesUtil.cyNetworkFactoryServiceRef.createNetwork();
		overview.getRow(overview).set("name", "jActiveModules Search Result "+ runCount++);
		
		activeModuleResult.modules = new ArrayList<Module>();
		
		Set<CyNode>  path_nodes = new HashSet<CyNode>();
		for (int i=0; i< subnetworks.length; i++){
			Module module = new Module();
			CyNode newNode = overview.addNode(); //Cytoscape.getCyNode(subnetworks[i].getTitle(), true);
			module.nodeSUID = newNode.getSUID();
			
			overview.getRow(newNode).set("name", subnetworks[i].getRow(subnetworks[i]).get("name", String.class));
			path_nodes.add(newNode);
			newNode.setNetworkPointer(subnetworks[i]);
			module.networkSUID = subnetworks[i].getSUID();
			// create an attribute for this new node
			//Cytoscape.getNodeAttributes().setAttribute(newNode.getIdentifier(), NODE_SCORE, new Double(activePaths[i].getScore()));
			
			if (overview.getDefaultNodeTable().getColumn(NODE_SCORE)== null){
				overview.getDefaultNodeTable().createColumn(NODE_SCORE, Double.class, false);
			}
			overview.getRow(newNode).set(NODE_SCORE, new Double(activePaths[i].getScore()));
			module.activePathScore = activePaths[i].getScore();
			activeModuleResult.modules.add(module);
		}

		ServicesUtil.cyNetworkManagerServiceRef.addNetwork(overview);
		activeModuleResult.overviewNetworkSUID = overview.getSUID();
		
		//Edges indicate that nodes in nested networks exist in both nested networks
		Set<CyEdge>  path_edges = getPathEdges(overview, path_nodes); //new HashSet<CyEdge>();
		//final CyNetwork overview = Cytoscape.createNetwork(path_nodes, path_edges, "jActiveModules Search Result "+ runCount++, cyNetwork, false);

		taskMonitor.setProgress(0.7);

		taskMonitor.setStatusMessage("Create edge attributes...");

		//3. Create an edge attribute "overlapScore", which is defined as NumberOfSharedNodes/min(two network sizes)
		
		overview.getDefaultEdgeTable().createColumn("jActiveModules_nodeCount_min_two", Integer.class, false);
		overview.getDefaultEdgeTable().createColumn("jActiveModules_nodeOverlapCount", Integer.class, false);
		overview.getDefaultEdgeTable().createColumn(EDGE_SCORE, Double.class, false);
		
		CyTable cyEdgeAttrs = this.cyNetwork.getDefaultEdgeTable(); //Cytoscape.getEdgeAttributes();
		Iterator it = path_edges.iterator();
		while(it.hasNext()){
			CyEdge aEdge = (CyEdge) it.next();
			int NumberOfSharedNodes = getNumberOfSharedNodes((CyNetwork)aEdge.getSource().getNetworkPointer(), 
					(CyNetwork)aEdge.getTarget().getNetworkPointer());
			
			int minNodeCount = Math.min(aEdge.getSource().getNetworkPointer().getNodeCount(), 
								aEdge.getTarget().getNetworkPointer().getNodeCount());

			//cyEdgeAttrs.setAttribute(aEdge.getIdentifier(), "jActiveModules_nodeCount_min_two", minNodeCount);
			overview.getRow(aEdge).set("jActiveModules_nodeCount_min_two", minNodeCount);
			
			//cyEdgeAttrs.setAttribute(aEdge.getIdentifier(), "jActiveModules_nodeOverlapCount", NumberOfSharedNodes);
			overview.getRow(aEdge).set("jActiveModules_nodeOverlapCount", NumberOfSharedNodes);
			double overlapScore = (double)NumberOfSharedNodes/minNodeCount;
			//cyEdgeAttrs.setAttribute(aEdge.getIdentifier(), EDGE_SCORE, overlapScore);
			overview.getRow(aEdge).set(EDGE_SCORE, overlapScore);
		}
		taskMonitor.setProgress(0.8);

		taskMonitor.setStatusMessage("Create a view for overview network...");

		//4. Create a view for overview network and apply visual style
		//Cytoscape.createNetworkView(overview, overview.getIdentifier(), tuning(), null);
		final CyNetworkView newView = ServicesUtil.cyNetworkViewFactoryServiceRef.createNetworkView(overview);
		ServicesUtil.cyNetworkViewManagerServiceRef.addNetworkView(newView);
		
		// Apply layout for overview
		Object context = layoutAlgorithm.getDefaultLayoutContext();
		insertTasksAfterCurrentTask(layoutAlgorithm.createTaskIterator(newView, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
			
		ServicesUtil.visualMappingManagerRef.setVisualStyle(overviewVS, newView);
		newView.updateView();

		taskMonitor.setStatusMessage("Create views for top n modules...");

		// Create view for top n modules
		int n = -1;
		try {
			n= new Integer(ServicesUtil.cytoscapePropertiesServiceRef.getProperties().getProperty(ActiveModulesUI.JACTIVEMODULES_TOP_N_MODULE)).intValue();			
		}
		catch(Exception e){
			n= 5;
		}

		 if (n> subnetworks.length){
			 n = subnetworks.length;
		 }

		 for (int i=0; i<n; i++){
				CyNetworkView theView = ServicesUtil.cyNetworkViewFactoryServiceRef.createNetworkView(subnetworks[i]);
				ServicesUtil.cyNetworkViewManagerServiceRef.addNetworkView(theView);
								
				insertTasksAfterCurrentTask(layoutAlgorithm.createTaskIterator(theView, context, CyLayoutAlgorithm.ALL_NODE_VIEWS,""));

				ServicesUtil.visualMappingManagerRef.setVisualStyle(moduleVS, theView);
				theView.updateView();
		 }
		 
		taskMonitor.setProgress(1.0);
	}
	

	private static int getNumberOfSharedNodes(CyNetwork networkA, CyNetwork networkB){
		
		Long[] nodeIndicesA = new Long[networkA.getNodeCount()];
		Long[] nodeIndicesB = new Long[networkB.getNodeCount()];
		
		Iterator<CyNode> it = networkA.getNodeList().iterator();
		int iA=0;
		while (it.hasNext()){
			nodeIndicesA[iA] = it.next().getSUID();
			iA++;
		}
		
		Iterator<CyNode> it2 = networkB.getNodeList().iterator();
		int iB=0;
		while (it2.hasNext()){
			nodeIndicesB[iB] = it2.next().getSUID();
			iB++;
		}
		
		HashSet<Long> hashSet = new HashSet<Long>();
		for (int i=0; i< nodeIndicesA.length; i++){
			hashSet.add( new Long(nodeIndicesA[i]));
		}

		int sharedNodeCount =0;
		for (int i=0; i< nodeIndicesB.length; i++){
			if (hashSet.contains(new Long(nodeIndicesB[i]))){
				sharedNodeCount++;
			}
		}
		
		return sharedNodeCount;
	}
	

	private Set<CyEdge> getPathEdges(CyNetwork overview, Set path_nodes) {
		HashSet<CyEdge> edgeSet = new HashSet<CyEdge>();
		
		Object[] nodes = path_nodes.toArray();
		
		HashSet[] hashSet = new HashSet[nodes.length];
		for (int i=0; i< nodes.length; i++){
			hashSet[i] = new HashSet<CyNode>(((CyNode)nodes[i]).getNetworkPointer().getNodeList());
		}
		
		for (int i=0; i< nodes.length-1; i++){
			for (int j=i+1; j<nodes.length; j++){
				// determine if there are overlap between nested networks
				if (hasTwoSetOverlap(hashSet[i], hashSet[j])){
					//CyEdge edge = Cytoscape.getCyEdge((CyNode)nodes[i], (CyNode)nodes[j], Semantics.INTERACTION, "overlap", true);
					CyEdge newEdge = overview.addEdge((CyNode)nodes[i], (CyNode)nodes[j], false);
					edgeSet.add(newEdge);
				}
			}
		}
		
		return edgeSet;
	}
	
	
	private boolean hasTwoSetOverlap(HashSet<CyNode> set1, HashSet<CyNode> set2) {
		Iterator<CyNode> it = set1.iterator();
		while (it.hasNext()){
			if (set2.contains(it.next())){
				return true;
			}
		}		
		return false;
	}
	
	
	private CySubNetwork[] createSubnetworks() {
		//CyNetwork[] subnetworks = new CyNetwork[activePaths.length];

		CySubNetwork[] subnetworks2 = new CySubNetwork[activePaths.length];
		
		CyRootNetwork rootNetwork = ServicesUtil.cyRootNetworkFactory.getRootNetwork(cyNetwork);
		
		for (int i = 0; i < activePaths.length; i++) {
			Component thePath = activePaths[i];
			String pathName = "Module_" + runCount + "_" + (i + 1);
			
			// get nodes for this path
			Vector nodeVect = (Vector) thePath.getDisplayNodes();
			Set<CyNode> nodeSet = new HashSet<CyNode>();
			for (int j = 0; j < nodeVect.size(); j++) {
				CyNode oneNode = (CyNode) nodeVect.elementAt(j);
				if (oneNode != null)
					nodeSet.add(oneNode);
			}
			
			// get edges for this path
			Set edgeSet = new HashSet();
			Iterator iterator = cyNetwork.getEdgeList().iterator(); //.edgesIterator();
			while (iterator.hasNext()) {
				CyEdge edge = (CyEdge) iterator.next();
				if (nodeSet.contains(edge.getSource()) && nodeSet.contains(edge.getTarget()))
					edgeSet.add(edge);
			}

			//subnetworks[i] = Cytoscape.createNetwork(nodeSet, edgeSet, pathName, cyNetwork, false);
			subnetworks2[i] = rootNetwork.addSubNetwork(nodeSet, edgeSet);
			subnetworks2[i].getRow(subnetworks2[i]).set("name", pathName);
			
//			if(i < MAX_NETWORK_VIEWS) {
//				//final CyNetworkView moduleView = Cytoscape.createNetworkView(subnetworks[i], subnetworks[i].getTitle(), tuning());
//				final CyNetworkView moduleView = this.cyNetworkViewManager.getNetworkView(subnetworks2[i].getSUID());
//				//moduleView.setVisualStyle(moduleVS.getTitle()); //.getName());
//
////				moduleView.updateView(); //.redrawGraph(false, true);
//			} 
		}
		
		return subnetworks2;
	}
	
	/**
	 * Returns the best scoring path from the last run. This is mostly used by
	 * the score distribution when calculating the distribution
	 */
	protected Component getHighScoringPath() {
		logger.info("High Scoring Path: " + activePaths[0].toString());
		logger.info("Score: " + activePaths[0].getScore());
		int size = activePaths[0].getNodes().size();
		logger.info("Size: " + size);
		logger.info("Raw score: " + activePaths[0].calculateSimpleScore());
		logger.info("Mean: " + Component.pStats.getMean(size));
		logger.info("Std: " + Component.pStats.getStd(size));
		return activePaths[0];
	}

	protected HashMap generateExpressionMap() {
		// set up the HashMap which is used to map from nodes
		// to z values. At this point, we are mapping from the
		// p values for expression to z values
		logger.info("Processing Expression Data into Hash");
		HashMap tempHash = new HashMap();
		logger.info("Do some testing of the ExpressionData object");
		CyTable nodeAttributes = this.cyNetwork.getDefaultNodeTable();

		// Create two identical lists of genes
		List<CyNode> geneList = new ArrayList<CyNode>();
		List<CyNode> shuffledList = new ArrayList<CyNode>();
		for (Iterator nodeIt = cyNetwork.getNodeList().iterator() /*.nodesIterator()*/; nodeIt.hasNext();) {
			CyNode n = (CyNode) nodeIt.next();
			geneList.add(n);
			shuffledList.add(n);
		}

		// If randomize, permute the second list of genes
		if ( randomize ) 
			Collections.shuffle(shuffledList);

		final Double[][] attribValues = new Double[attrNames.length][geneList.size()];
		final Map<String, Integer> geneNameToIndexMap = new HashMap<String, Integer>();
		for (int i = 0; i < geneList.size(); i++) {
			final String geneName = this.cyNetwork.getRow(geneList.get(i)).get("name", String.class); //.getIdentifier();
			
			geneNameToIndexMap.put(geneName, new Integer(i));
			for (int j = 0; j < attrNames.length; j++)
			{
				//attribValues[j][i] = nodeAttributes.get.getDoubleAttribute(geneName, attrNames[j]);
				Collection<CyRow> rows = nodeAttributes.getMatchingRows("name", geneName);
				Object[] objs = rows.toArray();
				// We assume name is unique, we expect one row returned for each name
				CyRow row = (CyRow) objs[0];
				attribValues[j][i] = row.get(attrNames[j], Double.class);
			}
		}

		// Perform the scaling:
		for (int j = 0; j < attrNames.length; j++) {
			final int index = apfParams.getExpressionAttributes().indexOf(attrNames[j]);
			final ScalingMethodX scalingMethod = ScalingMethodX.getEnumValue(apfParams.getScalingMethods().get(index));
			attribValues[j] = scaleInputValues(attribValues[j], scalingMethod);
		}

		for (int i = 0; i < geneList.size(); i++) {
		
			// If not randomizing these will be identical.
			CyNode current = geneList.get(i); 
			CyNode shuffle = shuffledList.get(i);

			// If randomizing, you'll get p-values for a different gene. 
			String canonicalName = this.cyNetwork.getRow(shuffle).get("name", String.class); //.getIdentifier();

			double[] tempArray = new double[attrNames.length];
			for (int j = 0; j < attrNames.length; j++) {
				final Double d = attribValues[j][geneNameToIndexMap.get(canonicalName)];
				if (d == null)
					tempArray[j] = ZStatistics.oneMinusNormalCDFInverse(.5);
				else {
					double sigValue = d.doubleValue();
					if (sigValue < MIN_SIG) {
						sigValue = MIN_SIG;
						logger.warn("Warning: value for " + this.cyNetwork.getRow(current).get("name", String.class)+ //.getIdentifier() + 
						                   " (" + canonicalName + ") adjusted to " + MIN_SIG);
					} 
					if (sigValue > MAX_SIG) {
						sigValue = MAX_SIG;
						logger.warn("Warning: value for " + this.cyNetwork.getRow(current).get("name", String.class)+ //.getIdentifier() + 
						                   " (" + canonicalName + ") adjusted to " + MAX_SIG);
					} 

					// transform the p-value into a z-value and store it in the
					// array of z scores for this particular node
					tempArray[j] = ZStatistics.oneMinusNormalCDFInverse(sigValue);
				}
			}
			tempHash.put(current, tempArray);
		}
		logger.info("Done processing into Hash");
		return tempHash;
	}

	private Double[] scaleInputValues(final Double[] inputValues, final ScalingMethodX scalingMethod) {
		if (scalingMethod == ScalingMethodX.NONE)
			return inputValues;

		int nullCount = 0;
		for (final Double inputValue : inputValues) {
			if (inputValue == null)
				++nullCount;
		}
		if (nullCount == inputValues.length)
			return null;

		final double[] unscaledValues = new double[inputValues.length - nullCount];
		int i = 0;
		for (final Double inputValue : inputValues) {
			if (inputValue != null)
				unscaledValues[i++] = inputValue;
		}

		if (scalingMethod == ScalingMethodX.RANK_LOWER || scalingMethod == ScalingMethodX.LINEAR_LOWER) {
			for (int k = 0; k < unscaledValues.length; ++k)
				unscaledValues[k] = -unscaledValues[k];
		}

		final String type;
		final double from, to;
		if (scalingMethod == ScalingMethodX.RANK_LOWER || scalingMethod == ScalingMethodX.RANK_UPPER) {
			from = 0.0;
			to   = 1.0;
			type = "rank";
		} else if (scalingMethod == ScalingMethodX.LINEAR_LOWER || scalingMethod == ScalingMethodX.LINEAR_UPPER) {
			final double EPS = 0.5 / unscaledValues.length;
			from = 0.0 + EPS;
			to   = 1.0 - EPS;
			type = "linear";
		} else
			throw new IllegalArgumentException("unknown scaling method: " + scalingMethod);

		final double[] scaledValues;
		try {
			scaledValues = ScalerFactory.getScaler(type).scale(unscaledValues, from, to);
		} catch (final IllegalArgumentException e) {
			logger.warn("Scaling failed: " + e.getMessage());
			return null;
		}

		final Double[] outputValues = new Double[inputValues.length];
		int k = 0;
		i = 0;
		for (final Double inputValue : inputValues) {
			if (inputValue == null)
				outputValues[k++] = null;
			else
				outputValues[k++] = scaledValues[i++];
		}

		return outputValues;
	}

	/**
	 * Scores the currently selected nodes in the graph, and pops up a window
	 * with the result
	 */
//	protected void scoreActivePath() {
//		String callerID = "jActiveModules";
//		ActivePathsFinder apf = new ActivePathsFinder(generateExpressionMap(),
//				attrNames, cyNetwork, apfParams, mainFrame, parentUI);
//
//		long start = System.currentTimeMillis();
//		Vector result = new Vector();
//		Iterator it = SelectUtil.getSelectedNodes(cyNetwork).iterator();
//		while (it.hasNext()) {
//			result.add(it.next());
//		}
//
//		double score = apf.scoreList(result);
//		long duration = System.currentTimeMillis() - start;
//		logger.info("-------------- back from score: " + duration
//				+ " msecs");
//		logger.info("-------------- score: " + score + " \n");
//		JOptionPane.showMessageDialog(mainFrame, "Score: " + score);
//	} // scoreActivePath

	/*
	protected class ActivePathControllerLauncherAction extends AbstractAction {
		ActivePathControllerLauncherAction() {
			super("Active Modules");
		} // ctor
		public void actionPerformed(ActionEvent e) {
			//showConditionsVsPathwaysTable();
		}
	}
	*/

	/**
	 * find all of the unique node names in the full set of active paths. there
	 * may be duplicates since some nodes may appear in several paths
	 */
	protected Vector combinePaths(Component[] activePaths) {
		HashSet set = new HashSet();
		for (int i = 0; i < activePaths.length; i++) {
			set.addAll(activePaths[i].getNodes());
		} // for i
		return new Vector(set);

	}

	protected void addActivePathToolbarButton() {
		// if (activePathToolbarButton != null)
		// cytoscapeWindow.getCyMenus().getToolBar().remove
		// (activePathToolbarButton);

		// activePathToolbarButton =
		// cytoscapeWindow.getCyMenus().getToolBar().add (new
		// ActivePathControllerLauncherAction ());

	} // addActivePathToolbarButton
	// ------------------------------------------------------------------------------
	public void displayPath(Component activePath, boolean clearOthersFirst, String pathTitle) {
		titleForCurrentSelection = pathTitle;
		//SelectFilter filter = cyNetwork.getSelectFilter();
		// cytoscapeWindow.selectNodesByName (activePath.getNodes (),
		// clearOthersFirst);
		if (clearOthersFirst) {
			// cyNetwork.unFlagAllNodes();
			//filter.unselectAllNodes();
			Iterator<CyNode> it = this.cyNetwork.getNodeList().iterator();
			while (it.hasNext()){
				CyNode node = it.next();
				this.cyNetwork.getRow(node).set(CyNetwork.SELECTED, false);
			}
		}
		//filter.setSelectedNodes(activePath.getDisplayNodesGeneric(), true);
		Iterator<CyNode> it2 = activePath.getDisplayNodesGeneric().iterator();
		while (it2.hasNext()){
			CyNode node2 = it2.next();
			this.cyNetwork.getRow(node2).set(CyNetwork.SELECTED, true);
		}
		
		// cyNetwork.setFlaggedNodes(activePath.getNodes(),true);
		
		Collection<CyNetworkView> views = ServicesUtil.cyNetworkViewManagerServiceRef.getNetworkViews(ServicesUtil.cyApplicationManagerServiceRef.getCurrentNetwork());
		Iterator<CyNetworkView> it = views.iterator();
		it.next().updateView();
		//Cytoscape.getCurrentNetworkView().redrawGraph(false, true);
	}
	// ------------------------------------------------------------------------------
	public void displayPath(Component activePath,
			String pathTitle) {
		displayPath(activePath, true, pathTitle);
	}
	// ------------------------------------------------------------------------------

	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class))
		{
			return (R)("Success.");
		} else if (type.equals(ActiveModuleAnnealResult.class)){
			if (apfParams.getGreedySearch()) {
				throw new IllegalArgumentException();
			}
			ActiveModuleAnnealResult result = new ActiveModuleAnnealResult();
			result.overviewNetworkSUID = activeModuleResult.overviewNetworkSUID;
			result.modules = activeModuleResult.modules;
			result.seed = apfParams.getRandomSeed();
			return (R) result;
		} else if (type.equals(ActiveModuleResult.class)){
			return (R) activeModuleResult;
		}
		return null;
	}
	
	/*
	private CyLayoutAlgorithm tuning() {
		final CyLayoutAlgorithm fd = layoutAlgorithm;
	
		fd.getSettings().get("defaultSpringLength").setValue("140");
		fd.getSettings().get("defaultNodeMass").setValue("9");
		fd.getSettings().updateValues();
		fd.updateSettings();
		
		return fd;
	}
*/
} // class ActivePaths (a CytoscapeWindow plugin)
