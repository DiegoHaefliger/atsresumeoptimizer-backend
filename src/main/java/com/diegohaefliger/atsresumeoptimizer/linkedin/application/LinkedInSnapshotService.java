package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkedInSnapshotService {

	LinkedInSnapshot capture(NewLinkedInSnapshot snapshot);

	Optional<LinkedInSnapshot> latest();

	LinkedInSnapshot get(UUID id);

	List<LinkedInSnapshotSummary> history();
}
