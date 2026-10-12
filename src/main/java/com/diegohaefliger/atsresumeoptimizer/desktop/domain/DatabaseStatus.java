package com.diegohaefliger.atsresumeoptimizer.desktop.domain;

import java.util.Optional;

public record DatabaseStatus(DatabaseMode mode, Optional<ExternalDatabase> external) {
}
