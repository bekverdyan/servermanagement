package main.java.servermanagement.util;

import main.java.servermanagement.util.mail.EmailUtils;
import model.ConfigModel;
import model.EC2;
import model.EmailCommand;
import model.KeyValuePair;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DeploymentManager_ {

    private static final String CONFIG_FILE_NAME = "config3.js";
    private final static Logger logger = Logger
            .getLogger(DeploymentManager_.class);

    public static void main(String[] args) {


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
                return;
            }

            String inputFilePath = cmd.getOptionValue("c");

        ConfigModel configModel = ConfigUtils
                .loadConfigJson(inputFilePath);


        logger.info(configModel);


        EmailUtils emailUtils = new EmailUtils();

        try {
            while (true) {
                List<EmailCommand> commands = emailUtils.checkForEmail(configModel);

                logger.warn(commands);
                if (!commands.isEmpty()) {

                    commands.forEach(el -> {
                        if (el.command.equalsIgnoreCase("start")) {
                            start(emailUtils,configModel, el.ec2Name, false);
                        } else {
                            stop(emailUtils,configModel, el.ec2Name);
                        }
                    });
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static void start(EmailUtils emailUtils, ConfigModel configModel, String ec2Name,
                             boolean buildAndDeploy) {

        EC2Manager ec2Manager = new EC2Manager();

        RDSManager rdsManager = new RDSManager();

        EC2 ec2 = configModel.ec2.stream()
                .filter(el -> el.name.equalsIgnoreCase(ec2Name))
                .findFirst()
                .get();

        ThreadFactory service = Executors.defaultThreadFactory();

        service.newThread(
                () -> {
                    try {

                        String endpoint = rdsManager
                                .startRDSInstance(ec2,
                                        configModel.snapshot);

                        if(ec2.replaceFiles != null && ec2.replaceFiles.size() > 0) {
                            KeyValuePair kvPair = ec2.replaceFiles
                                    .stream()
                                    .filter(el -> el.file
                                            .contains("NABS-java"))
                                    .findFirst()
                                    .get().keyWords
                                    .stream()
                                    .filter(kv -> kv.key
                                            .equals("db_connection_url"))
                                    .findFirst()
                                    .get();

                            kvPair.value = "=" + endpoint;
                        }

                        ec2.ip = ec2Manager.startEc2Instance(ec2);

                        ShellExecuter shellExecuter = null;
                        try {
                            shellExecuter = new ShellExecuter(
                                    configModel);

                            if (buildAndDeploy) {
                                shellExecuter.buildAndDeploy(ec2);
                            } else {
                                shellExecuter.deploy(ec2);
                            }
                        } catch (Exception e) {
                            logger.error(e);
                            throw e;
                        }finally {
                            shellExecuter.close();
                        }

                        emailUtils.sendEmail(configModel.mailSender, "server start", "servers are succesfully started");

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


        EC2 ec2 = configModel.ec2.stream()
                .filter(el -> el.name.equalsIgnoreCase(ec2Name))
                .findFirst()
                .get();

        service.newThread(() -> {
            try {
                rdsManager.stopRDSInstance(ec2);
                ec2Manager.stopEc2Instance(ec2);

                emailUtils.sendEmail(configModel.mailSender, "server stop", "servers are succesfully stoped");
            } catch (Exception e) {
                logger.error(e);
            }
        })
                .start();

    }
}
