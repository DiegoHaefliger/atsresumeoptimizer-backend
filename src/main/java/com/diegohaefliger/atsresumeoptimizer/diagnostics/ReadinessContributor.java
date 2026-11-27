package com.diegohaefliger.atsresumeoptimizer.diagnostics;

import java.util.List;

public interface ReadinessContributor {

	List<ReadinessCheck> checks();
}
