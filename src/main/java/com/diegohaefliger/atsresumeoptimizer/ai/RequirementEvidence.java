package com.diegohaefliger.atsresumeoptimizer.ai;

/** {@code evidence} é trecho literal do currículo que comprova a exigência {@code requirement} da vaga. */
public record RequirementEvidence(String requirement, String evidence) {
}
