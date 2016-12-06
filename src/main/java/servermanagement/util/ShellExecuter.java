package main.java.servermanagement.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.ConfigModel;
import model.EC2;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * This class executes the shell script on the remote server Requires the jSch
 * java library
 *
 */
public class ShellExecuter implements Closeable {
	final static Logger logger = Logger.getLogger(ShellExecuter.class);

	private static final String CODE_DIR_PATH = "/home/ubuntu/code/";
	private static final String CONFIG_FILE_NAME = "config.js";
	private static final String CONFIG_FILE_NAME_2 = "config2.js";
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
	private static String REMOTE_HOME_DIR = "/home/ubuntu"; // username

	private static int port = 22;
	private static final String privateKey = "/home/sergeyhlghatyan/ssh_keys/NABS.pem";

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

		ConfigModel config = ConfigUtils.loadConfigJson(CONFIG_FILE_NAME_2);
		try (final ShellExecuter shellExecuter = new ShellExecuter(config)) {

			// shellExecuter.changeFiles_(config.ec2.get(0));

			EC2 ec2 = config.ec2.get(0);

			// @SuppressWarnings("unused")
			// String command =
			// shellExecuter.replaceKeyWords(config.ec2.get(0));

			// String command =
			// "sed -i 's/^db_username.*/db_username=nabs/' /home/ubuntu/code/nabs/NABS/NABS-java/src/main/resources/settings.properties;"
			// +"sed -i 's/^db_password.*/db_password=nihenhucy/' /home/ubuntu/code/nabs/NABS/NABS-java/src/main/resources/settings.properties;";

			ec2.ip = "35.156.180.48";

			shellExecuter.openConnection(ec2);

			Session session = shellExecuter.getSession(ec2.name);
			//
			// shellExecuter.getReplaceKeyWordsCommands(ec2).forEach(
			// cmd -> {
			//
			// shellExecuter.executeCommand(session,
			// Command.from(cmd, "test"));
			// });

			System.out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ShellExecuter(ConfigModel configModel) {
		this.configModel = configModel;
		this.initGitUrlsWithCredentials();
	}

	private String replaceKeyWords(EC2 ec2) {
		StringBuilder commandBuilder = new StringBuilder();
		ec2.replaceFiles.forEach(item -> {
			item.keyWords.forEach(kv -> {

				commandBuilder.append(getReplaceCommand(kv.key, kv.value,
						item.file));
				commandBuilder.append(";\n");
			});
		});

		return commandBuilder.toString();
	}

	private List<Command> getReplaceKeyWordsCommands(EC2 ec2) {
		List<Command> commands = new ArrayList<Command>();
		ec2.replaceFiles
				.forEach(item -> {
					item.keyWords.forEach(kv -> {

						commands.add(Command
								.from((getReplaceCommand(kv.key, kv.value,
										item.file) + ";\n"), ec2.name));
					});
				});

		return commands;
	}

	private String getReplaceCommand(String before, String after,
			String filePath) {
		return String.format("sed -i 's|^%s.*|%s|' %s", before, before + after,
				filePath);
	}

	public void changeFiles_(EC2 ec2) {
		try {
			ec2.ip = "35.156.180.48";
			String spPath = "/home/ubuntu/temp/";

			openConnection(ec2);
			Session session = getSession(ec2.name);

			// String command =
			// "sed -i 's/STRING_TO_REPLACE/STRING_TO_REPLACE_IT/g' filename";
			// String command = "sed -i 's/sid=/sid=NEW/' "
			String command = "sed -i 's/^sid=.*/sid=NEW/' "
					+ " /home/ubuntu/code/nabs/NABS/NABS-java/src/main/resources/settings.properties";

			this.executeCommand(session, Command.from(command, "test"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void changeFiles(EC2 ec2) {
		ec2.ip = "35.156.180.48";
		String spPath = "/home/ubuntu/temp/";

		openConnection(ec2);

		ChannelSftp sftp;
		try {
			Session session = getSession(ec2.name);
			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect();
			// sftp.cd(spPath);
			// System.out.println(sftp.getHome());
			// System.out.println(sftp.ls("/home/ubuntu/code/"));

			InputStream stream = sftp.get(spPath + "script.sh");

			File f = new File("/home/ubuntu/temp/sample.txt");
			sftp.put(new FileInputStream(f), f.getName());
			// here you can also change the target file name by replacing
			// f.getName() with your choice of name

			// try {
			//
			// BufferedReader buffer = new BufferedReader(
			// new InputStreamReader(stream));
			// // buffer.lines().forEach(line -> logger.info(line));
			// String line = null;
			// while ((line = buffer.readLine()) != null) {
			// System.out.println(line);
			// }
			// buffer.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// } finally {
			// try {
			// stream.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// }
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (SftpException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void buildAndDeploy(EC2 ec2) {

		List<Command> commands = new ArrayList<Command>();
		String host = ec2.ip;

		if (host != null && host.length() > 6) {

			ec2.branches.forEach((projectName, branch) -> {

				// checkout all local changes git checkout .
					commands.add(Command.from(this
							.getDiscardLocalChangesCommand(this
									.getProjectPath(projectName)), ec2.name));

					// git fetch
					commands.add(Command.from(this.getFetchCommand(
							this.getProjectPath(projectName),
							getRemoteOrigin(projectName), branch), ec2.name));

					// git fetch tags
					commands.add(Command.from(this.getTagFetchCommand(
							this.getProjectPath(projectName),
							getRemoteOrigin(projectName)), ec2.name));

					// git pull needed branch
					commands.add(Command.from(this.getPullCommand(
							this.getProjectPath(projectName),
							getRemoteOrigin(projectName)), ec2.name));

					// checkout to branch
					commands.add(Command.from(
							this.getCheckoutCommand(
									this.getProjectPath(projectName), branch),
							ec2.name));
				});

			this.openConnection(ec2);
			String threadName = Thread.currentThread().getName();
			logger.info("---------- start processing thread " + threadName
					+ "-----------");
			pullBuildDeploy(new ArrayList<ShellExecuter.Command>(commands),
					ec2, threadName);
			logger.info("---------- stop processing thread " + threadName
					+ "-----------");

		}

		logger.info("to delete");
	}

	public void deploy(EC2 ec2) throws Exception {

		List<Command> commands = new ArrayList<Command>();

		this.openConnection(ec2);
		String threadName = Thread.currentThread().getName();
		logger.info("---------- start processing thread " + threadName
				+ "-----------");
		runDeployScripts(new ArrayList<ShellExecuter.Command>(commands),
				ec2.name, threadName);
		logger.info("---------- stop processing thread " + threadName
				+ "-----------");

		logger.info("to delete");
	}

	private void runDeployScripts(List<Command> commands, String ec2Name,
			String tag) throws Exception {

		logger.info(tag + " start deploy for " + ec2Name);
		// deploy
		this.deployNabs(ec2Name, tag);
		logger.info(tag + " end deploy " + ec2Name);

		logger.info(tag + " start deploy on grid " + ec2Name);

		// start grid and restart tomcat
		String command = " cd " + REMOTE_HOME_DIR + "/temp; ./script.sh";
		this.executeCommand(this.getSession(ec2Name),
				Command.from(command, ec2Name), tag);

		logger.info(tag + " end deploy on grid " + ec2Name);
		logger.info(tag + "--------------- instance " + ec2Name + " is ready");
	}

	private void pullBuildDeploy(List<Command> commands, EC2 ec2, String tag) {

		logger.info(tag
				+ " start pulling from get and checkout to branches for "
				+ ec2.name);
		// pull and checkout to branches
		this.executeCommands(this.getSession(ec2.name),
				new ArrayList<ShellExecuter.Command>(commands), tag);
		logger.info(tag + " end pulling from get and checkout to branches for "
				+ ec2.name);

		logger.info(tag + " start replacing values in config files " + ec2.name);
		// pull and checkout to branches
		this.executeCommands(this.getSession(ec2.name),
				new ArrayList<ShellExecuter.Command>(
						getReplaceKeyWordsCommands(ec2)), tag);
		logger.info(tag + " end replacing values in config files " + ec2.name);

		logger.info(tag + " start build and deploy for " + ec2.name);
		// build and deploy
		// this.buildAndDeployNabs(ec2Name, tag);
		logger.info(tag + " end build and deploy " + ec2.name);

		logger.info(tag + " start deploy on grid " + ec2.name);
		// start grid and restart tomcat
		String command = " cd " + REMOTE_HOME_DIR + "/temp; ./script.sh";
		// this.executeCommand(this.getSession(ec2Name),
		// Command.from(command, ec2Name), tag);
		logger.info(tag + " end deploy on grid " + ec2.name);
		logger.info(tag + "--------------- instance " + ec2.name + " is ready");
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

	private String getFetchCommand(String projectPath, String remoteOrigin,
			String branch) {
		return String.format("(cd %s && git fetch %s %s)", projectPath,
				remoteOrigin, branch);
	}

	private String getDiscardLocalChangesCommand(String projectPath) {
		// return String.format("(cd %s && git checkout . && git reset)",
		// projectPath);
		return String.format("(cd %s && git reset HEAD --hard)", projectPath);
	}

	private String getPullCommand(String projectPath, String remoteOrigin) {
		return String.format("(cd %s && git pull %s )", projectPath,
				remoteOrigin);
	}

	private String getTagFetchCommand(String projectPath, String remoteOrigin) {
		return String.format("(cd %s && git fetch --tags %s)", projectPath,
				remoteOrigin);
	}

	private boolean buildAndDeployNabs(String ec2Name, String tag) throws Exception {
		return this.executeCommand(
				this.getSession(ec2Name),
				Command.from(EXPORT_JAVA_6 + ";cd " + CODE_DIR_PATH
						+ "nabs/NABS;" + "sh " + BUILD_NABS, ec2Name), tag);
	}

	private boolean deployNabs(String ec2Name, String tag) throws Exception {
		return this.executeCommand(
				this.getSession(ec2Name),
				Command.from(EXPORT_JAVA_6 + ";cd " + CODE_DIR_PATH
						+ "nabs/NABS;" + "sh " + DEPLOY, ec2Name), tag);
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
				logger.info("identity added for " + ec2);

				/*
				 * Open a new session, with your username, host and port Set the
				 * password and call connect. session.connect() opens a new
				 * connection to remote SSH server. Once the connection is
				 * established, you can initiate a new channel. this channel is
				 * needed to connect to remotely execution program
				 */
				session = jsch.getSession(USERNAME, ec2.ip, port);
				session.setConfig("StrictHostKeyChecking", "no");
				// session.setPassword(PASSWORD);
				session.connect();

				sessionMap.put(ec2.name, session);

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

	private boolean executeCommand(Session session, Command command) throws Exception {
		return executeCommand(session, command, command.tag);
	}

	private boolean executeCommand(Session session, Command command, String tag) throws Exception{
		boolean bret = true;
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

			OutputStream out = channelExec.getOutputStream();
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
				// result.add(line);
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
			bret = false;
			throw e;
		}
		return bret;
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