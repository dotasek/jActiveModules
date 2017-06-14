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
import csplugins.jActiveModules.dialogs.ActivePathsParameterPanel;
import csplugins.jActiveModules.rest.models.ActiveModuleAnnealResult;
import csplugins.jActiveModules.rest.models.ActiveModuleResult;
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
	public ActiveModuleResult executeSearchStrategy(@PathParam(value="networkSUID") Long networkSUID, SearchStrategyParameters params) {
		return executeActivePaths(networkSUID, params);
	}

	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("{networkSUID}/findModulesWithAnnealStrategy")
	@ApiOperation(value = "Execute jActiveModules using an Annealing Strategy")
	public ActiveModuleResult executeAnnealStrategy(@PathParam(value="networkSUID") Long networkSUID, AnnealStrategyParameters params) {
		return executeActivePaths(networkSUID, params);
	}
	
	
	public <K extends GeneralParameters> ActiveModuleResult executeActivePaths(Long networkSUID, K params) {
		CyNetwork cyNetwork = cyApplicationManager.getCurrentNetwork();

		if (cyNetwork == null) {
			String messageString = "Could not find current Network";
			throw ciExceptionFactory.getCIException(404, new CIError[]{ciErrorFactory.getCIError(404, resourceErrorRoot + ":" + "findModulesWithSearchStrategy"+ ":"+ 1, messageString)});		
		}

		ActivePathFinderParameters activePathFinderParameters;
		Class<? extends ActiveModuleResult> resultClass;
		if (params instanceof SearchStrategyParameters) {
			activePathFinderParameters = getApfParams((SearchStrategyParameters)params);
			resultClass = ActiveModuleResult.class;
		} else if (params instanceof AnnealStrategyParameters) {
			activePathFinderParameters = getApfParams((AnnealStrategyParameters)params);
			resultClass = ActiveModuleAnnealResult.class;
		} else {
			throw new IllegalArgumentException();
		}
		activePathFinderParameters.setNetwork(cyNetwork);
		
		ActiveModulesTaskObserver activeModulesTaskObserver = new ActiveModulesTaskObserver("", "", resultClass);
		
		ActivePaths activePaths;
		
		
		try {
			activePaths = new ActivePaths(cyNetwork, activePathFinderParameters, activeModulesUI);
			ActivePathsTaskFactory factory = new ActivePathsTaskFactory(activePaths);
			ServicesUtil.synchronousTaskManagerServiceRef.execute(factory.createTaskIterator(), activeModulesTaskObserver);
			return activeModulesTaskObserver.activeModuleResult;
		} catch (final Exception e) {
			throw ciExceptionFactory.getCIException(500, new CIError[]{ciErrorFactory.getCIError(500, "", "")});
		}
	}
	
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
		
		activePathFinderParameters.setMaxDepth(params.maxDepthFromStartNodes);
		activePathFinderParameters.setSearchDepth(params.searchDepth);
		activePathFinderParameters.setSearchFromNodes(params.searchFromSelectedNodes);
		
		return activePathFinderParameters;
	}
	
	private ActivePathFinderParameters getApfParams(AnnealStrategyParameters params) {
		ActivePathFinderParameters activePathFinderParameters = new ActivePathFinderParameters();
		activePathFinderParameters.setGreedySearch(false);
		
		setAttributes(params, activePathFinderParameters);
		
		
		activePathFinderParameters.setTotalIterations(params.iterations);
		activePathFinderParameters.setInitialTemperature(params.startTemp);
		activePathFinderParameters.setFinalTemperature(params.endTemp);
		activePathFinderParameters.setToQuench(params.quenching);
		activePathFinderParameters.setMinHubSize(params.hubfinding);
		if (params.seed != null) {
			activePathFinderParameters.setRandomSeed(params.seed);
		} else {
			activePathFinderParameters.setRandomSeed(ActivePathsParameterPanel.currentTimeSeed());
		}
		return activePathFinderParameters;
	}
}
