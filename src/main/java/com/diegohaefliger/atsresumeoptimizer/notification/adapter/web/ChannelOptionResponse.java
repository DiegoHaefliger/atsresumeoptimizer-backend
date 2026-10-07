package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;

record ChannelOptionResponse(NotificationChannel channel, boolean available) {
}
