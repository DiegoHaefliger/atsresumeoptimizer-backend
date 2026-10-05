package com.diegohaefliger.atsresumeoptimizer.resume;

import com.diegohaefliger.atsresumeoptimizer.resume.domain.EmptyResumeFileException;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeFileTooLargeException;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeTooManyPagesException;

public final class ResumeUploadLimits {

	public static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;
	public static final int MAX_PAGES = 5;

	private ResumeUploadLimits() {
	}

	public static void validateSize(long sizeBytes) {
		if (sizeBytes == 0) {
			throw new EmptyResumeFileException();
		}
		if (sizeBytes > MAX_FILE_SIZE_BYTES) {
			throw new ResumeFileTooLargeException();
		}
	}

	/** DOCX/ODT chegam com {@code pageCount} nulo: sem renderizar não dá pra contar página. */
	public static void validatePageCount(Integer pageCount) {
		if (pageCount != null && pageCount > MAX_PAGES) {
			throw new ResumeTooManyPagesException(MAX_PAGES);
		}
	}
}
