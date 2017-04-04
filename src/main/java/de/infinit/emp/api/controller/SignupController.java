package de.infinit.emp.api.controller;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import de.infinit.emp.Status;
import de.infinit.emp.Uuid;
import de.infinit.emp.api.domain.Tag;
import de.infinit.emp.api.domain.User;
import de.infinit.emp.api.model.TagModel;
import de.infinit.emp.api.model.UserModel;
import spark.Request;
import spark.Response;

public class SignupController extends Controller {
	private static SignupController instance = null;
	final UserModel userModel = UserModel.instance();
	final TagModel tagModel = TagModel.instance();

	private SignupController() {
		super();
	}

	public static SignupController instance() {
		if (instance == null) {
			instance = new SignupController();
		}
		return instance;
	}

	class ReserveUserAccountRequest {
		class Obj {
			List<String> companyId;
		}

		Obj info;
	}

	class AddUserAccountRequest {
		String email;
		String username;
		String firstname;
		String lastname;
		@SerializedName("display_name")
		String displayName;
		String password;
		String verification;
	}

	public Object reserveAccount(Request request, Response response) {
		ReserveUserAccountRequest body = decode(request.body(), ReserveUserAccountRequest.class);
		User user = new User();
		user.setVerification(Uuid.next());
		if (userModel.create(user) == null) {
			return fail();
		}
		return result("uuid", user.getUuid(), "verification", user.getVerification(), "info", body.info);
	}

	public Object addAccount(Request request, Response response) {
		AddUserAccountRequest req = decode(request.body(), AddUserAccountRequest.class);
		User user = userModel.findFirstByColumn("verification", req.verification);
		if (user == null) {
			return status(Status.UNKNOWN_VERIFICATION);
		}
		user.setDisplayName(req.displayName);
		user.setEmail(req.email);
		user.setFirstName(req.firstname);
		user.setLastName(req.lastname);
		user.setPassword(req.password);
		user.setUserName(req.username);
		user.setPartner(config.partner());
		Tag tag = new Tag(user.getUuid(), null, null);
		if (tagModel.create(tag) == null) {
			return fail();
		}
		user.setTagAll(tag);
		if (userModel.update(user) == null) {
			return fail();
		}
		return ok();
	}
}