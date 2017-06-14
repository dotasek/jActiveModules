package csplugins.jActiveModules.rest;

import java.util.ArrayList;

import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskObserver;

import csplugins.jActiveModules.rest.models.ActiveModuleResult;

public class ActiveModulesTaskObserver<K extends ActiveModuleResult> implements TaskObserver {
	
	CIResponse<?> response;
	public CIResponse<?> getResponse() {
		return response;
	}

	K activeModuleResult;
	private String resourcePath;
	private String errorCode;

	private final Class<K> resultClass;
	
	public ActiveModulesTaskObserver(String resourcePath, String errorCode, Class<K> resultClass){
		response = null;
		this.resourcePath = resourcePath;
		this.errorCode = errorCode;
		this.resultClass = resultClass;
	}

	@Override
	public void allFinished(FinishStatus arg0) {

		if (arg0.getType() == FinishStatus.Type.SUCCEEDED || arg0.getType() == FinishStatus.Type.CANCELLED) {
			response = new CIResponse<ActiveModuleResult>();
			((CIResponse<ActiveModuleResult>)response).data = activeModuleResult;
			response.errors = new ArrayList<CIError>();
		}
		else {
			//response = this.diffusionResource.buildCIErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resourcePath, errorCode, arg0.getException().getMessage(), arg0.getException());
		}
	}

	@Override
	public void taskFinished(ObservableTask arg0) {
		K jsonResult = arg0.getResults(resultClass);
		activeModuleResult = jsonResult;	
	}
}