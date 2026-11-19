package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInProfileDraft;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidLinkedInSnapshotException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.DocumentTextReader;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import org.springframework.stereotype.Service;

@Service
class LinkedInPdfImportServiceImpl implements LinkedInPdfImportService {

	static final int MAX_TEXT_CHARS = 60_000;

	private static final String NOT_A_LINKEDIN_PDF =
			"Este arquivo não parece ser o PDF do seu perfil do LinkedIn. No seu perfil, clique em Mais e depois em Salvar como PDF.";

	private final DocumentTextReader textReader;
	private final AiPort aiPort;
	private final LinkedInProfileDraftMapper draftMapper;
	private final LinkedInSnapshotService snapshotService;

	LinkedInPdfImportServiceImpl(DocumentTextReader textReader, AiPort aiPort, LinkedInProfileDraftMapper draftMapper,
			LinkedInSnapshotService snapshotService) {
		this.textReader = textReader;
		this.aiPort = aiPort;
		this.draftMapper = draftMapper;
		this.snapshotService = snapshotService;
	}

	@Override
	public LinkedInCapture importPdf(byte[] content) {
		String text = read(content);
		LinkedInProfileUrl url = LinkedInProfileUrl.find(text).orElseThrow(() -> new InvalidLinkedInSnapshotException(NOT_A_LINKEDIN_PDF));
		LinkedInProfileDraft draft = ask(text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text);
		LinkedInProfile profile = draftMapper.toProfile(draft, url.url(), url.customUrl());
		if (profile.lacksContent()) {
			throw new InvalidLinkedInSnapshotException(NOT_A_LINKEDIN_PDF);
		}
		return snapshotService.capture(new NewLinkedInSnapshot(SnapshotSource.PDF, profile, null, null));
	}

	private String read(byte[] content) {
		try {
			return textReader.read(content);
		} catch (DocumentExtractionException unreadable) {
			throw new InvalidLinkedInSnapshotException("Não consegui ler este PDF. Tente exportar de novo pelo LinkedIn.");
		} catch (BusinessException unsupported) {
			throw new InvalidLinkedInSnapshotException(NOT_A_LINKEDIN_PDF);
		}
	}

	private LinkedInProfileDraft ask(String text) {
		try {
			return aiPort.readLinkedInProfile(text).value();
		} catch (AiCallException failure) {
			throw new LinkedInAnalysisUnavailableException(failure.getMessage());
		}
	}
}
