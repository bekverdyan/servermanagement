package model;

import java.util.List;
import java.util.Map;

public class EC2 {
	public String id;
	public String name;
	public String rds;
	public String ip;
	public Map<String, String> branches;
	public List<ReplaceConfig> replaceFiles;


	@Override
	public String toString() {
		return "EC2 [id=" + id + ", name=" + name + ", rds=" + rds + ", ip="
				+ ip + "]";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getBranches() {
		return branches;
	}

	public void setBranches(Map<String, String> branches) {
		this.branches = branches;
	}
	
	
}