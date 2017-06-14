package csplugins.jActiveModules.rest.models;

import io.swagger.annotations.ApiModelProperty;

public class AnnealStrategyParameters extends GeneralParameters {
	@ApiModelProperty(value = "Iterations", example="2500", allowableValues="range[0,100000000]",required=true)
	public Integer iterations;
	
	@ApiModelProperty(value = "Start Temperature", example="1.0", allowableValues="range[0.0001,100]",required=true)
	public Double startTemp;
	
	@ApiModelProperty(value = "End Temperature (must be lower than start)", example="1.0", allowableValues="range[0.0001,100]",required=true)
	public Double endTemp;
	
	@ApiModelProperty(value = "Quenching", example="true", required=true)
	public Boolean quenching;
	
	@ApiModelProperty(value = "Hubfinding", required=true, allowableValues="range[0,10000]")
	public Integer hubfinding;
	
	@ApiModelProperty(value="Non-Random Seed. Leaving this parameter out generates a seed value from the current time.", example="17", required=false)
	public Integer seed;
}
