package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.ai.ChatTool;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class ListJobsTool implements ChatTool {

	private static final String NO_JOBS = "O usuário ainda não cadastrou vagas.";

	private final ChatJobLookup jobs;
	private final SelectionProcessService processes;

	ListJobsTool(ChatJobLookup jobs, SelectionProcessService processes) {
		this.jobs = jobs;
		this.processes = processes;
	}

	@Override
	public String name() {
		return "list_jobs";
	}

	@Override
	public String description() {
		return "Lista as vagas cadastradas pelo usuário (código, cargo, empresa, senioridade e etapa do processo seletivo).";
	}

	@Override
	public List<Parameter> parameters() {
		return List.of();
	}

	@Override
	public String label(Map<String, String> arguments) {
		return "Listando suas vagas";
	}

	@Override
	public String run(Map<String, String> arguments) {
		List<JobOffer> offers = jobs.all();
		if (offers.isEmpty()) {
			return NO_JOBS;
		}
		Map<UUID, String> stageByJob = new HashMap<>();
		for (SelectionProcess process : processes.list(Optional.empty())) {
			if (process.jobPostingId() != null) {
				stageByJob.putIfAbsent(process.jobPostingId(), process.stage().label());
			}
		}
		return offers.stream()
				.map(offer -> "%s | %s | %s | %s | etapa: %s".formatted(ChatJobLookup.display(offer.code()), offer.title(),
						offer.company() == null ? "empresa não informada" : offer.company(),
						offer.seniority() == null ? "senioridade não informada" : offer.seniority(),
						stageByJob.getOrDefault(offer.jobPostingId(), "sem processo")))
				.collect(Collectors.joining("\n"));
	}
}
