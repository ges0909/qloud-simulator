package de.infinit.emp;

import java.io.IOException;
import java.sql.SQLException;

import de.infinit.emp.controller.AuthenticationFilter;
import de.infinit.emp.controller.LoggingFilter;

//import org.h2.tools.Server;

import de.infinit.emp.controller.SensorController;
import de.infinit.emp.controller.SessionController;

public class Main {
	public static void main(String[] args) throws IOException, SQLException {
		//Server server = Server.createTcpServer().start();
		new LoggingFilter();
		new AuthenticationFilter();
		new SessionController();
		new SensorController();
		//server.stop();
	}
}
