package main.java.servermanagement.util;

import java.io.File;
import java.io.IOException;

import model.EC2;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;

public class EC2Manager {

	private final static Logger logger = Logger.getLogger(EC2Manager.class);

	private static final String CREDENTIAL_PATH = "/home/sergeyhlghatyan/ssh_keys/aws/credentials";

	public EC2Manager() {
	}

	public String startEc2Instance(EC2 ec2) {
		AmazonEC2AsyncClient client = getClient();
		String ip = startServer(client, ec2);
		client.shutdown();
		return ip;
	}

	private AmazonEC2AsyncClient getClient() {
		AWSCredentials credentials;
		AmazonEC2AsyncClient client = null;
		try {
			credentials = new PropertiesCredentials(new File(CREDENTIAL_PATH));
			client = new AmazonEC2AsyncClient(credentials);
			client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
		} catch (IllegalArgumentException | IOException e) {
			logger.error(e);
		}

		return client;
	}

	private String startServer(AmazonEC2Async client, EC2 ec2) {
		String ip = null;
		try {

			StartInstancesRequest startRequest = new StartInstancesRequest()
					.withInstanceIds(ec2.id);

			@SuppressWarnings("unused")
			StartInstancesResult startResult = client
					.startInstances(startRequest);

			DescribeInstancesResult describeInstanceResult = null;
			Integer instanceState = -1;
			while (instanceState != 16) { // Loop until the instance is in the
											// "running" state.
				describeInstanceResult = getInstanceStatus(ec2.id, client);

				instanceState = describeInstanceResult.getReservations().get(0)
						.getInstances().get(0).getState().getCode();

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}

			logger.debug(ec2.name + " started");

			ip = describeInstanceResult
					.getReservations()
					.get(0)
					.getInstances()
					.stream()
					.filter(instance -> instance.getTags().stream()
							.filter(tag -> tag.getKey().equals("Name"))
							.findFirst().get().getValue().equals(ec2.name))
					.findFirst().get().getPublicIpAddress();

		} catch (Exception e) {
			logger.error(e);
		}

		return ip;
	}

	// 0 : pending
	// 16 : running
	// 32 : shutting-down
	// 48 : terminated
	// 64 : stopping
	// 80 : stopped

	public DescribeInstancesResult getInstanceStatus(String instanceId,
			AmazonEC2Async ec2) {
		DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest()
				.withInstanceIds(instanceId);
		DescribeInstancesResult describeInstanceResult = ec2
				.describeInstances(describeInstanceRequest);
		//
		// InstanceState state = describeInstanceResult.getReservations().get(0)
		// .getInstances().get(0).getState();
		//
		//
		// String ip = describeInstanceResult.getReservations().get(0)
		// .getInstances().get(0).getPublicIpAddress();

		return describeInstanceResult;
	}

	public void stopEc2Instance(EC2 ec2) {
		AmazonEC2AsyncClient client = getClient();
		stopServer(client, ec2);
	}

	private void stopServer(AmazonEC2Async client, EC2 ec2) {
		StopInstancesRequest stopRequest = new StopInstancesRequest()
				.withInstanceIds(ec2.id);
		client.stopInstances(stopRequest);
	}
}
