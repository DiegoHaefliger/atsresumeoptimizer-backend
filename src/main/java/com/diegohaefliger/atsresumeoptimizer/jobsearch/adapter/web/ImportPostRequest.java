package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ImportPostRequest(
		@NotBlank @Size(max = 1000) @HttpUrl String url,
		@Size(max = 20000) String text) {
}
