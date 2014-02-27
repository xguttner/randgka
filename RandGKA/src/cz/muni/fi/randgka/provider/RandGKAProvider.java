package cz.muni.fi.randgka.provider;

import java.security.Provider;

public final class RandGKAProvider extends Provider {
	
	private static final long serialVersionUID = -239135023709413494L;
	
	private static final String name = "RandGKAProvider";
	private static final double version = 1.0;
	private static final String info = "Provider for randomness generation.";

	public RandGKAProvider() {
		super(name, version, info);
		put("SecureRandom.UHRandExtractor", "cz.muni.fi.randgka.provider.random.UHRandExtractor");
	}
	
	protected RandGKAProvider(String name, double version, String info) {
		super(name, version, info);
	}
}
