package csplugins.jActiveModules.rest.models;

import io.swagger.annotations.ApiModelProperty;

public class ActiveModuleAnnealResult extends ActiveModuleResult{
	@ApiModelProperty(value = "Seed value for deterministic behavior")
	public Integer seed;
}
