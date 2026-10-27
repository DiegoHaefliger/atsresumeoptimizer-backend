package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.application.NewLinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidLinkedInSnapshotException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInImage;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotSummary;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import java.util.Base64;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface LinkedInSnapshotWebMapper {

	@Mapping(target = "photo", expression = "java(image(request.photoBase64(), request.photoMimeType()))")
	@Mapping(target = "banner", expression = "java(image(request.bannerBase64(), request.bannerMimeType()))")
	NewLinkedInSnapshot toCommand(LinkedInSnapshotRequest request, SnapshotSource source);

	@Mapping(target = "hasPhoto", expression = "java(snapshot.hasPhoto())")
	@Mapping(target = "hasBanner", expression = "java(snapshot.hasBanner())")
	@Mapping(target = "missingSections", expression = "java(snapshot.missingSections())")
	LinkedInSnapshotResponse toResponse(LinkedInSnapshot snapshot);

	LinkedInSnapshotSummaryResponse toSummaryResponse(LinkedInSnapshotSummary summary);

	default LinkedInImage image(String base64, String mimeType) {
		if (base64 == null || base64.isBlank()) {
			return null;
		}
		try {
			return new LinkedInImage(Base64.getDecoder().decode(base64.strip()), mimeType);
		} catch (IllegalArgumentException exception) {
			throw new InvalidLinkedInSnapshotException("Imagem em base64 inválida.");
		}
	}
}
