package com.diegohaefliger.atsresumeoptimizer.analysis;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import java.util.Optional;

/** Usado por {@code rewrite} pra saber se dá pra reescrever sem enxergar {@code AnalysisEntity}. */
public interface AnalysisSnapshotPort {

	Optional<AnalysisSnapshot> find(AnalysisId id);
}
