package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

class JobSourceException extends RuntimeException {

	JobSourceException(String message) {
		super(message);
	}

	JobSourceException(String message, Throwable cause) {
		super(message, cause);
	}
}
