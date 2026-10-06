package com.diegohaefliger.atsresumeoptimizer.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JobStructuringService {

/** Sempre passa pela IA: o cache de respostas por prompt/versão evita custo, e prompt novo reestrutura a vaga já gravada. */
	JobStructuringResult structureFromText(String jobText);

	JobStructuringResult structureFromText(String jobText, JobDetails details);

	JobStructuringResult structureFromTargetRole(String targetRole);

	/** Grava a vaga e os dados digitados pelo usuário sem chamar a IA; a estruturação vem depois, no pipeline. */
	UUID register(String jobText, JobDetails details);

		String getRawText(UUID jobPostingId);

	Optional<JobOffer> offer(UUID jobPostingId);

	Map<UUID, JobOffer> offers(Collection<UUID> jobPostingIds);

	/** Se a IA falhar a vaga fica gravada, só sem título. */
	JobRegistration registerAndStructure(String jobText, JobDetails details);

	/** Texto novo vira vaga relida pela IA; dados digitados substituem os anteriores (campo vazio apaga). */
	JobRegistration update(UUID jobPostingId, String jobText, JobDetails details);

	/** Palavras-chave que valem pra vaga: a lista do usuário se ele editou, senão a extraída pela IA. */
	List<String> keywords(UUID jobPostingId);

	/** As principais, destacadas na adaptação: a escolha do usuário, senão as 5 primeiras da lista. */
	List<String> selectedKeywords(UUID jobPostingId);

	/** A lista do usuário passa a valer no lugar da extraída pela IA; {@code selected} são as principais, dentro dela. */
	void replaceKeywords(UUID jobPostingId, List<String> keywords, List<String> selected);

	/** Perfis gerados a partir de cargo-alvo ficam de fora. */
	List<JobListing> listJobs();

	/** Não apaga: análises antigas continuam apontando pra vaga. */
	void hideFromRecent(UUID jobPostingId);
}
