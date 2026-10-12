package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ConnectionCheck;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseSettings;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;

public interface DesktopDatabaseService {

	DatabaseSettings current();

	ConnectionCheck check(ExternalDatabase database);

	boolean useExternal(ExternalDatabase database, boolean copyData);

	boolean useEmbedded();
}
