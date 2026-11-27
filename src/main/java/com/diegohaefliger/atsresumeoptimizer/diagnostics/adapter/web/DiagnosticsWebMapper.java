package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.ClientError;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface DiagnosticsWebMapper {

	@Mapping(target = "text", expression = "java(report.asText())")
	@Mapping(target = "verdict", expression = "java(report.verdict())")
	DiagnosticReportResponse toResponse(DiagnosticReport report);

	ClientError toDomain(ClientErrorRequest request);
}
