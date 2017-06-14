package csplugins.jActiveModules.rest.models;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel()
public class Attribute {
	@ApiModelProperty(value = "A node column name", example="gal80Rexp")
	public String columnName;
	@ApiModelProperty(value = "Reverse the scaling", example="false")
	public Boolean reverseSignature;
	@ApiModelProperty(value = "Scaling", example="rank/upper", allowableValues="none (prescaled),linear/lower,linear/upper,rank/lower,rank/upper")
	public String scaling;
}
