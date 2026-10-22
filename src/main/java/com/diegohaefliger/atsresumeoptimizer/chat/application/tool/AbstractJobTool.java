package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.ai.ChatTool;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import java.util.List;
import java.util.Map;

abstract class AbstractJobTool implements ChatTool {

	private static final String CODE = "codigo";
	private static final String UNKNOWN_JOB = "Vaga %s não encontrada. Use list_jobs para ver os códigos disponíveis.";

	private final ChatJobLookup jobs;
	private final String action;

	AbstractJobTool(ChatJobLookup jobs, String action) {
		this.jobs = jobs;
		this.action = action;
	}

	@Override
	public List<Parameter> parameters() {
		return List.of(new Parameter(CODE, "Código da vaga como o app mostra (ex.: V0012). Veja em list_jobs."));
	}

	@Override
	public String label(Map<String, String> arguments) {
		String code = arguments.getOrDefault(CODE, "");
		return jobs.byCode(code)
				.map(offer -> "%s %s — %s".formatted(action, ChatJobLookup.display(offer.code()), offer.title()))
				.orElse("%s %s".formatted(action, code));
	}

	@Override
	public String run(Map<String, String> arguments) {
		String code = arguments.getOrDefault(CODE, "");
		return jobs.byCode(code).map(this::describe).orElse(UNKNOWN_JOB.formatted(code));
	}

	abstract String describe(JobOffer offer);
}
