package com.diegohaefliger.atsresumeoptimizer.jobnote.adapter.web;

import java.time.Instant;

record JobNoteResponse(String content, Instant updatedAt) {
}
