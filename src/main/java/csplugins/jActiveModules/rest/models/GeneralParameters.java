package csplugins.jActiveModules.rest.models;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
@ApiModel
public class GeneralParameters {
	
	public List<Attribute> nodeAttributes;
	@ApiModelProperty(value = "Number of Modules", example="5", allowableValues="range[1,1000]", required=true)
	public Integer numberOfModules;
	@ApiModelProperty(value = "Overlap Threshold", example="0.1", allowableValues="range[0,1.0]", required=true)
	public Double overlapThreshold;
	@ApiModelProperty(value = "Adjust Score for Size", example="true", required=true)
	public Boolean adjustScoreForSize;
	@ApiModelProperty(value = "Use Regional Scoring", example="true", required=true)
	public Boolean useRegionalScoring;
}
