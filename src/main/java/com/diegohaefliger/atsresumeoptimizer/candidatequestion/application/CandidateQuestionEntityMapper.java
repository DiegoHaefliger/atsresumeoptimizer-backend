package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionDraft;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestion;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface CandidateQuestionEntityMapper {

	CandidateQuestionSet toDomain(CandidateQuestionSetEntity entity);

	CandidateQuestion toDomain(CandidateQuestionValue value);

	List<CandidateQuestionValue> toValues(List<CandidateQuestionDraft> drafts);

	CandidateQuestionValue toValue(CandidateQuestionDraft draft);
}
