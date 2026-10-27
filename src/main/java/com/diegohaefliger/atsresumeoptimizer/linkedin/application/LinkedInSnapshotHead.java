package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import java.time.Instant;
import java.util.UUID;

interface LinkedInSnapshotHead {

	UUID getId();

	Instant getCapturedAt();

	String getSource();

	LinkedInProfile getContent();
}
