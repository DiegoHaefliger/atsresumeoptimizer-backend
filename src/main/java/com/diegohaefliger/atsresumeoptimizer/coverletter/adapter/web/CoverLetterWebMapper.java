package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
interface CoverLetterWebMapper {

	CoverLetterResponse toResponse(CoverLetter coverLetter);

	List<CoverLetterResponse> toResponses(List<CoverLetter> coverLetters);
}
