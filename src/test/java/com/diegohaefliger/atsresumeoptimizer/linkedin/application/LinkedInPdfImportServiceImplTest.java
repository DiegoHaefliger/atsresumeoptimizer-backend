package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInProfileDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInProfileEntryDraft;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidLinkedInSnapshotException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.DocumentTextReader;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkedInPdfImportServiceImplTest {

	private static final byte[] PDF = {1, 2, 3};
	private static final String TEXT = "Contact\nwww.linkedin.com/in/ana-souza (LinkedIn)\nAna Souza\nDev Java";

	@Mock
	private DocumentTextReader textReader;

	@Mock
	private AiPort aiPort;

	@Mock
	private LinkedInSnapshotService snapshotService;

	private LinkedInPdfImportServiceImpl service() {
		return new LinkedInPdfImportServiceImpl(textReader, aiPort, new LinkedInProfileDraftMapperImpl(), snapshotService);
	}

	private static AiResult<LinkedInProfileDraft> result(LinkedInProfileDraft draft) {
		return new AiResult<>(draft, new AiUsage("gpt-4o-mini", 100, 50, new BigDecimal("0.001"), false));
	}

	@Test
	void turnsTheExportedPdfIntoASnapshotWithoutTheDurationInThePeriod() {
		when(textReader.read(PDF)).thenReturn(TEXT);
		when(aiPort.readLinkedInProfile(TEXT)).thenReturn(result(new LinkedInProfileDraft("Ana Souza", "Dev Java", "São Paulo", "Sobre",
				List.of(new LinkedInProfileEntryDraft("Dev", "Acme", "janeiro de 2020 - Present (5 anos 2 meses)", null, "fiz X")),
				List.of(), List.of(), List.of(), List.of(), List.of("Java"), List.of("Português (Nativo)"))));
		when(snapshotService.capture(any())).thenAnswer(invocation -> new LinkedInCapture(null, false, List.of()));

		service().importPdf(PDF);

		ArgumentCaptor<NewLinkedInSnapshot> captured = ArgumentCaptor.forClass(NewLinkedInSnapshot.class);
		verify(snapshotService).capture(captured.capture());
		assertThat(captured.getValue().source()).isEqualTo(SnapshotSource.PDF);
		var profile = captured.getValue().profile();
		assertThat(profile.profileUrl()).isEqualTo("https://www.linkedin.com/in/ana-souza/");
		assertThat(profile.customUrl()).isEqualTo("linkedin.com/in/ana-souza");
		assertThat(profile.headline()).isEqualTo("Dev Java");
		assertThat(profile.experiences().get(0).period()).isEqualTo("janeiro de 2020 - Present");
		assertThat(profile.skills()).containsExactly("Java");
		assertThat(profile.languages()).containsExactly("Português (Nativo)");
		assertThat(captured.getValue().photo()).isNull();
	}

	@Test
	void refusesAPdfThatIsNotFromLinkedIn() {
		when(textReader.read(PDF)).thenReturn("Currículo de Ana, sem endereço do LinkedIn");

		assertThatThrownBy(() -> service().importPdf(PDF)).isInstanceOf(InvalidLinkedInSnapshotException.class);
		verify(aiPort, never()).readLinkedInProfile(any());
	}

	@Test
	void refusesWhenTheAiReadsNothingFromThePdf() {
		when(textReader.read(PDF)).thenReturn(TEXT);
		when(aiPort.readLinkedInProfile(TEXT)).thenReturn(result(new LinkedInProfileDraft(null, null, null, null, null, null, null, null, null, null, null)));

		assertThatThrownBy(() -> service().importPdf(PDF)).isInstanceOf(InvalidLinkedInSnapshotException.class);
		verify(snapshotService, never()).capture(any());
	}

	@Test
	void reportsAnAiFailureAsUnavailable() {
		when(textReader.read(PDF)).thenReturn(TEXT);
		when(aiPort.readLinkedInProfile(TEXT)).thenThrow(new AiCallException("Nenhum provedor de IA configurado.", null));

		assertThatThrownBy(() -> service().importPdf(PDF))
				.isInstanceOf(LinkedInAnalysisUnavailableException.class)
				.hasMessage("Nenhum provedor de IA configurado.");
	}

	@Test
	void sendsOnlyTheStartOfAVeryLongText() {
		String huge = TEXT + "x".repeat(LinkedInPdfImportServiceImpl.MAX_TEXT_CHARS);
		when(textReader.read(PDF)).thenReturn(huge);
		when(aiPort.readLinkedInProfile(any())).thenReturn(result(new LinkedInProfileDraft("Ana", "Dev", null, null, null, null, null, null, null, null, null)));
		when(snapshotService.capture(any())).thenAnswer(invocation -> new LinkedInCapture(null, false, List.of()));

		service().importPdf(PDF);

		ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
		verify(aiPort).readLinkedInProfile(sent.capture());
		assertThat(sent.getValue()).hasSize(LinkedInPdfImportServiceImpl.MAX_TEXT_CHARS);
	}
}
