package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
@Order(30)
class DesktopReadiness implements ReadinessContributor {

	private static final String NAME = "Pasta de dados";
	private static final long BYTES_PER_MB = 1024L * 1024L;
	private static final long MIN_FREE_MB = 1024;
	private static final String PERMISSION_HINT = "Confira se a pasta .ats-resume-optimizer na sua pasta de usuário existe e não está só leitura.";
	private static final String SPACE_HINT = "Libere espaço no disco: o banco local e os currículos gerados ficam nessa pasta.";

	private final DesktopProperties properties;

	DesktopReadiness(DesktopProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<ReadinessCheck> checks() {
		Path dataDir = properties.dataDir();
		if (!Files.isDirectory(dataDir) || !Files.isWritable(dataDir)) {
			return List.of(ReadinessCheck.failure(NAME, "sem permissão de escrita", PERMISSION_HINT));
		}
		try {
			long freeMb = Files.getFileStore(dataDir).getUsableSpace() / BYTES_PER_MB;
			return freeMb < MIN_FREE_MB
					? List.of(ReadinessCheck.warning(NAME, "pouco espaço livre no disco (%d MB)".formatted(freeMb), SPACE_HINT))
					: List.of(ReadinessCheck.ok(NAME, "gravável · %d MB livres".formatted(freeMb)));
		} catch (IOException exception) {
			return List.of(ReadinessCheck.warning(NAME, "não deu pra medir o espaço livre: " + exception.getMessage(), SPACE_HINT));
		}
	}
}
