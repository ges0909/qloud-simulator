package de.infinit.emp;

import org.aeonbits.owner.Config;

public interface SimulatorConfig extends Config {
	@Key("db.url")
	@DefaultValue("jdbc:h2:mem:test")
	String url();

	@Key("db.username")
	@DefaultValue("test")
	String username();

	@Key("db.password")
	@DefaultValue("test")
	String password();
	
	@Key("partner")
	@DefaultValue("brightone")
	String partner();
	
	@Key("key")
	@DefaultValue("abcdefghijklmnopqrstuvwxyz")
	String key();
	
	@Key("user.pattern")
	@DefaultValue("sim.*")
	String userPattern();

	@Key("device.pattern")
	@DefaultValue("SIMUL-.*")
	String devicePattern();
}