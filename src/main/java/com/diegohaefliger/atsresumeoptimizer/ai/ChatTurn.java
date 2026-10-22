package com.diegohaefliger.atsresumeoptimizer.ai;

public record ChatTurn(Speaker speaker, String text) {

	public enum Speaker {
		USER,
		ASSISTANT
	}
}
