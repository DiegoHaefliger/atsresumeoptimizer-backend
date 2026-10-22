package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import org.springframework.stereotype.Component;

@Component
class GetJobTool extends AbstractJobTool {

	private final JobDescription description;

	GetJobTool(ChatJobLookup jobs, JobDescription description) {
		super(jobs, "Lendo a vaga");
		this.description = description;
	}

	@Override
	public String name() {
		return "get_job";
	}

	@Override
	public String description() {
		return "Devolve os dados completos de uma vaga: empresa, senioridade, modelo, benefícios, palavras-chave e o texto da vaga.";
	}

	@Override
	String describe(JobOffer offer) {
		return description.of(offer);
	}
}
