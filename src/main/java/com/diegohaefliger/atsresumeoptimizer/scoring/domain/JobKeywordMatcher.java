package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import java.util.List;

public interface JobKeywordMatcher {

	List<KeywordMatch> match(ParsingResult parsing, JobStructured job);
}
