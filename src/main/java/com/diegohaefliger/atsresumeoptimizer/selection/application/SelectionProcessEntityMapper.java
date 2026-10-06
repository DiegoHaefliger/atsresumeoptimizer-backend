package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessData;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.StageMovement;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
interface SelectionProcessEntityMapper {

	@Mapping(target = "id", source = "entity.id")
	@Mapping(target = "jobPostingId", source = "entity.jobPostingId")
	@Mapping(target = "jobCode", source = "offer.code")
	@Mapping(target = "company", source = "offer.company")
	@Mapping(target = "jobTitle", source = "offer.title")
	@Mapping(target = "jobUrl", source = "offer.sourceUrl")
	@Mapping(target = "processUrl", source = "entity.processUrl")
	@Mapping(target = "stage", source = "entity.stage")
	@Mapping(target = "appliedOn", source = "entity.appliedOn")
	@Mapping(target = "nextStepOn", source = "entity.nextStepOn")
	@Mapping(target = "contactName", source = "entity.contactName")
	@Mapping(target = "contactEmail", source = "entity.contactEmail")
	@Mapping(target = "salary", source = "entity.salary")
	@Mapping(target = "notes", source = "entity.notes")
	@Mapping(target = "createdAt", source = "entity.createdAt")
	@Mapping(target = "updatedAt", source = "entity.updatedAt")
	@Mapping(target = "history", source = "history")
	SelectionProcess toDomain(SelectionProcessEntity entity, JobOffer offer, List<StageMovement> history);

	StageMovement toDomain(SelectionStageMovementEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "stage", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(SelectionProcessData data, @MappingTarget SelectionProcessEntity entity);
}
