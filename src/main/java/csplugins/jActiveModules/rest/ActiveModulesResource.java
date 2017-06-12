package csplugins.jActiveModules.rest;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.model.CyNetwork;

import csplugins.jActiveModules.ActiveModulesUI;
import csplugins.jActiveModules.ActivePaths;
import csplugins.jActiveModules.ActivePathsTaskFactory;
import csplugins.jActiveModules.ServicesUtil;
import csplugins.jActiveModules.data.ActivePathFinderParameters;
import csplugins.jActiveModules.rest.models.AnnealStrategyParameters;
import csplugins.jActiveModules.rest.models.Attribute;
import csplugins.jActiveModules.rest.models.GeneralParameters;
import csplugins.jActiveModules.rest.models.SearchStrategyParameters;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"Apps: jActiveModules"})
@Path("/jactivemodules/v1/")
public class ActiveModulesResource {

	public final ActiveModulesUI activeModulesUI;

	public final CyApplicationManager cyApplicationManager;
	public final CIErrorFactory ciErrorFactory;
	public final CIExceptionFactory ciExceptionFactory;

	private final static String resourceErrorRoot = "urn:cytoscape:ci:jactivemodules:v1";

	public ActiveModulesResource(CyApplicationManager cyApplicationManager, CIErrorFactory ciErrorFactory, CIExceptionFactory ciExceptionFactory, ActiveModulesUI activeModulesUI) {
		this.cyApplicationManager = cyApplicationManager;
		this.ciErrorFactory = ciErrorFactory;
		this.ciExceptionFactory = ciExceptionFactory;
		this.activeModulesUI = activeModulesUI;
	}

	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("{networkSUID}/findModulesWithSearchStrategy")
	@ApiOperation(value = "Execute jActiveModules using a Search Strategy")
	public void executeActivePaths(@PathParam(value="networkSUID") Long networkSUID, SearchStrategyParameters params) {
		CyNetwork cyNetwork = cyApplicationManager.getCurrentNetwork();

		if (cyNetwork == null) {
			String messageString = "Could not find current Network";
			throw ciExceptionFactory.getCIException(404, new CIError[]{ciErrorFactory.getCIError(404, resourceErrorRoot + ":" + "findModulesWithSearchStrategy"+ ":"+ 1, messageString)});		
		}

		ActivePaths activePaths;
		
		ActivePathFinderParameters activePathFinderParameters = getApfParams(params);
		activePathFinderParameters.setNetwork(cyNetwork);
		try {
			activePaths = new ActivePaths(cyNetwork, getApfParams(params), activeModulesUI);
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			JOptionPane.showMessageDialog(ServicesUtil.cySwingApplicationServiceRef.getJFrame(),
					"Error running jActiveModules (1)!  " + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ActivePathsTaskFactory factory = new ActivePathsTaskFactory(activePaths);
		ServicesUtil.taskManagerServiceRef.execute(factory.createTaskIterator());
	}

	/*
	private ActivePathFinderParameters getApfParams() {
		ActivePathFinderParameters activePathFinderParameters = new ActivePathFinderParameters();
		activePathFinderParameters.setInitialTemperature(0);
		activePathFinderParameters.setFinalTemperature(0);
		activePathFinderParameters.setHubAdjustment(0);
		activePathFinderParameters.setTotalIterations(1000);
		activePathFinderParameters.setNumberOfPaths(0);
		activePathFinderParameters.setDisplayInterval(0);
		activePathFinderParameters.setMinHubSize(0);
		activePathFinderParameters.setRandomSeed(0);
		activePathFinderParameters.setSearchDepth(0);
		activePathFinderParameters.setMaxDepth(0);
		activePathFinderParameters.setToQuench(0);
		activePathFinderParameters.setToUseMCFile();
		activePathFinderParameters.setMCboolean();
		activePathFinderParameters.setMcFileName();
		activePathFinderParameters.setRegionalBoolean(false);
		activePathFinderParameters.setSearchFromNodes(false);
		activePathFinderParameters.setDefault(false);
		this.maxThreads = oldAPFP.getMaxThreads();
		this.exit = oldAPFP.getExit();
		this.save = oldAPFP.getSave();
		this.outputFile = oldAPFP.getOutputFile();
		this.greedySearch = oldAPFP.getGreedySearch();
		this.enableMaxDepth = oldAPFP.getEnableMaxDepth();
		this.run = oldAPFP.getRun();
		this.randomizeExpression = oldAPFP.getRandomizeExpression();
		this.randomIterations = oldAPFP.getRandomIterations();
		this.overlapThreshold = oldAPFP.getOverlapThreshold();
	}
	*/
	
	private void setAttributes(GeneralParameters generalParameters, ActivePathFinderParameters params) {
		List<String> attributeNames = new ArrayList<String>();
		List<Boolean> reverseSignature = new ArrayList<Boolean>();
		List<String> scaling = new ArrayList<String>();
		for (Attribute attribute : generalParameters.nodeAttributes) {
			attributeNames.add(attribute.columnName);
			reverseSignature.add(attribute.reverseSignature);
			scaling.add(attribute.scaling);
		}
		params.setExpressionAttributes(attributeNames);
		params.setScalingMethods(scaling);
		params.setSwitchSigs(reverseSignature);
	}
	
	//Greedy Search
	private  ActivePathFinderParameters getApfParams(SearchStrategyParameters params) {
		ActivePathFinderParameters activePathFinderParameters = new ActivePathFinderParameters();
		activePathFinderParameters.setGreedySearch(true);
		
		setAttributes(params, activePathFinderParameters);
		
		return activePathFinderParameters;
	}
	
	private ActivePathFinderParameters getApfParams(AnnealStrategyParameters params) {
		ActivePathFinderParameters activePathFinderParameters = new ActivePathFinderParameters();
		activePathFinderParameters.setGreedySearch(false);
		return activePathFinderParameters;
	}
}
