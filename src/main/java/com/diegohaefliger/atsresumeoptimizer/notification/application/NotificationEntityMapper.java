package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.Notification;
import org.mapstruct.Mapper;

@Mapper
interface NotificationEntityMapper {

	Notification toDomain(NotificationEntity entity);
}
