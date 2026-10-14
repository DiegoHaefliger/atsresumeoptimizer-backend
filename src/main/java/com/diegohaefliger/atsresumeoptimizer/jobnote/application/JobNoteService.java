package com.diegohaefliger.atsresumeoptimizer.jobnote.application;

import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.JobNote;
import java.util.UUID;

public interface JobNoteService {

	JobNote get(UUID jobPostingId);

	JobNote save(UUID jobPostingId, String content);
}
