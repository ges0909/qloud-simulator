package de.infinit.emp.filter;

import static spark.Spark.halt;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.infinit.emp.controller.SessionController;
import de.infinit.emp.entity.Session;
import de.infinit.emp.model.SessionModel;
import spark.Request;
import spark.Response;

public class AuthenticationFilter {
	static final Logger log = Logger.getLogger(AuthenticationFilter.class.getName());
	static final SessionModel sessionService = new SessionModel();

	static final String STATUS_NO_AUTH = "{\"status\":\"no-auth\"}";
	static final String STATUS_NO_SESSION = "{\"status\":\"no-session\"}";

	public static void authenticateRequest(Request request, Response response) {
		String method = request.requestMethod();
		String path = request.pathInfo();
		if (method.equals("GET") && path.equals("/api/session")) {
			return; // new session requested => bypass authorization check
		}
		if (path.equals("/upload") || path.equals("/favicon.ico")) {
			return; // upload requested => bypass authorization check
		}
		String authorization = request.headers("Authorization");
		if (authorization == null) {
			log.log(Level.SEVERE, "Authorization header: missing");
			halt(STATUS_NO_AUTH);
		}
		String[] authorizationParts = authorization.split("\\s");
		if (authorizationParts.length != 2) {
			log.log(Level.SEVERE, "Authorization header: only 'Bearer' and <Sid> allowed");
			halt(STATUS_NO_AUTH);
		}
		if (!authorizationParts[0].equals("Bearer")) {
			log.log(Level.SEVERE, "Authorization header: missing keyword 'Bearer'");
			halt(STATUS_NO_AUTH);
		}
		String sid = authorizationParts[1];
		if (sid == null) {
			halt(STATUS_NO_AUTH);
		}
		Session session = sessionService.findSessionBySid(sid);
		if (session == null) {
			log.log(Level.SEVERE, "Authorization header: sid {0}: unknow session", sid);
			halt(STATUS_NO_SESSION);
		}
		request.session().attribute(SessionController.QLOUD_SESSION, session);
		log.log(Level.INFO, "valid sid: {0} ", sid);
	}
}