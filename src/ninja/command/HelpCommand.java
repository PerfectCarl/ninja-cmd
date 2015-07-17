package ninja.command;

public class HelpCommand extends Command {

	@Override
	public void execute() throws Exception {
		info("Available commands:");
		for (Command command : CommandRegistry.instance().getCommands()) {
			info(String.format("  - %-10s: %s", command.getCommand(), command.getHelp()));
			command.displayUsage();
		}
	}

	@Override
	public String getCommand() {
		return "help";
	}

	@Override
	public String getHelp() {
		return "list all the available commands";
	}

}
