package com.diegohaefliger.atsresumeoptimizer.interview.application;

import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionDraft;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestion;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface InterviewQuestionEntityMapper {

	InterviewQuestionSet toDomain(InterviewQuestionSetEntity entity);

	InterviewQuestion toDomain(InterviewQuestionValue value);

	List<InterviewQuestionValue> toValues(List<InterviewQuestionDraft> drafts);

	InterviewQuestionValue toValue(InterviewQuestionDraft draft);
}
