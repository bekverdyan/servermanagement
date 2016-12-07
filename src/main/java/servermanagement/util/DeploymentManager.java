package main.java.servermanagement.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import main.java.servermanagement.util.mail.EmailUtils;
import model.ConfigModel;
import model.EC2;
import model.EmailCommand;
import model.KeyValuePair;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

public class DeploymentManager {

    private static final String CONFIG_FILE_NAME = "config3.js";
    private final static Logger logger = Logger
            .getLogger(DeploymentManager.class);

    public static void main(String[] args) {


        //String inputFilePath = readArguments(args);
        String inputFilePath = CONFIG_FILE_NAME;

        ConfigModel configModel = ConfigUtils
                .loadConfigJson(inputFilePath);

        if (configModel == null) {
            logger.error("invalid config file path " + inputFilePath);
            System.exit(1);
        }

        logger.info(configModel);


        EmailUtils emailUtils = new EmailUtils();

        try {
            while (true) {
                List<EmailCommand> commands = emailUtils.checkForEmail(configModel);
                logger.warn(commands);
                if (!commands.isEmpty()) {

                    commands.forEach(el -> {
                        if (el.command.equalsIgnoreCase("start")) {
                            start(emailUtils, configModel, el.ec2Name);
                        } else {
                            stop(emailUtils, configModel, el.ec2Name);
                        }
                    });
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static String readArguments(String[] args) {
        Options options = new Options();

        Option input = new Option("c", "input", true, "input file path");
        input.setRequired(true);
        options.addOption(input);

        CommandLineParser parser = new GnuParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
            return null;
        }

        return cmd.getOptionValue("c");
    }

    public static void start(EmailUtils emailUtils, ConfigModel configModel, String ec2Name) {

        EC2Manager ec2Manager = new EC2Manager();

        RDSManager rdsManager = new RDSManager();

        EC2 ec2 = configModel.getByName(ec2Name);

        ThreadFactory service = Executors.defaultThreadFactory();

        service.newThread(
                () -> {
                    try {

                        String endpoint = rdsManager
                                .startRDSInstance(ec2,
                                        configModel.snapshot);
                        if (endpoint != null) {
                            String ip = ec2Manager.startEc2Instance(ec2);

                            if (ip != null) {
                                ec2.ip = ip;

                                Deployer deployer = null;
                                try {
                                    deployer = new Deployer();
                                    boolean result = deployer.deploy(ec2);

                                    if (emailUtils != null) {

                                        String subject = ec2.name + " start status";
                                        String message = null;
                                        if (result) {
                                            message = ec2.name + " is successfully started";
                                        } else {
                                            message = ec2.name + " failed to start";
                                        }
                                        emailUtils.sentEmail(configModel.mailSender, subject, message);
                                    }

                                } catch (Exception e) {
                                    logger.error(e);
                                } finally {
                                    if (deployer != null) {
                                        deployer.close();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                })
                .start();
    }

    public static void stop(EmailUtils emailUtils, ConfigModel configModel, String ec2Name) {

        EC2Manager ec2Manager = new EC2Manager();

        RDSManager rdsManager = new RDSManager();

        ThreadFactory service = Executors.defaultThreadFactory();


        EC2 ec2 = configModel.getByName(ec2Name);

        service.newThread(() -> {
            try {
                String subject = ec2.name + " stop status";
                String message = null;

                boolean bret = rdsManager.stopRDSInstance(ec2);
                if (bret) {
                    message = ec2.rds + " is successfully deleted\n";
                } else {
                    message = ec2.rds + " fails to delete\n";
                }


                bret = ec2Manager.stopEc2Instance(ec2);

                if (emailUtils != null) {

                    if (bret) {
                        message = message + ec2.name + " is successfully stopped";
                    } else {
                        message = message + ec2.name + " fails to stop";
                    }

                    emailUtils.sentEmail(configModel.mailSender, subject, message);
                }
            } catch (Exception e) {
                logger.error(e);
            }
        })
                .start();

    }
}
