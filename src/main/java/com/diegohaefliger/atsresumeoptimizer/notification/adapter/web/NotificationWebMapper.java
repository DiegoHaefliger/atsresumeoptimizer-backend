package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.Notification;
import org.mapstruct.Mapper;

@Mapper
interface NotificationWebMapper {

	NotificationResponse toResponse(Notification notification);
}
