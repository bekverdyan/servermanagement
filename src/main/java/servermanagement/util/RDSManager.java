package main.java.servermanagement.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import model.EC2;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest;

public class RDSManager {

	private final static Logger logger = Logger.getLogger(RDSManager.class);

	private static final String CREDENTIAL_PATH = "/home/sergeyhlghatyan/ssh_keys/aws/credentials";

	public RDSManager() {
	}

	public String startRDSInstance(EC2 ec2, String snapshot) throws Exception {
		String endpoint = null;
		AmazonRDSClient rdsClient = getClient();
		endpoint = restoreDBFromSnapshot(rdsClient, ec2, snapshot);
		rdsClient.shutdown();

		return endpoint;
	}

	private AmazonRDSAsyncClient getClient() {
		AmazonRDSAsyncClient rdsClient = null;
		try {
			AWSCredentials credentials = new PropertiesCredentials(new File(
					CREDENTIAL_PATH));
			rdsClient = new AmazonRDSAsyncClient(credentials);
			rdsClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}

		return rdsClient;
	}

	public String restoreDBFromSnapshot(AmazonRDSClient rds, EC2 ec2,
			String snapshotIdentifier) throws Exception {

		String endpoint = null;
		try {

			DescribeDBInstancesRequest dbInstancesRequest = new DescribeDBInstancesRequest()
					.withDBInstanceIdentifier(ec2.rds);

			DescribeDBInstancesResult describeDBInstancesResult = rds
					.describeDBInstances(dbInstancesRequest);
			String instanceState = describeDBInstancesResult.getDBInstances()
					.get(0).getDBInstanceStatus();

			endpoint = describeDBInstancesResult.getDBInstances().get(0)
					.getEndpoint().getAddress();

			if (!instanceState.equals("available")) {
				try {

					RestoreDBInstanceFromDBSnapshotRequest restoreRequest = new RestoreDBInstanceFromDBSnapshotRequest(
							ec2.rds, snapshotIdentifier)
							.withDBInstanceClass("db.r3.xlarge");
					rds.restoreDBInstanceFromDBSnapshot(restoreRequest);

				} catch (com.amazonaws.services.rds.model.DBInstanceAlreadyExistsException ex) {
					logger.warn(ex);
				}

				while (!instanceState.equals("available")) {
					describeDBInstancesResult = rds
							.describeDBInstances(dbInstancesRequest);

					instanceState = describeDBInstancesResult.getDBInstances()
							.get(0).getDBInstanceStatus();

					endpoint = describeDBInstancesResult.getDBInstances()
							.get(0).getEndpoint().getAddress();

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						logger.error(e);
					}
				}
			}
		} catch (Exception e2) {
			logger.error(e2);
			throw e2;
		}

		return endpoint;
	}

	public void stopRDSInstance(EC2 ec2) {
		AmazonRDSClient rdsClient = getClient();
		deleteDBInstances(rdsClient, ec2);
		rdsClient.shutdown();
	}

	private void deleteDBInstances(AmazonRDSClient rds, EC2 ec2) {
		List<DBInstance> dBInstances = new ArrayList<DBInstance>();
		DeleteDBInstanceRequest deleteRequest = null;

		deleteRequest = new DeleteDBInstanceRequest(ec2.rds)
				.withSkipFinalSnapshot(true);
		dBInstances.add(rds.deleteDBInstance(deleteRequest));
	}
}
