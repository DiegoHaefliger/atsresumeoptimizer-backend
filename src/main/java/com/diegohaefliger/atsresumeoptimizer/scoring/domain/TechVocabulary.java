package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import java.util.Set;

public interface TechVocabulary {

	/** Termos conhecidos citados no texto, pelo nome canônico: alias e grafia equivalente contam como o mesmo termo. */
	Set<String> termsIn(String text);
}
