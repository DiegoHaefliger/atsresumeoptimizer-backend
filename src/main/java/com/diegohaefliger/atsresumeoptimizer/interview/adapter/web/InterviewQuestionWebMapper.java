package com.diegohaefliger.atsresumeoptimizer.interview.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface InterviewQuestionWebMapper {

	InterviewQuestionSetResponse toResponse(InterviewQuestionSet set);

	List<InterviewQuestionSetResponse> toResponses(List<InterviewQuestionSet> sets);
}
