package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
interface NotificationSettingsEntityMapper {

	NotificationSettings toDomain(NotificationSettingsEntity entity);

	@Mapping(target = "updatedAt", ignore = true)
	void update(NotificationSettings settings, @MappingTarget NotificationSettingsEntity entity);

	default String toText(ZoneId zone) {
		return zone.getId();
	}

	default ZoneId toZone(String text) {
		return ZoneId.of(text);
	}
}
