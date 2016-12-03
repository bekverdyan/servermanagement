package model;

import java.util.List;
import java.util.Map;

public class ConfigModel {

	public static class EC2 {
		public String id;
		public Map<String, String> branches;

		@Override
		public String toString() {
			return "EC2 [id=" + id + ", branches=" + branches + "]";
		}
	}

	public String gitUserName;
	public String gitPassword;
	public List<EC2> ec2;

	@Override
	public String toString() {
		return "ConfigModel [ec2=" + ec2 + "]";
	}
}
