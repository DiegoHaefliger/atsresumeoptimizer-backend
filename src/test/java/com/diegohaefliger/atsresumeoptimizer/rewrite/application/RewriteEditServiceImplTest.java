package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RewriteEditServiceImplTest {

	@Mock
	private AnalysisSnapshotPort analysisSnapshotPort;

	@Mock
	private ResumeService resumeService;

	@Mock
	private ResumeDocumentWriter documentWriter;

	@InjectMocks
	private RewriteEditServiceImpl service;

	@Test
	void writesTheEditedContentAsNewVersionsOfTheAdaptedResumeLeavingTheBaseUntouched() {
		AnalysisId analysisId = AnalysisId.generate();
		UUID adaptedVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		UUID docxId = UUID.randomUUID();
		ResumeContact contact = new ResumeContact("ana@email.com", null, null, null, null, "Panambi, RS");
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(UUID.randomUUID(), AnalysisStatus.COMPLETED, 70, AnalysisMode.GENERAL, null, null)));
		when(resumeService.latestAdaptedVersion(analysisId.value())).thenReturn(Optional.of(adaptedVersionId));
		when(documentWriter.writeAdaptedVersion(eq(adaptedVersionId), eq(ResumeTemplate.CLASSIC), any(), eq(contact)))
				.thenReturn(new StoredResumeDocuments(resumeId, docxId, null, new byte[0]));

		ComposedResume documents = service.saveEdited(analysisId, ResumeTemplate.CLASSIC,
				new StructuredResume("Ana  Silva", "Backend", List.of()), contact);

		ArgumentCaptor<StructuredResume> contentCaptor = ArgumentCaptor.forClass(StructuredResume.class);
		verify(documentWriter).writeAdaptedVersion(eq(adaptedVersionId), eq(ResumeTemplate.CLASSIC),
				contentCaptor.capture(), eq(contact));
		assertThat(contentCaptor.getValue().name()).isEqualTo("Ana Silva");
		assertThat(documents).isEqualTo(new ComposedResume(resumeId, docxId, null));
	}

	@Test
	void createsTheAdaptedResumeWhenTheAnalysisHasNoneYet() {
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		ResumeContact contact = new ResumeContact(null, null, null, null, null, null);
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 70, AnalysisMode.JOB_MATCH, "Vaga", null)));
		when(resumeService.latestAdaptedVersion(analysisId.value())).thenReturn(Optional.empty());
		when(documentWriter.writeAdapted(eq(sourceVersionId), eq(analysisId.value()), eq("adaptado para a vaga"),
				eq(ResumeTemplate.CLASSIC), any(), eq(contact)))
				.thenReturn(new StoredResumeDocuments(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new byte[0]));

		service.saveEdited(analysisId, ResumeTemplate.CLASSIC, new StructuredResume("Ana", null, List.of()), contact);

		verify(documentWriter).writeAdapted(eq(sourceVersionId), eq(analysisId.value()), eq("adaptado para a vaga"),
				eq(ResumeTemplate.CLASSIC), any(), eq(contact));
	}

	@Test
	void failsWhenTheAnalysisDoesNotExist() {
		AnalysisId analysisId = AnalysisId.generate();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.saveEdited(analysisId, ResumeTemplate.CLASSIC,
				new StructuredResume("Ana", null, List.of()), new ResumeContact(null, null, null, null, null, null)))
				.isInstanceOf(AnalysisNotFoundException.class);
	}
}
