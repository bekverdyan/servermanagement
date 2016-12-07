package main.java.servermanagement.util;

import com.jcraft.jsch.*;
import model.ConfigModel;
import model.EC2;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class executes the shell script on the remote server Requires the jSch
 * java library
 */
public class Deployer implements Closeable {
    final static Logger logger = Logger.getLogger(Deployer.class);

    private static final String CODE_DIR_PATH = "/home/ubuntu/code/";
    private static final String CALIBRATION_DIR = CODE_DIR_PATH + "grid/calibration";

    private final String EXPORT_JAVA_6 = "export JAVA_HOME=/usr/lib/jvm/java-6-oracle/jre";

    private static final String DEPLOY = CODE_DIR_PATH + "nabs/NABS/deploy.sh";

    private static String USERNAME = "ubuntu"; // username for remote host

    private static int port = 22;
    private static final String privateKey = "src/main/resources/credentials/NABS.pem";

    private Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();

    static class Command {
        public String command;
        public String tag;

        public Command(String command, String tag) {
            this.command = command;
            this.tag = tag;
        }

        public static Command from(String command, String tag) {
            return new Command(command, tag);
        }

        @Override
        public String toString() {
            return "Command [command=" + command + ", tag=" + tag + "]";
        }
    }

    public static void main(String[] args) {

    }

    public Deployer() {
        //File file = new File(getClass().getClassLoader().getResource("example.txt").getFile());
    }

    public boolean deploy(EC2 ec2) {

        boolean bret = true;
        Session session = null;
        try {
            session = this.openConnection(ec2);
            ec2.threadName = Thread.currentThread()
                    .getName();

            if (session.isConnected()) {

                bret = this.executeDeployNabs(session, ec2);
                if (!bret) {
                    logger.debug(String.format("deploy script with tag %s has failed", ec2.getLogTag()));
                } else {
                    bret = startAndDeployOnGrid(session, ec2);
                    if (!bret) {
                        logger.debug(String.format("start and deploy script with tag %s has failed", ec2.getLogTag()));
                    }
                }
            }
        } catch (Exception ex) {
            bret = false;
            logger.error(ex);
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
        return bret;
    }

    private String getPrivateKeyAbsolutePath() {
        String path = null;
        File file = null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            file = new File(classLoader.getResource(privateKey)
                    .getFile());
            path = file.getAbsolutePath();
        } catch (Exception e) {
            logger.error(e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
        return path;
    }

    private boolean executeDeployNabs(Session session, EC2 ec2) throws Exception {
        return this.executeCommand(
                session,
                Command.from(EXPORT_JAVA_6 + "; " + "sh " + DEPLOY, ec2.name));
    }

    private boolean startAndDeployOnGrid(Session session, EC2 ec2) {
        String command = " cd " + CALIBRATION_DIR + "; ./start_and_deploy.sh";
        return this.executeCommand(session,
                Command.from(command, ec2.getLogTag()));
    }

    private Session openConnection(EC2 ec2) {
        Session session = null;
        if (sessionMap.containsKey(ec2.name)) {
            session = sessionMap.get(ec2.name);
        } else {
            try {
                /**
                 * Create a new Jsch object This object will execute shell
                 * commands or scripts on server
                 */
                JSch jsch = new JSch();

                jsch.addIdentity(privateKey);
                logger.info("identity added for " + ec2.getLogTag());

                session = jsch.getSession(USERNAME, ec2.ip, port);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();
                sessionMap.put(ec2.name, session);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return session;
    }


    private boolean executeCommand(Session session, Command command) {
        boolean bret = true;
        String tag = command.tag;
        try {

            // create the excution channel over the session
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");

            InputStream in = channelExec.getInputStream();

            channelExec.setCommand(command.command);

            logger.debug(tag + command);

            // Execute the command
            channelExec.connect();

            // Read the output from the input stream we set above
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = reader.readLine()) != null) {
                // result.add(line);
                logger.debug(tag + line);
            }

            int exitStatus = channelExec.getExitStatus();

            channelExec.disconnect();

            if (exitStatus < 0) {
                logger.debug(tag + " Done, but exit status not set!");
                bret = false;
            } else if (exitStatus > 0) {
                logger.debug(tag + " Done, but with error!");
                bret = false;
            } else {
                logger.debug(tag + " Done!");
            }

        } catch (Exception e) {
            logger.error(tag + " " + e.getMessage(), e);
            bret = false;
        }
        return bret;
    }

    @Override
    public void close() throws IOException {
        try {
            this.sessionMap.values()
                    .forEach(session -> session.disconnect());
            this.sessionMap.clear();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}