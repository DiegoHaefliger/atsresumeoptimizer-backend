package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

public class GoogleAuthRevokedException extends RuntimeException {

	public GoogleAuthRevokedException() {
		super("O Google recusou o refresh token (acesso revogado ou expirado).");
	}
}
