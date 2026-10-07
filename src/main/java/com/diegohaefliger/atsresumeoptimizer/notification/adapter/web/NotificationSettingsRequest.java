package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;

record NotificationSettingsRequest(
		@NotEmpty List<@NotNull @Positive Integer> leadMinutes,
		@NotEmpty Set<@NotNull NotificationChannel> channels,
		@NotBlank String timezone) {
}
