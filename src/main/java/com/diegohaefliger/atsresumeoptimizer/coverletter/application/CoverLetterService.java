package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import java.util.Optional;

public interface CoverLetterService {

	/** A apresentação é da vaga da análise: outra análise da mesma vaga enxerga a mesma. */
	Optional<CoverLetter> find(AnalysisId analysisId);

	/** Gerar de novo substitui a anterior da vaga. */
	CoverLetter generate(AnalysisId analysisId);

	CoverLetter save(AnalysisId analysisId, String content);
}
