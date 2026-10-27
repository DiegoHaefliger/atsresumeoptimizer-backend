package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

public interface PairingTokenService {

	String current();

	String regenerate();

	void verify(String token);
}
