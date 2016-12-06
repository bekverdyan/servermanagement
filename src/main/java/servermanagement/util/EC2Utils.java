package main.java.servermanagement.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

public class EC2Utils {
  public static void main(String[] args) {
    try {
      AWSCredentials credentials = new PropertiesCredentials(new File("/home/sergeyhlghatyan/ssh_keys/aws/credentials"));
      AmazonEC2Async ec2 = new AmazonEC2AsyncClient(credentials);

      List<Instance> instances = new ArrayList<Instance>();

      ec2.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));

      Filter filter = new Filter().withName("tag:BUNDESBANK").withValues("BUNDESBANK-EC2");

      String print = "Instance Name: %s, Instance Id; %s";

      List<String> ids = new ArrayList<String>();
      // ids.add("i-dc50dd60"); // 1
      // ids.add("i-8c63d530"); // 2
      ids.add("i-c3350c7f"); // 3
      // ids.add("i-7ab936c6"); // 5

      startServers(ec2, ids);

      System.out.println("Complete");

      /*
       * for (Instance instance : instances) {
       * System.out.println(String.format(print,
       * instance.getTags().get(instance.getTags().indexOf(new
       * Tag().withKey("Name"))), instance.getInstanceId())); }
       */

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, String> getInstances(AmazonEC2Async ec2, Filter filter) {
    Map<String, String> instances = new HashMap<String, String>();
    DescribeInstancesRequest describeRequest = new DescribeInstancesRequest().withFilters(filter);
    DescribeInstancesResult describeResult = ec2.describeInstances(describeRequest);

    String instanceName = null;

    for (Reservation reservation : describeResult.getReservations()) {
      for (Instance instance : reservation.getInstances()) {

        for (Tag tag : instance.getTags()) {
          if (StringUtils.equalsIgnoreCase(tag.getKey(), "Name")) {
            instanceName = tag.getValue();
            break;
          }
        }

        instances.put(instance.getInstanceId(), instanceName);
      }
    }

    return instances;
  }

  public static List<InstanceStateChange> startServers(AmazonEC2Async ec2, List<String> instanceIds) {
    StartInstancesRequest startRequest = new StartInstancesRequest().withInstanceIds(instanceIds);
    StartInstancesResult startResult = ec2.startInstances(startRequest);

    Integer instanceState = -1;
    while (instanceState != 16) { // Loop until the instance is in the
      // "running" state.
      instanceState = getInstanceStatus(instanceIds.get(0), ec2);
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
      }
    }

    System.out.println("started");
    System.out.println(instanceState);

    return startResult.getStartingInstances();
  }

  // 0 : pending
  // 16 : running
  // 32 : shutting-down
  // 48 : terminated
  // 64 : stopping
  // 80 : stopped

  public static Integer getInstanceStatus(String instanceId, AmazonEC2Async ec2) {
    DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
    DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
    InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();

    String ip = describeInstanceResult.getReservations().get(0).getInstances().get(0).getPublicIpAddress();

    return state.getCode();
  }

  public static List<InstanceStateChange> stopServers(AmazonEC2Async ec2, Collection<String> instanceIds) {
    StopInstancesRequest stopRequest = new StopInstancesRequest().withInstanceIds(instanceIds);
    StopInstancesResult stopResult = ec2.stopInstances(stopRequest);

    return stopResult.getStoppingInstances();
  }
}
