package ninja.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class CommandRegistry {

	private HashMap<String, Command> commands = new HashMap<>();
	private static CommandRegistry instance = new CommandRegistry();

	public void register(Command command) {
		commands.put(command.getCommand().toLowerCase(), command);
	}

	public static CommandRegistry instance() {
		return instance;
	}

	public Command get(String commandName) {
		return commands.get(commandName);
	}

	public List<Command> getCommands() {
		ArrayList<Command> result = new ArrayList<Command>();
		for (Command cmd : commands.values()) {
			result.add(cmd);
		}
		result.sort(new Comparator<Command>() {
			@Override
			public int compare(Command o1, Command o2) {
				return o1.getCommand().compareTo(o2.getCommand());
			}
		});
		return result;
	}
}
