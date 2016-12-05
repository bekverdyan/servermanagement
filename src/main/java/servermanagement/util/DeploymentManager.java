package main.java.servermanagement.util;

import java.util.Map;

import org.apache.log4j.Logger;

import model.ConfigModel;

public class DeploymentManager {
	
	private final static Logger logger = Logger.getLogger(DeploymentManager.class);
	
	public static void main(String[] args){
		
		ConfigModel config = ConfigUtils.loadConfigJson();
		
		EC2Manager ec2Manager = new EC2Manager(config);
		Map<String,String> ec2NameIpMap = ec2Manager.startEc2Instances();
		
		try (final ShellExecuter shellExecuter = new ShellExecuter(config)) {

			shellExecuter.buildAndDeploy(ec2NameIpMap);
			
		} catch (Exception e) {
			logger.error(e);
		}
	}
}
