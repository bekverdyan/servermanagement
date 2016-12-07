package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;

public class ConfigModel {

  public static class EC2 {
    public String id;
    public String name;
    public Map<String, String> branches;

    @Override
    public String toString() {
      return "EC2 [id=" + id + ", branches=" + branches + "]";
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Map<String, String> getBranches() {
      return branches;
    }

    public void setBranches(Map<String, String> branches) {
      this.branches = branches;
    }

  }

  public String gitUserName;
  public String gitPassword;
  public List<EC2> ec2;
  public String mailSender;
  public List<String> senderEmails;

  public List<String> getValidatedEmails() {
    List<String> validEmails = new ArrayList<>();
    
    for (String email : senderEmails) {
      if (EmailValidator.getInstance().isValid(email))
        validEmails.add(email);
    }

    return validEmails;
  }

  @Override
  public String toString() {
    return "ConfigModel [ec2=" + ec2 + "]";
  }
}
