package main.java.servermanagement.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import model.ConfigModel;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;

public class EC2Manager {

  private final static Logger logger = Logger.getLogger(EC2Manager.class);

  private static final String CREDENTIAL_PATH = "/home/sergeyhlghatyan/ssh_keys/aws/credentials";

  private AmazonEC2Async _ec2;
  private ConfigModel _configModel;

  public EC2Manager(ConfigModel configModel) {

    _configModel = configModel;

    AWSCredentials credentials;
    try {
      credentials = new PropertiesCredentials(new File(CREDENTIAL_PATH));
      _ec2 = new AmazonEC2AsyncClient(credentials);

    } catch (IllegalArgumentException | IOException e) {
      logger.error(e);
    }
  }

  public Map<String, String> startEc2Instances() {

    Map<String, String> ec2NameIdMap = _configModel.ec2.stream().collect(
        Collectors.toMap(ConfigModel.EC2::getName, ConfigModel.EC2::getId));

    return startServers(_ec2, ec2NameIdMap);
  }

  private Map<String, String> startServers(AmazonEC2Async ec2, Map<String, String> instanceIds) {
    StartInstancesRequest startRequest = new StartInstancesRequest().withInstanceIds(instanceIds.values());

    @SuppressWarnings("unused")
    StartInstancesResult startResult = ec2.startInstances(startRequest);

    Map<String, String> ec2ipMap = new HashMap<>();

    instanceIds.forEach((name, instanceId) -> {

      DescribeInstancesResult describeInstanceResult = null;
      Integer instanceState = -1;
      while (instanceState != 16) { // Loop until the instance is in the
          // "running" state.
          describeInstanceResult = getInstanceStatus(instanceId, ec2);

          instanceState = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState().getCode();

          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            logger.error(e);
          }
        }

        logger.debug(name + " started");

        String ip = describeInstanceResult.getReservations().get(0).getInstances().stream()
            .filter(instance -> instance.getTags().get(0).getValue().equals(name)).findAny().get().getPublicIpAddress();

        ec2ipMap.put(name, ip);

      });

    return ec2ipMap;
  }

  // 0 : pending
  // 16 : running
  // 32 : shutting-down
  // 48 : terminated
  // 64 : stopping
  // 80 : stopped

  public DescribeInstancesResult getInstanceStatus(String instanceId, AmazonEC2Async ec2) {
    DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
    DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
    //
    // InstanceState state = describeInstanceResult.getReservations().get(0)
    // .getInstances().get(0).getState();
    //
    //
    // String ip = describeInstanceResult.getReservations().get(0)
    // .getInstances().get(0).getPublicIpAddress();

    return describeInstanceResult;
  }
}
