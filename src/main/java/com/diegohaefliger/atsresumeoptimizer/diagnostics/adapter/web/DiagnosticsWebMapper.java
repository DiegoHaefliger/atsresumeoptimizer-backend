package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.ClientError;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface DiagnosticsWebMapper {

	@Mapping(target = "text", expression = "java(report.asText())")
	DiagnosticReportResponse toResponse(DiagnosticReport report);

	ClientError toDomain(ClientErrorRequest request);
}
