package main.java.servermanagement.util;

import java.io.IOException;
import java.util.Map;

import main.java.servermanagement.util.mail.FetchingEmail;
import model.ConfigModel;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

public class DeploymentManager {

  private final static Logger logger = Logger.getLogger(DeploymentManager.class);

  public static void main(String[] args) {

    /**
     * Enable this part of code to check if there is a new requests for qa
     * server management
     */
    FetchingEmail fetchingEmail = new FetchingEmail();
    try {
      JSONObject jsonObject = fetchingEmail.checkForEmail();
      System.out.println(jsonObject.toJSONString());
    } catch (IOException e) {
      e.printStackTrace();
    }

    /*ConfigModel config = ConfigUtils.loadConfigJson();

    EC2Manager ec2Manager = new EC2Manager(config);
    Map<String, String> ec2NameIpMap = ec2Manager.startEc2Instances();

    try (final ShellExecuter shellExecuter = new ShellExecuter(config)) {

      shellExecuter.buildAndDeploy(ec2NameIpMap);

    } catch (Exception e) {
      logger.error(e);
    }*/
  }
}
