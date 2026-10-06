package com.diegohaefliger.atsresumeoptimizer.ai;

/** {@code evidence}: trecho do currículo quando o candidato tem a competência com outro nome; null se a grafia é a da vaga. */
public record PrioritySkill(String term, String evidence, boolean usedInExperience) {

	public String evidenceOrTerm() {
		return evidence == null || evidence.isBlank() ? term : evidence;
	}
}
