package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ConnectionCheck;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseMode;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseSettings;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseStatus;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DesktopDatabaseException;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("desktop")
class DesktopDatabaseServiceImpl implements DesktopDatabaseService {

	private final DatabaseStatus running;
	private final ExternalDatabaseStore store;
	private final DatabaseProbe probe;
	private final DatabaseCopier copier;
	private final ObjectProvider<DesktopRestart> restart;

	DesktopDatabaseServiceImpl(DatabaseStatus running, ExternalDatabaseStore store, DatabaseProbe probe, DatabaseCopier copier,
			ObjectProvider<DesktopRestart> restart) {
		this.running = running;
		this.store = store;
		this.probe = probe;
		this.copier = copier;
		this.restart = restart;
	}

	@Override
	public DatabaseSettings current() {
		if (running.mode() == DatabaseMode.EXTERNAL) {
			return DatabaseSettings.of(DatabaseMode.EXTERNAL, running.external().orElseThrow());
		}
		return store.previous().map(previous -> DatabaseSettings.of(DatabaseMode.EMBEDDED, previous))
				.orElseGet(DatabaseSettings::embedded);
	}

	@Override
	public ConnectionCheck check(ExternalDatabase database) {
		return probe.check(database);
	}

	@Override
	public synchronized boolean useExternal(ExternalDatabase database, boolean copyData) {
		ConnectionCheck check = probe.check(database);
		if (copyData) {
			if (!check.empty()) {
				throw DesktopDatabaseException.notEmpty(database);
			}
			try {
				copier.copyTo(database);
			} catch (SQLException | IllegalStateException exception) {
				throw new DesktopDatabaseException(HttpStatus.BAD_GATEWAY,
						"A cópia para " + database.address() + " falhou e nada foi gravado lá: " + exception.getMessage(), exception);
			}
		}
		store.save(database);
		return restart();
	}

	@Override
	public synchronized boolean useEmbedded() {
		store.disable();
		return restart();
	}

	private boolean restart() {
		Optional<DesktopRestart> available = Optional.ofNullable(restart.getIfAvailable());
		available.ifPresent(DesktopRestart::restart);
		return available.isPresent();
	}
}
