package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import org.mapstruct.Mapper;

@Mapper
interface CoverLetterWebMapper {

	CoverLetterResponse toResponse(CoverLetter coverLetter);
}
