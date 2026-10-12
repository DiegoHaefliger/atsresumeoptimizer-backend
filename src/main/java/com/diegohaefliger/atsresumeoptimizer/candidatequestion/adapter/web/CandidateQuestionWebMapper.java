package com.diegohaefliger.atsresumeoptimizer.candidatequestion.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface CandidateQuestionWebMapper {

	CandidateQuestionSetResponse toResponse(CandidateQuestionSet set);

	List<CandidateQuestionSetResponse> toResponses(List<CandidateQuestionSet> sets);
}
