package model;

import java.util.List;

public class ConfigModel {

	public String gitUserName;
	public String gitPassword;
	public List<EC2> ec2;
	public String snapshot;

	@Override
	public String toString() {
		return "ConfigModel [ec2=" + ec2 + "]";
	}
}
