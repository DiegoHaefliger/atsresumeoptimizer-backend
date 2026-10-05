package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

class DroppedContentException extends RuntimeException {

	DroppedContentException(String reason) {
		super(reason);
	}
}
