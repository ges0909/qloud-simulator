package de.infinit.emp;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import org.aeonbits.owner.ConfigCache;

import com.google.gson.Gson;

import de.infinit.emp.admin.controller.ConfigController;
import de.infinit.emp.admin.controller.UploadController;
import de.infinit.emp.api.controller.Controller;
import de.infinit.emp.api.controller.EventController;
import de.infinit.emp.api.controller.ObjectController;
import de.infinit.emp.api.controller.PartnerController;
import de.infinit.emp.api.controller.SensorController;
import de.infinit.emp.api.controller.SessionController;
import de.infinit.emp.api.controller.SignupController;
import de.infinit.emp.api.controller.TagController;
import de.infinit.emp.api.controller.UserController;
import de.infinit.emp.api.domain.Sensor;
import de.infinit.emp.api.model.Persistence;
import de.infinit.emp.api.model.SensorModel;
import de.infinit.emp.filter.AuthenticationFilter;
import de.infinit.emp.filter.LoggingFilter;
import spark.template.freemarker.FreeMarkerEngine;

public class Application {
	static final ApplicationConfig config = ConfigCache.getOrCreate(ApplicationConfig.class);
	static final Logger log = Logger.getLogger(Application.class.getName());
	static final FreeMarkerEngine fm = new FreeMarkerEngine(new FreeMarkerConfig());
	static final Gson gson = new Gson();
	
	public static ScheduledExecutorService executor;

	public static void main(String[] args) throws IOException, SQLException {
		initApplication();

		port(config.port());
		staticFileLocation("/public"); // to serve css, ...

		before(LoggingFilter::logRequest);
		
		serveApiEndpoints();
		serveAdminEndpoints();

		after(LoggingFilter::logResponse);

		// 'Not found' and 'Internal server error' are handled ALWAYS after all other routes
		notFound(Controller.instance()::notFound);
		internalServerError(Controller.instance()::internalServerError);
	}

	private static void initApplication() throws SQLException {
		// create database tables
		Persistence.createTabelsIfNotExists();
		// start background sensor value generation
		executor = Executors.newScheduledThreadPool(config.numberOfThreads());
		List<Sensor> sensors = SensorModel.instance().queryForAll();
		sensors.stream().forEach(Sensor::startSimulation);
	}

	static void serveApiEndpoints() {
		path("/api", () -> {
			before("/*", AuthenticationFilter::authenticateRequest);

			path("/session", () -> {
				get("", SessionController.instance()::requestNonAuthorizedSession, gson::toJson);
				post("", SessionController.instance()::loginToPartnerOrProxySession, gson::toJson);
				delete("", SessionController.instance()::logoutFromSession, gson::toJson);
			});
			path("/partner", () -> path("/user", () -> {
				get("", PartnerController.instance()::getAccounts, gson::toJson);
				get("/:uuid", PartnerController.instance()::getAccount, gson::toJson);
				post("/:uuid", PartnerController.instance()::deleteAccount, gson::toJson);
			}));
			path("/signup", () -> {
				post("/verification", SignupController.instance()::reserveAccount, gson::toJson);
				post("/user", SignupController.instance()::addAccount, gson::toJson);
			});
			path("/user", () -> {
				get("", UserController.instance()::getUser, gson::toJson);
				post("", UserController.instance()::updateUser, gson::toJson);
				get("/invitation", UserController.instance()::getUserInvitations, gson::toJson);
				post("/invitation", UserController.instance()::inviteUser, gson::toJson);
				post("/link", UserController.instance()::acceptInvitation, gson::toJson);
			});
			path("/tag", () -> {
				get("", TagController.instance()::getTags, gson::toJson);
				get("/:uuid", TagController.instance()::getTag, gson::toJson);
				post("", TagController.instance()::createTag, gson::toJson);
				delete("/:uuid", TagController.instance()::deleteTag, gson::toJson);
				post("/:uuid", TagController.instance()::updateTag, gson::toJson);
				get("/:uuid/object", TagController.instance()::getTaggedObjects, gson::toJson);
			});
			path("/object", () -> post("/:uuid/tag", ObjectController.instance()::updateTagAttachment, gson::toJson));
			path("/sensor", () -> {
				post("", SensorController.instance()::createSensor, gson::toJson);
				get("/:uuid", SensorController.instance()::getSensor, gson::toJson);
				post("/:uuid", SensorController.instance()::updateSensor, gson::toJson);
				delete("/:uuid", SensorController.instance()::deleteSensor, gson::toJson);
				get("/:uuid/data", SensorController.instance()::getSensorData, gson::toJson);
				get("/:uuid/event", EventController.instance()::susbcribeSensorForEvents, gson::toJson);
				delete("/:uuid/event", EventController.instance()::cancelSensorEventSubcription, gson::toJson);
				post("/:uuid/action", Controller.instance()::notImplemented, gson::toJson);
			});
			path("/event", () -> get("", EventController.instance()::getSensorEvents, gson::toJson));

			after("/*", (request, response) -> response.type("application/json"));
		});
	}

	static void serveAdminEndpoints() {
		path("/config", () -> {
			get("", ConfigController.instance()::showConfiguration, fm);
			post("", ConfigController.instance()::saveConfiguration, fm);
			after((request, response) -> response.type("text/html"));
		});
		path("/upload", () -> {
			get("", UploadController.instance()::displayUploadForm, fm);
			post("", UploadController.instance()::uploadFile, fm);
			after((request, response) -> response.type("text/html"));
		});
	}
}
