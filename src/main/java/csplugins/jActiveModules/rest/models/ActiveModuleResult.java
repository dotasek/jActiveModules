package csplugins.jActiveModules.rest.models;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ActiveModuleResult {
	@ApiModelProperty(value = "Network SUID of the overview network")
	public Long overviewNetworkSUID;
	@ApiModelProperty(value = "A list of generated modules")
	public List<Module> modules;
}
