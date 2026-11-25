package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ClientErrorRequest(
		@NotBlank @Size(max = 2000) String message,
		@Size(max = 20000) String stack,
		@Size(max = 20000) String componentStack,
		@Size(max = 500) String page) {
}
