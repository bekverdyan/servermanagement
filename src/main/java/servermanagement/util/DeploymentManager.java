package main.java.servermanagement.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import model.ConfigModel;
import model.EC2;

import org.apache.log4j.Logger;

public class DeploymentManager {

	private static final String CONFIG_FILE_NAME = "config.js";
	private final static Logger logger = Logger
			.getLogger(DeploymentManager.class);

	public static void main(String[] args) {
		ConfigModel configModel = ConfigUtils.loadConfigJson(CONFIG_FILE_NAME);

		List<String> ec2List = new ArrayList<String>();
		ec2List.add("NABS-QA-31");

		List<EC2> ecToProcess = configModel.ec2.stream()
				.filter(ec -> ec2List.contains(ec.name))
				.collect(Collectors.toList());

		boolean buildAndDeploy = false;

		start(configModel, ecToProcess, buildAndDeploy);
		//stop(configModel,ecToProcess);
	}

	public static void start(ConfigModel configModel, List<EC2> ecToProcess,
			boolean buildAndDeploy) {

		EC2Manager ec2Manager = new EC2Manager();

		RDSManager rdsManager = new RDSManager();

		ThreadFactory service = Executors.defaultThreadFactory();

		ecToProcess
				.forEach(ec2 -> {
					service.newThread(
							() -> {
								try {

									rdsManager.startRDSInstance(ec2,
											configModel.snapshot);
									ec2.ip = ec2Manager.startEc2Instance(ec2);

									try (final ShellExecuter shellExecuter = new ShellExecuter(
											configModel)) {
										if (buildAndDeploy) {
											shellExecuter.buildAndDeploy(ec2);
										} else {
											shellExecuter.deploy(ec2);
										}
									} catch (Exception e) {
										logger.error(e);
									}
								} catch (Exception e) {
									logger.error(e);
								}

							}).start();
				});
	}

	public static void stop(ConfigModel configModel, List<EC2> ecToProcess) {

		EC2Manager ec2Manager = new EC2Manager();

		RDSManager rdsManager = new RDSManager();

		ThreadFactory service = Executors.defaultThreadFactory();

		ecToProcess.forEach(ec2 -> {
			service.newThread(() -> {
				try {
					rdsManager.stopRDSInstance(ec2);
					ec2Manager.stopEc2Instance(ec2);
				} catch (Exception e) {
					logger.error(e);
				}
			}).start();
		});
	}
}
