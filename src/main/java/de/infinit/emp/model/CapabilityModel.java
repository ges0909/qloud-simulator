package de.infinit.emp.model;

import de.infinit.emp.domain.Capability;

public class CapabilityModel extends Model<Capability, String> {
	public CapabilityModel() {
		super(Capability.class);
	}
	
	public Capability create(Capability capability) {
		return create(super.dao, capability);
	}
}