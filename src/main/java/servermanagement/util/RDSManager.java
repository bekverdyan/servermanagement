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

    private static final String CREDENTIAL_PATH = "src/main/resources/credentials/credentials.txt";

    public RDSManager() {
    }

    public String startRDSInstance(EC2 ec2, String snapshot) {
        String endpoint = null;
        AmazonRDSClient rdsClient = null;
        try {
            rdsClient = getClient();
            if (rdsClient != null) {
                endpoint = restoreDBFromSnapshot(rdsClient, ec2, snapshot);
                if (endpoint == null) {
                    logger.debug(String.format("start rds for %s has failed", ec2.getLogTag()));
                }
            } else {
                logger.debug(String.format("get AmazonRDSClient for  %s has failed", ec2.getLogTag()));
            }
        } finally {
            if (rdsClient != null) {
                rdsClient.shutdown();
            }
        }
        return endpoint;
    }

    private AmazonRDSAsyncClient getClient() {
        AmazonRDSAsyncClient rdsClient = null;
        try {
            AWSCredentials credentials = new PropertiesCredentials(new File(
                    CREDENTIAL_PATH));
            rdsClient = new AmazonRDSAsyncClient(credentials);
            rdsClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        } catch (Exception e) {
            logger.error(e);
        }
        return rdsClient;
    }

    public String restoreDBFromSnapshot(AmazonRDSClient rds, EC2 ec2,
                                        String snapshotIdentifier) {

        String endpoint = null;
        try {

            DescribeDBInstancesRequest dbInstanceDescribeRequest = new DescribeDBInstancesRequest()
                    .withDBInstanceIdentifier(ec2.rds);

            DescribeDBInstancesResult describeDBInstancesResult = rds
                    .describeDBInstances(dbInstanceDescribeRequest);

            String instanceState = describeDBInstancesResult.getDBInstances()
                    .get(0)
                    .getDBInstanceStatus();

            if (!instanceState.equals("available")) {

                RestoreDBInstanceFromDBSnapshotRequest restoreRequest = new RestoreDBInstanceFromDBSnapshotRequest(
                        ec2.rds, snapshotIdentifier)
                        .withDBInstanceClass("db.r3.xlarge");

                rds.restoreDBInstanceFromDBSnapshot(restoreRequest);


                do {
                    describeDBInstancesResult = rds
                            .describeDBInstances(dbInstanceDescribeRequest);

                    instanceState = describeDBInstancesResult.getDBInstances()
                            .get(0)
                            .getDBInstanceStatus();


                    if (!instanceState.equals("available")) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            logger.error(e);
                        }

                        endpoint = describeDBInstancesResult.getDBInstances()
                                .get(0)
                                .getEndpoint()
                                .getAddress();
                    }
                }
                while (!instanceState.equals("available"));

            }
        } catch (Exception e2) {
            logger.error(e2);
        }

        return endpoint;
    }

    public boolean stopRDSInstance(EC2 ec2) {


        boolean bret = true;
        AmazonRDSClient client = null;
        try {
            client = getClient();
            if (client != null) {
                bret = deleteDBInstances(client, ec2);
                if (!bret) {
                    logger.debug(String.format("delete Rds for %s has failed", ec2.getLogTag()));
                }
            } else {
                logger.debug(String.format("get AmazonRDSClient for  %s has failed", ec2.getLogTag()));
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }

        return bret;
    }

    private boolean deleteDBInstances(AmazonRDSClient rds, EC2 ec2) {
        boolean bret = true;
        try {
            DeleteDBInstanceRequest deleteRequest = new DeleteDBInstanceRequest(ec2.rds)
                    .withSkipFinalSnapshot(true);
            rds.deleteDBInstance(deleteRequest);
        } catch (Exception ex) {
            bret = false;
            logger.error(ex);
        }
        return bret;
    }
}
