package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record GoogleCredentialsRequest(
		@NotBlank @Size(max = 255) String clientId, @NotBlank @Size(max = 255) String clientSecret) {
}
