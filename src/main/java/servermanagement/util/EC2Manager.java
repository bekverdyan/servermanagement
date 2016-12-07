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

   // private static final String CREDENTIAL_PATH = "/home/sergeyhlghatyan/ssh_keys/aws/credentials";
   private static final String CREDENTIAL_PATH = "src/main/resources/credentials/credentials.txt";

    public EC2Manager() {
    }

    public String startEc2Instance(EC2 ec2) {
        String ip = null;
        AmazonEC2AsyncClient client = null;
        try {
            if(ec2 != null) {
                client = getClient();
                if (client != null) {
                    ip = startServer(client, ec2);
                    if (ip == null) {
                        logger.debug(String.format("start server for %s has failed", ec2.getLogTag()));
                    }
                } else {
                    logger.debug(String.format("get AmazonEC2AsyncClient for  %s has failed", ec2.getLogTag()));
                }
            }else{
                logger.debug("Ec2 instance is null");
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }

        return ip;
    }

    private AmazonEC2AsyncClient getClient() {
        AWSCredentials credentials;
        AmazonEC2AsyncClient client = null;
        try {
            File file = new File(CREDENTIAL_PATH);
            credentials = new PropertiesCredentials(file);
            client = new AmazonEC2AsyncClient(credentials);
            client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        } catch (Exception e) {
            logger.error(e);
        }

        return client;
    }

    private String startServer(AmazonEC2Async client, EC2 ec2) {
        String ip = null;
        try {

            DescribeInstancesResult describeInstanceResult;

            StartInstancesRequest startRequest = new StartInstancesRequest()
                    .withInstanceIds(ec2.id);

            @SuppressWarnings("unused")
            StartInstancesResult startResult = client
                    .startInstances(startRequest);

            Integer instanceState = -1;

            do {
                describeInstanceResult = getInstanceStatus(ec2.id, client);

                instanceState = describeInstanceResult.getReservations()
                        .get(0)
                        .getInstances()
                        .get(0)
                        .getState()
                        .getCode();

                if (instanceState != 16) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }
            } while (instanceState != 16);// Loop until the instance is in the

            logger.debug(ec2.name + " started");

            ip = describeInstanceResult
                    .getReservations()
                    .get(0)
                    .getInstances()
                    .get(0)
                    .getPublicIpAddress();

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
        return ec2
                .describeInstances(describeInstanceRequest);
    }

    public boolean stopEc2Instance(EC2 ec2) {

        boolean bret = true;
        AmazonEC2AsyncClient client = null;
        try {
            client = getClient();
            if (client != null) {
                bret = stopServer(client, ec2);
                if (!bret) {
                    logger.debug(String.format("stop server for %s has failed", ec2.getLogTag()));
                }
            } else {
                logger.debug(String.format("get AmazonEC2AsyncClient for  %s has failed", ec2.getLogTag()));
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }

        return bret;
    }

    private boolean stopServer(AmazonEC2Async client, EC2 ec2) {
        boolean bret = true;
        try {
            StopInstancesRequest stopRequest = new StopInstancesRequest()
                    .withInstanceIds(ec2.id);
            client.stopInstances(stopRequest);
        } catch (Exception ex) {
            bret = false;
            logger.error(ex);
        }
        return bret;
    }
}
