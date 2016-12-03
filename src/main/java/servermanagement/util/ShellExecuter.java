package main.java.servermanagement.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.ConfigModel;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * This class executes the shell script on the remote server Requires the jSch
 * java library
 *
 */
// /release_20161115
public class ShellExecuter implements Closeable {

	// private static final Logger LOGGER = Logger.getLogger(ShellExecuter.class
	// .getName());

	final static Logger logger = Logger.getLogger(ShellExecuter.class);

	private static final String CODE_DIR_PATH = "/home/ubuntu/code/";
	private static final String CONFIG_FILE_NAME = "config.js";
	private static final String LOG_FILE_NAME = "log.log";

	private static final String BUILD_AND_DEPLOY = CODE_DIR_PATH
			+ "nabs/NABS/build_and_deploy.sh";

	private static final String NABS = "nabs";
	private static final String FINVAL = "finval";
	private static final String FINMATH_ADAPTER = "finmath-adapter";
	private static final String FOUNDATION = "foundation";
	private static final String GRID = "grid";
	private static final String ECONOMIC_MEANING = "economic-meaning";

	private static final Map<String, String> projectOriginMap = new HashMap<>();

	private ConfigModel configModel = null;
	private Session session = null;

	public static void main(String[] args) {

		try (final ShellExecuter shellExecuter = new ShellExecuter()) {

			shellExecuter.pullandCheckoutProjectsToBranches();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ShellExecuter() {
		// setupLogging();
		this.configModel = this.loadConfigJson(CONFIG_FILE_NAME);
		this.initGitUrlsWithCredentials();
		this.session = this.openConnection();
	}

	public void pullandCheckoutProjectsToBranches() {
		List<String> commands = new ArrayList<String>();
		this.configModel.ec2.forEach(ec2 -> {

			ec2.branches.forEach((projectName, branch) -> {

				commands.add(this.getCheckoutCommand(
						this.getProjectPath(projectName), branch));

				commands.add(this.getPullCommand(
						this.getProjectPath(projectName),
						getRemoteOrigin(projectName), branch));

			});
		});

		this.executeCommands(this.session, commands);
	}

	// https://<username>:<password>@github.com/<github_account>/<repository_name>.git"
	// <branch_name>
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

	private String getPullCommand(String projectPath, String remoteOrigin,
			String branchName) {
		return String.format("(cd %s && git pull %s %s )", projectPath,
				remoteOrigin, branchName);
	}

	public void buildAndDeployNabs() {
		this.executeCommand(this.session, BUILD_AND_DEPLOY);
	}

	public ConfigModel loadConfigJson(String fileName) {
		ConfigModel config = null;

		try {
			String content = new String(Files.readAllBytes(Paths.get(fileName)));

			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			config = gson.fromJson(content, ConfigModel.class);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}

	private static String USERNAME = "ubuntu"; // username for remote host
	// private static String PASSWORD ="password"; // password of the remote
	// host
	private static String host = "35.156.8.144"; // remote host address
	private static int port = 22;
	String privateKey = "/home/sergeyhlghatyan/ssh_keys/NABS.pem";

	private Session openConnection() {
		Session session = null;
		try {
			/**
			 * Create a new Jsch object This object will execute shell commands
			 * or scripts on server
			 */
			JSch jsch = new JSch();

			jsch.addIdentity(privateKey);
			logger.info("identity added ");

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

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
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
	public List<String> executeCommands(Session session,
			Collection<String> commands) {
		List<String> result = new ArrayList<String>();
		try {

			for (String command : commands) {
				executeCommand(session, command);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	public List<String> executeCommand(Session session, String command) {
		List<String> result = new ArrayList<String>();
		try {

			// create the excution channel over the session
			ChannelExec channelExec = (ChannelExec) session.openChannel("exec");

			// Gets an InputStream for this channel. All data arriving in as
			// messages from the remote side can be read from this stream.
			InputStream in = channelExec.getInputStream();

			// Set the command that you want to execute
			// In our case its the remote shell script
			// channelExec.setCommand("sh " + commands[]);

			channelExec.setCommand(command);
			logger.debug(command);

			// Execute the command
			channelExec.connect();

			// Read the output from the input stream we set above
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			String line;

			// Read each line from the buffered reader and add it to result
			// list
			// You can also simple print the result here
			while ((line = reader.readLine()) != null) {
				result.add(line);
				logger.debug(line);
			}

			// result.forEach(item -> System.out.println(item));

			// retrieve the exit status of the remote command corresponding
			// to
			// this channel
			int exitStatus = channelExec.getExitStatus();

			// Safely disconnect channel and disconnect session. If not done
			// then it may cause resource leak
			channelExec.disconnect();

			if (exitStatus < 0) {
				logger.debug("Done, but exit status not set!");
			} else if (exitStatus > 0) {
				logger.debug("Done, but with error!");
			} else {
				logger.debug("Done!");
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	@Override
	public void close() throws IOException {
		try {
			this.session.disconnect();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}