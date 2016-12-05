package main.java.servermanagement.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ConfigModel;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * This class executes the shell script on the remote server Requires the jSch
 * java library
 *
 */
public class ShellExecuter implements Closeable {
	final static Logger logger = Logger.getLogger(ShellExecuter.class);

	private static final String CODE_DIR_PATH = "/home/ubuntu/code/";

	private static final String BUILD_AND_DEPLOY = CODE_DIR_PATH
			+ "nabs/NABS/build_and_deploy.sh";

	private final String EXPORT_JAVA_6 = "export JAVA_HOME=/usr/lib/jvm/java-6-oracle/jre";

	private static final String DEPLOY = CODE_DIR_PATH + "nabs/NABS/deploy.sh";

	private static final String BUILD_NABS = CODE_DIR_PATH
			+ "nabs/build_nabs.sh";

	private static final String NABS = "nabs";
	private static final String FINVAL = "finval";
	private static final String FINMATH_ADAPTER = "finmath-adapter";
	private static final String FOUNDATION = "foundation";
	private static final String GRID = "grid";
	private static final String ECONOMIC_MEANING = "economic-meaning";

	private static final String MASTER_BRANCH = "master";

	private static final Map<String, String> projectOriginMap = new HashMap<>();
	private static String USERNAME = "ubuntu"; // username for remote host
	private static String REMOTE_HOME_DIR = "/home/ubuntu"; // username for
															// remote host
	// private static String PASSWORD ="password"; // password of the remote
	// host
	// private static String host = "35.156.179.132"; // remote host address
	private static int port = 22;
	private static final String privateKey = "/home/sergeyhlghatyan/ssh_keys/NABS.pem";

	private ExecutorService pool = Executors.newFixedThreadPool(4);

	private ConfigModel configModel = null;
	private Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();

	static class Command implements Cloneable {
		public String command;
		public String tag;

		public Command() {

		}

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

	private Session getSession(String key) {
		return sessionMap.get(key);
	}

	public static void main(String[] args) {

		ConfigModel config = ConfigUtils.loadConfigJson();
		try (final ShellExecuter shellExecuter = new ShellExecuter(config)) {

			Map<String,String> map = new HashMap<String, String>();
			map.put("qa-1", "35.156.179.132");
			
			 shellExecuter.buildAndDeploy(map);

			// String command = " cd /home/ubuntu/temp; ./script.sh";
			// shellExecuter.executeCommand(shellExecuter.getSession(),
			// Command.from(command, "test"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ShellExecuter(ConfigModel configModel) {
		// setupLogging();
		this.configModel = configModel;
		this.initGitUrlsWithCredentials();
		// this.session = this.openConnection();
	}

	public void buildAndDeploy(Map<String, String> ec2NameIpMap) {

		this.configModel.ec2.forEach(ec2 -> {

			List<Command> commands = new ArrayList<Command>();
			String host = ec2NameIpMap.get(ec2.name);

			if (host != null && host.length() > 6) {

				ec2.branches.forEach((projectName, branch) -> {

					commands.add(Command.from(this.getFetchCommand(
							this.getProjectPath(projectName),
							getRemoteOrigin(projectName)), ec2.name));

					commands.add(Command.from(
							this.getCheckoutCommand(
									this.getProjectPath(projectName), branch),
							ec2.name));

					commands.add(Command.from(this.getPullCommand(
							this.getProjectPath(projectName),
							getRemoteOrigin(projectName), branch), ec2.name));

				});

				pool.execute(() -> {
					this.openConnection(ec2.name, host);
					String threadName = Thread.currentThread().getName();
					logger.info("---------- start processing thread "
							+ threadName + "-----------");
					deploy(new ArrayList<ShellExecuter.Command>(commands),
							ec2.name, threadName);
					logger.info("---------- stop processing thread "
							+ threadName + "-----------");
				});
			}
		});
		logger.info("to delete");
	}

	private void deploy(List<Command> commands, String ec2Name, String tag) {

		logger.info(tag
				+ " start pull ing from get and checkout to branches for "
				+ ec2Name);
		// pull and checkout to branches
		this.executeCommands(this.getSession(ec2Name),
				new ArrayList<ShellExecuter.Command>(commands), tag);
		logger.info(tag
				+ " end pull ing from get and checkout to branches for "
				+ ec2Name);

		logger.info(tag + " start build and deploy for " + ec2Name);
		// build and deploy
		this.buildAndDeployNabs(ec2Name, tag);
		logger.info(tag + " end build and deploy " + ec2Name);

		logger.info(tag + " start deploy on grid " + ec2Name);
		// start grid and restart tomcat
		String command = " cd " + REMOTE_HOME_DIR + "/temp; ./script.sh";
		this.executeCommand(this.getSession(ec2Name),
				Command.from(command, ec2Name), tag);
		logger.info(tag + " end deploy on grid " + ec2Name);
		logger.info(tag + "--------------- instance " + ec2Name + " is ready");
	}

	private void initGitUrlsWithCredentials() {
		projectOriginMap.put(NABS, String.format(
				"https://%s:%s@github.com/SCDM/nabs.git",
				this.configModel.gitUserName, this.configModel.gitPassword));
		projectOriginMap.put(FINVAL, String.format(
				"https://%s:%s@github.com/SCDM/finval.git",
				this.configModel.gitUserName, this.configModel.gitPassword));
		projectOriginMap.put(FINMATH_ADAPTER, String.format(
				"https://%s:%s@github.com/SCDM/finmath-adapter.git",
				this.configModel.gitUserName, this.configModel.gitPassword));
		projectOriginMap.put(FOUNDATION, String.format(
				"https://%s:%s@github.com/SCDM/foundation.git",
				this.configModel.gitUserName, this.configModel.gitPassword));
		projectOriginMap.put(GRID, String.format(
				"https://%s:%s@github.com/SCDM/grid.git",
				this.configModel.gitUserName, this.configModel.gitPassword));
		projectOriginMap.put(ECONOMIC_MEANING, String.format(
				"https://%s:%s@github.com/SCDM/economic-meaning.git",
				this.configModel.gitUserName, this.configModel.gitPassword));
	}

	private String getProjectPath(String projectName) {
		return String.format(CODE_DIR_PATH + projectName);
	}

	private String getRemoteOrigin(String projectName) {
		return projectOriginMap.get(projectName);
	}

	private String getCheckoutCommand(String projectPath, String branchName) {
		return String.format("(cd %s && git checkout %s )", projectPath,
				branchName);
	}

	private String getFetchCommand(String projectPath, String remoteOrigin) {
		return String.format("(cd %s && git fetch %s)", projectPath,
				remoteOrigin);
	}

	private String getPullCommand(String projectPath, String remoteOrigin,
			String branchName) {
		return String.format("(cd %s && git pull %s %s )", projectPath,
				remoteOrigin, branchName);
	}

	private void buildAndDeployNabs(String ec2Name, String tag) {
		this.executeCommand(
				this.getSession(ec2Name),
				Command.from(EXPORT_JAVA_6 + ";cd " + CODE_DIR_PATH
						+ "nabs/NABS;" + "sh " + BUILD_AND_DEPLOY, ec2Name),
				tag);
	}

	private Session openConnection(String ec2, String host) {
		Session session = null;
		if (sessionMap.containsKey(ec2)) {
			session = sessionMap.get(ec2);
		} else {
			try {
				/**
				 * Create a new Jsch object This object will execute shell
				 * commands or scripts on server
				 */
				JSch jsch = new JSch();

				jsch.addIdentity(privateKey);
				logger.info("identity added for " + ec2);

				/*
				 * Open a new session, with your username, host and port Set the
				 * password and call connect. session.connect() opens a new
				 * connection to remote SSH server. Once the connection is
				 * established, you can initiate a new channel. this channel is
				 * needed to connect to remotely execution program
				 */
				session = jsch.getSession(USERNAME, host, port);
				session.setConfig("StrictHostKeyChecking", "no");
				// session.setPassword(PASSWORD);
				session.connect();

				sessionMap.put(ec2, session);

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		return session;
	}

	/**
	 * This method will execute the script file on the server. This takes file
	 * name to be executed as an argument The result will be returned in the
	 * form of the list
	 * 
	 * @param scriptFileName
	 * @return
	 */
	private List<String> executeCommands(Session session,
			Collection<Command> commands, String tag) {
		List<String> result = new ArrayList<String>();
		try {

			for (Command command : commands) {
				executeCommand(session, command, tag);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	private List<String> executeCommand(Session session, Command command,
			String tag) {
		List<String> result = new ArrayList<String>();
		tag = tag + " ";
		try {

			// create the excution channel over the session
			ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
			channelExec.setInputStream(null);

			// Gets an InputStream for this channel. All data arriving in as
			// messages from the remote side can be read from this stream.
			InputStream in = channelExec.getInputStream();

			// Set the command that you want to execute
			// In our case its the remote shell script
			// channelExec.setCommand("sh " + commands[]);

			((ChannelExec) channelExec).setPty(true);
			// channelExec.setPtyType("VT100");
			channelExec.setCommand(command.command);

			logger.debug(tag + command);

			// OutputStream out = channelExec.getOutputStream();
			((ChannelExec) channelExec).setErrStream(System.err);

			// Execute the command
			channelExec.connect();

			// out.write(("" + "\n").getBytes());
			// out.flush();

			// Read the output from the input stream we set above
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			String line;

			// Read each line from the buffered reader and add it to result
			// list
			// You can also simple print the result here
			while ((line = reader.readLine()) != null) {
				result.add(line);
				logger.debug(tag + line);
			}

			// retrieve the exit status of the remote command corresponding
			// to
			// this channel
			int exitStatus = channelExec.getExitStatus();

			// Safely disconnect channel and disconnect session. If not done
			// then it may cause resource leak
			channelExec.disconnect();

			if (exitStatus < 0) {
				logger.debug(tag + "Done, but exit status not set!");
			} else if (exitStatus > 0) {
				logger.debug(tag + "Done, but with error!");
			} else {
				logger.debug(tag + "Done!");
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	@Override
	public void close() throws IOException {
		try {
			this.sessionMap.values().forEach(session -> session.disconnect());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}