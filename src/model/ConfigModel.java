package model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;

public class ConfigModel {

    public String gitUserName;
    public String gitPassword;
    public List<EC2> ec2;


    public String credentialsFilePath;
    public List<String> senderEmails;

    public List<String> getValidatedEmails() {
        List<String> validEmails = new ArrayList<>();

        for (String email : senderEmails) {
            if (EmailValidator.getInstance()
                    .isValid(email))
                validEmails.add(email);
        }

        return validEmails;
    }

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
