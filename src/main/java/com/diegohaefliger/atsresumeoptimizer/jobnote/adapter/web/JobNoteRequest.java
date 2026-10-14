package com.diegohaefliger.atsresumeoptimizer.jobnote.adapter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record JobNoteRequest(@NotNull @Size(max = JobNoteRequest.MAX_CONTENT) String content) {

	static final int MAX_CONTENT = 20000;
}
