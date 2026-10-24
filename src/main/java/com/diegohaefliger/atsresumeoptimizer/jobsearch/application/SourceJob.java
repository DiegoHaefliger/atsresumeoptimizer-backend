package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.LocalDate;

record SourceJob(String externalId, String url, String title, String company, String location, LocalDate postedOn) {
}
