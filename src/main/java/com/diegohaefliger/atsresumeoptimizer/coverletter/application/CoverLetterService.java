package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import java.util.List;
import java.util.UUID;

public interface CoverLetterService {

	/** Da mais nova para a mais antiga: cada geração fica no histórico da vaga. */
	List<CoverLetter> list(UUID jobPostingId);

	CoverLetter generate(UUID jobPostingId, UUID resumeVersionId);

	CoverLetter update(UUID jobPostingId, UUID coverLetterId, String content);
}
