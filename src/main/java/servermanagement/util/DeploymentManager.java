package main.java.servermanagement.util;

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
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

public class DeploymentManager {

    private static final String CONFIG_FILE_NAME = "config2.js";
    private final static Logger logger = Logger
            .getLogger(DeploymentManager.class);

    public static void main(String[] args) {


//            Options options = new Options();
//
//            Option input = new Option("c", "input", true, "input file path");
//            input.setRequired(true);
//            options.addOption(input);
//
//            CommandLineParser parser = new GnuParser();
//            HelpFormatter formatter = new HelpFormatter();
//            CommandLine cmd;
//
//            try {
//                cmd = parser.parse(options, args);
//            } catch (ParseException e) {
//                System.out.println(e.getMessage());
//                formatter.printHelp("utility-name", options);
//
//                System.exit(1);
//                return;
//            }
//
//            String inputFilePath = cmd.getOptionValue("c");

        ConfigModel configModel = ConfigUtils
                .loadConfigJson(CONFIG_FILE_NAME);


        logger.info(configModel);

//
//        EmailUtils emailUtils = new EmailUtils();
//
//        try {
//            while (true) {
//                List<EmailCommand> commands = emailUtils.checkForEmail(configModel);
//
//                logger.warn(commands);
//                if (!commands.isEmpty()) {
//
//                    commands.forEach(el -> {
//                        if (el.command.equals("start")) {
//                            start(emailUtils,configModel, el.ec2Name, false);
//                        } else {
//                            stop(emailUtils,configModel, el.ec2Name);
//                        }
//                    });
//                }
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    logger.error(e);
//                }
//            }
//        } catch (Exception e) {
//            logger.error(e);
//        }


        start(null,configModel,"NABS-QA-41" , false);

        // stop(configModel,ecToProcess);
    }

    public static void start(EmailUtils emailUtils, ConfigModel configModel, String ec2Name,
                             boolean buildAndDeploy) {

        EC2Manager ec2Manager = new EC2Manager();

        RDSManager rdsManager = new RDSManager();

        EC2 ec2 = configModel.ec2.stream()
                .filter(el -> el.name.equals(ec2Name))
                .findFirst()
                .get();

        ThreadFactory service = Executors.defaultThreadFactory();

        service.newThread(
                () -> {
                    try {

                        String endpoint = rdsManager
                                .startRDSInstance(ec2,
                                        configModel.snapshot);

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

                        ec2.ip = ec2Manager.startEc2Instance(ec2);

                        try (final ShellExecuter shellExecuter = new ShellExecuter(
                                configModel)) {
                            if (buildAndDeploy) {
                                shellExecuter.buildAndDeploy(ec2);
                            } else {
                                shellExecuter.deploy(ec2);
                            }
                        } catch (Exception e) {
                            logger.error(e);
                            throw e;
                        }

                        //emailUtils.sentEmail(configModel.mailSender, "server start", "servers are succesfully started");

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
                .filter(el -> el.name.equals(ec2Name))
                .findFirst()
                .get();

        service.newThread(() -> {
            try {
                rdsManager.stopRDSInstance(ec2);
                ec2Manager.stopEc2Instance(ec2);

                emailUtils.sentEmail(configModel.mailSender, "server start", "servers are succesfully started");
            } catch (Exception e) {
                logger.error(e);
            }
        })
                .start();

    }
}
