package ninja.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ninja.command.CleanCommand;
import ninja.command.Command;
import ninja.command.CommandRegistry;
import ninja.command.CompileCommand;
import ninja.command.CreateCommand;
import ninja.command.DepCommand;
import ninja.command.EclipseCommand;
import ninja.command.HelpCommand;
import ninja.command.IntellijCommand;
import ninja.command.PackageCommand;
import ninja.command.RunCommand;

public class Main {
	static protected final Logger log = LoggerFactory.getLogger(Command.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Main().execute(args);
	}

	private void execute(String[] args) {

		Command command = null;
		CommandRegistry.instance().register(new RunCommand());
		CommandRegistry.instance().register(new EclipseCommand());
		CommandRegistry.instance().register(new DepCommand());
		CommandRegistry.instance().register(new CreateCommand());
		CommandRegistry.instance().register(new HelpCommand());
		CommandRegistry.instance().register(new PackageCommand());
		CommandRegistry.instance().register(new CompileCommand());
		CommandRegistry.instance().register(new CleanCommand());
		CommandRegistry.instance().register(new IntellijCommand());
		String commandName = "<none>";

		if (args.length >= 1) {
			commandName = args[0].toLowerCase();
			command = CommandRegistry.instance().get(commandName);

		}
		if (command == null) {
			fatal(String.format("No command to with the name '%s'. Try 'ninja help' to launch the help command",
					commandName));

		}
		command.setArgs(args);
		try {

			command.execute();
		} catch (Exception e) {
			error(e);
		}
	}

	private void fatal(String message) {
		log.error(message);
		System.exit(1);
	}

	private void error(Exception e) {
		log.error(e.getMessage(), e);
	}

}
