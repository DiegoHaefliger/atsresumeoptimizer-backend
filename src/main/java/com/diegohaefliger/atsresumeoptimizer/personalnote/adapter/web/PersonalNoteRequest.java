package com.diegohaefliger.atsresumeoptimizer.personalnote.adapter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record PersonalNoteRequest(@NotNull @Size(max = PersonalNoteRequest.MAX_CONTENT) String content) {

	static final int MAX_CONTENT = 20000;
}
