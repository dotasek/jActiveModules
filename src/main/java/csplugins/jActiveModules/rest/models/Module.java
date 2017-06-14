package csplugins.jActiveModules.rest.models;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class Module {
	@ApiModelProperty(value = "Node SUID of the module in the overview network")
	public Long nodeSUID;
	@ApiModelProperty(value = "Network SUID of the module network")
	public Long networkSUID;
	@ApiModelProperty(value = "Module Active Path Score")
	public Double activePathScore;
}
