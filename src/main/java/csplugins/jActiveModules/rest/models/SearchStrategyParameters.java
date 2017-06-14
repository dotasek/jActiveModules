package csplugins.jActiveModules.rest.models;

import io.swagger.annotations.ApiModelProperty;

public class SearchStrategyParameters extends GeneralParameters {
	@ApiModelProperty(value = "Search Depth", example="1", required=true)
	public Integer searchDepth;
	@ApiModelProperty(value = "Search from Selected Nodes", example="false", required=true)
	public Boolean searchFromSelectedNodes;
	@ApiModelProperty(value = "Max Depth from Start Nodes", required=false)
	public Integer maxDepthFromStartNodes;
}
