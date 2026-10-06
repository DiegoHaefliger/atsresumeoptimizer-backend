package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.HttpUrl;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

record SelectionProcessRequest(
		@NotBlank @Size(max = 255) String company,
		@NotBlank @Size(max = 255) String jobTitle,
		@Size(max = 1000) @HttpUrl String jobUrl,
		@Size(max = 1000) @HttpUrl String processUrl,
		SelectionStage stage,
		LocalDate appliedOn,
		LocalDate nextStepOn,
		@Size(max = 255) String contactName,
		@Size(max = 255) @Email String contactEmail,
		@PositiveOrZero BigDecimal salary,
		@Size(max = 5000) String notes) {
}
