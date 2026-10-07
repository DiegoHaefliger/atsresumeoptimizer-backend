package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import org.mapstruct.Mapper;

@Mapper
interface CoverLetterEntityMapper {

	CoverLetter toDomain(CoverLetterEntity entity);
}
