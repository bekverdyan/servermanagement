package model;

import java.util.List;

public class ConfigModel {

    public String gitUserName;
    public String gitPassword;
    public List<EC2> ec2;

    public String snapshot;
    public String mailSender;
    public String credentialsFilePath;

    @Override
    public String toString() {
        return "ConfigModel [ec2=" + ec2 + "]";
    }

    public EC2 getByName(String name) {
        return this.ec2.stream()
                .filter(el -> el.name.equalsIgnoreCase(name))
                .findFirst()
                .get();

    }
}
