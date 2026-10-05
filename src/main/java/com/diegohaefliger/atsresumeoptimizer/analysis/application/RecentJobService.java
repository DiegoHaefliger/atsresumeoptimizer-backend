package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;
import java.util.UUID;

public interface RecentJobService {

	List<RecentJobView> recentJobs();

	void remove(UUID jobPostingId);
}
