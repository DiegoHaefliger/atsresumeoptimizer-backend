package com.diegohaefliger.atsresumeoptimizer.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JobStructuringService {

	JobStructuringResult structureFromText(String jobText);

	JobStructuringResult structureFromText(String jobText, JobDetails details);

	JobStructuringResult structureFromTargetRole(String targetRole);

	/** Grava a vaga e os dados digitados pelo usuário sem chamar a IA; a estruturação vem depois, no pipeline. */
	UUID register(String jobText, JobDetails details);

	/** O cache por hash de {@link #structureFromText} evita chamar a IA de novo pro mesmo texto. */
	String getRawText(UUID jobPostingId);

	Optional<JobOffer> offer(UUID jobPostingId);

	Map<UUID, JobOffer> offers(Collection<UUID> jobPostingIds);

	/** Se a IA falhar a vaga fica gravada, só sem título. */
	JobRegistration registerAndStructure(String jobText, JobDetails details);

	/** Texto novo vira vaga relida pela IA; dados digitados substituem os anteriores (campo vazio apaga). */
	JobRegistration update(UUID jobPostingId, String jobText, JobDetails details);

	/** Perfis gerados a partir de cargo-alvo ficam de fora. */
	List<JobListing> listJobs();

	/** Não apaga: análises antigas continuam apontando pra vaga. */
	void hideFromRecent(UUID jobPostingId);
}
