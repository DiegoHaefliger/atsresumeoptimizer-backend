package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

/** {@code context}: dados do app (vagas anexadas etc.) já formatados; vão como dado, não como instrução. */
public record ChatBrief(String context, List<ChatTurn> history) {

	public ChatBrief {
		context = context == null ? "" : context;
		history = List.copyOf(history);
	}
}
