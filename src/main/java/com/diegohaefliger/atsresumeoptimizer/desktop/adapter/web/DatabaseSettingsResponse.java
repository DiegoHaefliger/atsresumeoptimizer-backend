package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseMode;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.SslMode;

record DatabaseSettingsResponse(DatabaseMode mode, String host, Integer port, String database, String username, SslMode sslMode) {
}
