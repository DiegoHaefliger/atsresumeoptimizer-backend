package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.InvalidNotificationSettingsException;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface NotificationSettingsWebMapper {

	NotificationSettings toDomain(NotificationSettingsRequest request);

	@Mapping(target = "availableChannels", expression = "java(channelOptions())")
	NotificationSettingsResponse toResponse(NotificationSettings settings);

	default List<ChannelOptionResponse> channelOptions() {
		return Arrays.stream(NotificationChannel.values())
				.map(channel -> new ChannelOptionResponse(channel, channel.available()))
				.toList();
	}

	default String toText(ZoneId zone) {
		return zone.getId();
	}

	default ZoneId toZone(String text) {
		try {
			return ZoneId.of(text);
		} catch (DateTimeException exception) {
			throw new InvalidNotificationSettingsException("Fuso horário inválido: " + text);
		}
	}
}
