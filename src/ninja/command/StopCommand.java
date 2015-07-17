package ninja.command;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class StopCommand extends Command {

	/**
	 * Port to listen to stop jetty on sending stop command
	 * 
	 * @parameter
	 * @required
	 */
	protected int stopPort;

	/**
	 * Key to provide when stopping jetty on executing java
	 * -DSTOP.KEY=&lt;stopKey&gt; -DSTOP.PORT=&lt;stopPort&gt; -jar start.jar
	 * --stop
	 * 
	 * @parameter
	 * @required
	 */
	protected String stopKey;

	@Override
	public void execute() throws Exception {
		try {
			Socket s = new Socket(InetAddress.getByName("127.0.0.1"), stopPort);
			s.setSoLinger(false, 0);

			OutputStream out = s.getOutputStream();
			out.write((stopKey + "\r\nstop\r\n").getBytes());
			out.flush();
			s.close();
		} catch (ConnectException e) {
			info("Jetty not running!");
		} catch (Exception e) {
			error(e);
		}

	}

	@Override
	public String getCommand() {
		return "stop";
	}

	@Override
	public String getHelp() {
		return "Stop the application and shut down the web server";
	}

}
