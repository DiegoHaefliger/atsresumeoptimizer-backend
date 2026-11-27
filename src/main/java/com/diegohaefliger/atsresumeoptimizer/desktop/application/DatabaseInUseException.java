package com.diegohaefliger.atsresumeoptimizer.desktop.application;

public class DatabaseInUseException extends RuntimeException {

	public DatabaseInUseException(String holder) {
		super("O banco de dados já está em uso por " + holder + ".");
	}
}
