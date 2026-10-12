package com.diegohaefliger.atsresumeoptimizer.release.application;

import com.diegohaefliger.atsresumeoptimizer.release.domain.LatestRelease;
import java.util.Optional;

interface ReleaseGateway {

	Optional<LatestRelease> latest();
}
