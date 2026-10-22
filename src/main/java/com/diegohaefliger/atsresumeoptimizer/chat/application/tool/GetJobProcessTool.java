package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class GetJobProcessTool extends AbstractJobTool {

	private static final String NO_PROCESS = "O usuário ainda não abriu um processo seletivo para essa vaga.";

	private final SelectionProcessService processes;

	GetJobProcessTool(ChatJobLookup jobs, SelectionProcessService processes) {
		super(jobs, "Lendo o processo da vaga");
		this.processes = processes;
	}

	@Override
	public String name() {
		return "get_job_process";
	}

	@Override
	public String description() {
		return "Devolve a etapa atual do processo seletivo da vaga, o histórico de etapas e a próxima entrevista agendada.";
	}

	@Override
	String describe(JobOffer offer) {
		Optional<SelectionProcess> found = processes.list(Optional.empty()).stream()
				.filter(process -> offer.jobPostingId().equals(process.jobPostingId()))
				.findFirst();
		return found.map(GetJobProcessTool::report).orElse(NO_PROCESS);
	}

	private static String report(SelectionProcess process) {
		StringBuilder text = new StringBuilder("Etapa atual: %s\n".formatted(process.stage().label()));
		if (process.appliedOn() != null) {
			text.append("Candidatura enviada em: %s\n".formatted(process.appliedOn()));
		}
		process.history().forEach(movement -> text.append("- %s em %s\n".formatted(movement.stage().label(), movement.stageAt())));
		SelectionSchedule next = process.nextSchedule();
		if (next != null) {
			text.append("Próximo compromisso: %s (%s) em %s\n".formatted(next.stage().label(), next.status(), next.scheduledAt()));
		}
		return text.toString();
	}
}
