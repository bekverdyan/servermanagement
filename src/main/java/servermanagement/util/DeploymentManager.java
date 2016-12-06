package main.java.servermanagement.util;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

import main.java.servermanagement.util.mail.EmailUtils;
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
    EmailUtils fetchingEmail = new EmailUtils();

    /*try {
      fetchingEmail.sentEmail("sergey.hlghatyan@scdm.de", "Dvijeni ka ?", "Ka ha dvijeni ?"
          + "BR"
          + "Aram");
    } catch (MessagingException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }*/

    /*try {
      JSONObject jsonObject = fetchingEmail.checkForEmail();
      System.out.println(jsonObject.toJSONString());
    } catch (IOException e) {
      e.printStackTrace();
    }*/

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
