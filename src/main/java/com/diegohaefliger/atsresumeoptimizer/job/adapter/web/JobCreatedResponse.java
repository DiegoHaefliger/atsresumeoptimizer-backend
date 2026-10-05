package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import java.util.List;
import java.util.UUID;

record JobCreatedResponse(UUID id, String title, String seniority, List<String> requiredKeywords) {
}
