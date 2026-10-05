package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionInfo;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ExportedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeFormat;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ComposedResumeWithoutNameException;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeEditingServiceImplTest {

	private static final UUID RESUME_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final ResumeContact CONTACT = new ResumeContact("ana@email.com", null, null, null, null, null);

	@Mock
	private ResumeService resumeService;
	@Mock
	private ResumeAnalysisPipeline parsingPipeline;
	@Mock
	private ResumeContentStore contentStore;
	@Mock
	private ResumeDocumentWriter documentWriter;

	@InjectMocks
	private ResumeEditingServiceImpl service;

	@Test
	void loadsTheContentSavedWhenTheResumeWasBuiltInTheEditor() {
		EditableResume stored = new EditableResume(null, ResumeTemplate.MODERN_BLUE,
				new StructuredResume("Ana Silva", "Backend", List.of()), CONTACT, false);
		when(contentStore.find(VERSION_ID)).thenReturn(Optional.of(stored));
		when(resumeService.title(RESUME_ID)).thenReturn("Ana.pdf");

		EditableResume editable = service.load(RESUME_ID, VERSION_ID);

		assertThat(editable).isEqualTo(stored.withTitle("Ana.pdf"));
		verify(resumeService).assertExists(RESUME_ID, VERSION_ID);
		verifyNoInteractions(parsingPipeline);
	}

	@Test
	void importsTheUploadedFileWhenThereIsNoSavedContent() {
		when(contentStore.find(VERSION_ID)).thenReturn(Optional.empty());
		when(resumeService.downloadContent(VERSION_ID)).thenReturn(new byte[] {1});
		when(parsingPipeline.analyze(any())).thenReturn(new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "Ana Silva", "Ana Silva", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false), List.of(),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty(), Optional.empty())));

		EditableResume editable = service.load(RESUME_ID, VERSION_ID);

		assertThat(editable.importedFromFile()).isTrue();
		assertThat(editable.content().name()).isEqualTo("Ana Silva");
		assertThat(editable.contact().email()).isEqualTo("ana@email.com");
	}

	@Test
	void savesTheEditionAsNewVersionsOfTheSameResume() {
		UUID docxId = UUID.randomUUID();
		UUID pdfId = UUID.randomUUID();
		when(documentWriter.writeEdited(eq(VERSION_ID), eq(ResumeTemplate.CLASSIC), any(), eq(CONTACT)))
				.thenReturn(new StoredResumeDocuments(RESUME_ID, docxId, pdfId, new byte[0]));

		ComposedResume saved = service.save(RESUME_ID, VERSION_ID, null, ResumeTemplate.CLASSIC,
				new StructuredResume("Ana Silva", null, List.of()), CONTACT);

		assertThat(saved).isEqualTo(new ComposedResume(RESUME_ID, docxId, pdfId));
		verify(resumeService, never()).rename(any(), any());
	}

	@Test
	void renamesTheResumeWhenANewTitleIsGiven() {
		when(documentWriter.writeEdited(eq(VERSION_ID), eq(ResumeTemplate.CLASSIC), any(), eq(CONTACT)))
				.thenReturn(new StoredResumeDocuments(RESUME_ID, UUID.randomUUID(), UUID.randomUUID(), new byte[0]));

		service.save(RESUME_ID, VERSION_ID, "Meu currículo", ResumeTemplate.CLASSIC,
				new StructuredResume("Ana Silva", null, List.of()), CONTACT);

		verify(resumeService).rename(RESUME_ID, "Meu currículo");
	}

	@Test
	void rejectsAnEditionWithoutTheCandidateName() {
		assertThatThrownBy(() -> service.save(RESUME_ID, VERSION_ID, null, ResumeTemplate.CLASSIC,
				new StructuredResume(" ", null, List.of()), CONTACT))
				.isInstanceOf(ComposedResumeWithoutNameException.class);
		verifyNoInteractions(documentWriter);
	}

	@Test
	void exportsTheStoredFileAsIsWhenItAlreadyHasTheRequestedFormat() {
		when(resumeService.getVersionInfo(RESUME_ID, VERSION_ID))
				.thenReturn(new ResumeVersionInfo(new byte[] {7}, "x.pdf", "application/pdf"));
		when(resumeService.title(RESUME_ID)).thenReturn("Diego_Haefliger.pdf");

		ExportedResume exported = service.export(RESUME_ID, VERSION_ID, ResumeFormat.PDF);

		assertThat(exported.fileName()).isEqualTo("Diego_Haefliger.pdf");
		assertThat(exported.content()).containsExactly(7);
		verifyNoInteractions(documentWriter);
	}

	@Test
	void rendersTheSavedContentWhenTheRequestedFormatIsNotStored() {
		EditableResume stored = new EditableResume(null, ResumeTemplate.CLASSIC,
				new StructuredResume("Ana Silva", null, List.of()), CONTACT, false);
		when(resumeService.getVersionInfo(RESUME_ID, VERSION_ID))
				.thenReturn(new ResumeVersionInfo(new byte[] {7}, "x.docx", ResumeFormat.DOCX.mimeType()));
		when(resumeService.title(RESUME_ID)).thenReturn("Meu: currículo");
		when(contentStore.find(VERSION_ID)).thenReturn(Optional.of(stored));
		when(documentWriter.render(eq(ResumeTemplate.CLASSIC), any(), eq(CONTACT)))
				.thenReturn(new RenderedResume(new byte[] {1}, new byte[] {2}, "texto"));

		ExportedResume exported = service.export(RESUME_ID, VERSION_ID, ResumeFormat.PDF);

		assertThat(exported.fileName()).isEqualTo("Meu_ currículo.pdf");
		assertThat(exported.content()).containsExactly(2);
	}
}
