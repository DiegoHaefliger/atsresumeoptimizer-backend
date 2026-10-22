package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record ChatTurn(Speaker speaker, String text, List<ChatImage> images) {

	public ChatTurn {
		images = images == null ? List.of() : List.copyOf(images);
	}

	public ChatTurn(Speaker speaker, String text) {
		this(speaker, text, List.of());
	}

	public enum Speaker {
		USER,
		ASSISTANT
	}
}
