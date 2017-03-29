package de.infinit.emp.controller;

import java.util.Map;

import org.aeonbits.owner.ConfigCache;

import com.google.gson.Gson;

import de.infinit.emp.Uuid;
import de.infinit.emp.SimulatorConfig;
import de.infinit.emp.Status;
import de.infinit.emp.model.Session;
import de.infinit.emp.service.SessionService;
import spark.Request;
import spark.Response;

public class SessionController extends Controller {
	public static final String QLOUD_SESSION = "session";
	static final Gson gson = new Gson();
	final SimulatorConfig config;
	final SessionService sessionService;

	class LoginRequest {
		String partner;
		String key;
		String user;
	}

	public SessionController() {
		config = ConfigCache.getOrCreate(SimulatorConfig.class);
		sessionService = new SessionService();
	}

	public Map<String, Object> requestNonAuthorizedSession(Request request, Response response) {
		Session session = sessionService.createSession(request.scheme(), request.host());
		return result("server", session.getServer(), "sid", session.getSid());
	}

	public Map<String, Object> loginToPartnerOrProxySession(Request request, Response response) {
		LoginRequest body = gson.fromJson(request.body(), LoginRequest.class);
		if (body.partner == null || body.key == null) {
			return status(Status.WRONG_CREDENTIALS);
		}
		if (!(config.partner().equals(body.partner) && config.key().equals(body.key))) {
			return status(Status.WRONG_CREDENTIALS);
		}
		Session session = request.session().attribute(QLOUD_SESSION);
		session.setPartner(body.partner);
		session.setKey(body.key);
		session.setUserUuid(body.user);
		return status(Status.OK);
	}

	public Map<String, Object> logoutFromSession(Request request, Response response) {
		Session session = request.session().attribute(QLOUD_SESSION);
		if (sessionService.deleteSession(session) == null) {
			return status(Status.FAIL);
		}
		return status(Status.OK);
	}
}