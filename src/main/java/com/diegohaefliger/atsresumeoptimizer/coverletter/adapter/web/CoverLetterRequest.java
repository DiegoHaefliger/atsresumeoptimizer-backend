package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CoverLetterRequest(@NotBlank @Size(max = 10000) String content) {
}
