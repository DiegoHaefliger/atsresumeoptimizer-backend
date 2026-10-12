package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ConnectionCheck;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseSettings;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import org.mapstruct.Mapper;

@Mapper
interface DesktopDatabaseWebMapper {

	ExternalDatabase toDomain(ExternalDatabaseRequest request);

	DatabaseSettingsResponse toResponse(DatabaseSettings settings);

	ConnectionCheckResponse toResponse(ConnectionCheck check);
}
