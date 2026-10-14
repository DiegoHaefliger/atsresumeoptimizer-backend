package com.diegohaefliger.atsresumeoptimizer.personalnote.adapter.web;

import java.time.Instant;

record PersonalNoteResponse(String content, Instant updatedAt) {
}
