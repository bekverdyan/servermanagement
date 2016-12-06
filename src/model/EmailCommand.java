package model;

/**
 * Created by sergeyhlghatyan on 12/6/16.
 */
public class EmailCommand {
    public String ec2Name;
    public String command;

    public EmailCommand(String ec2Name, String command) {
        this.ec2Name = ec2Name;
        this.command = command;
    }


    public static EmailCommand createFromRawValue(String raw) throws Exception {
        String[] splitted = raw.split("\\s+");
        return new EmailCommand(splitted[0], splitted[1]);
    }

    @Override
    public String toString() {
        return "EmailCommand{" +
                "ec2Name='" + ec2Name + '\'' +
                ", command='" + command + '\'' +
                '}';
    }
}
