package com.diegohaefliger.atsresumeoptimizer.cost.application;

import com.diegohaefliger.atsresumeoptimizer.cost.domain.JobCost;
import java.util.UUID;

public interface JobCostService {

	JobCost cost(UUID jobPostingId);
}
