package com.diegohaefliger.atsresumeoptimizer.resume.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegohaefliger.atsresumeoptimizer.TestcontainersConfiguration;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeUploadLimits;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionInfo;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.FavoriteResumeMustBeBaseException;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.EmptyResumeFileException;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeFileTooLargeException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ResumeServiceImplTest {

	private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

	@Autowired
	private ResumeService resumeService;

	@Test
	void uploadsToStorageAndPersistsResumeAndVersion() {
		ResumeVersionCreated created = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo do pdf"), "curriculo.pdf", "application/pdf", "texto ingenuo",
				"texto estruturado", 1);

		assertThat(created.resumeId()).isNotNull();
		assertThat(created.resumeVersionId()).isNotNull();
		assertThat(created.storageKey()).contains("curriculo.pdf");
		assertThat(created.sizeBytes()).isPositive();
		assertThat(created.sha256()).hasSize(64);
	}

	@Test
	void listsOnlyTheAdaptedResumesOfTheGivenAnalyses() {
		ResumeVersionCreated base = resumeService.storeUpload(
				"base.pdf", unique("base"), "base.pdf", "application/pdf", "texto", "texto", 1);
		UUID analysisId = UUID.randomUUID();
		UUID otherAnalysisId = UUID.randomUUID();
		ResumeVersionCreated adapted = resumeService.storeAdapted(base.resumeVersionId(), analysisId, "Vaga A",
				unique("adaptado A"), "curriculo-adaptado.docx", DOCX_MIME, "texto");
		resumeService.storeAdapted(base.resumeVersionId(), otherAnalysisId, "Vaga B", unique("adaptado B"),
				"curriculo-adaptado.docx", DOCX_MIME, "texto");

		resumeService.updateAtsScore(adapted.resumeId(), 83);

		List<ResumeSummary> found = resumeService.listAdaptedFromAnalyses(List.of(analysisId));

		assertThat(found).extracting(ResumeSummary::id).containsExactly(adapted.resumeId());
		assertThat(found.getFirst().origin()).isEqualTo(ResumeOrigin.ADAPTED);
		assertThat(found.getFirst().atsScore()).isEqualTo(83);
		assertThat(resumeService.listAdaptedFromAnalyses(List.of())).isEmpty();
	}

	@Test
	void attachesAGeneratedVersionToTheSameResumeAndAllowsDownloadingIt() {
		ResumeVersionCreated original = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo original"), "curriculo.pdf", "application/pdf", "texto original",
				"texto original", 1);

		ResumeVersionCreated rewritten = resumeService.storeGeneratedVersion(
				original.resumeVersionId(), unique("conteudo reescrito"), "curriculo-adaptado.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "texto reescrito");

		assertThat(rewritten.resumeId()).isEqualTo(original.resumeId());
		assertThat(rewritten.resumeVersionId()).isNotEqualTo(original.resumeVersionId());

		ResumeVersionInfo info = resumeService.getVersionInfo(original.resumeId(), rewritten.resumeVersionId());

		assertThat(info.fileName()).isEqualTo("curriculo-adaptado.docx");
		assertThat(new String(info.content())).startsWith("conteudo reescrito");
	}

	@Test
	void numbersTheVersionAndTellsTheAnalysisTheAdaptedResumeCameFrom() {
		ResumeVersionCreated base = resumeService.storeUpload(
				"base.pdf", unique("base origem"), "base.pdf", "application/pdf", "texto", "texto", 1);
		UUID analysisId = UUID.randomUUID();
		ResumeVersionCreated adapted = resumeService.storeAdapted(base.resumeVersionId(), analysisId, "Vaga",
				unique("adaptado origem"), "curriculo-adaptado.docx", DOCX_MIME, "texto");
		ResumeVersionCreated edited = resumeService.storeGeneratedVersion(adapted.resumeVersionId(),
				unique("editado origem"), "curriculo-editado.docx", DOCX_MIME, "texto");

		assertThat(resumeService.origin(base.resumeId(), base.resumeVersionId()))
				.isEqualTo(new ResumeVersionOrigin(1, Optional.empty()));
		assertThat(resumeService.origin(adapted.resumeId(), edited.resumeVersionId()))
				.isEqualTo(new ResumeVersionOrigin(2, Optional.of(analysisId)));
	}

	@Test
	void storesTheAdaptedResumeApartFromTheBaseAndLinkedToTheAnalysis() {
		ResumeVersionCreated base = resumeService.storeUpload(
				"Ana Silva", unique("conteudo base"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);
		UUID analysisId = UUID.randomUUID();

		ResumeVersionCreated adapted = resumeService.storeAdapted(base.resumeVersionId(), analysisId,
				"adaptado para Dev Java", unique("conteudo adaptado"), "curriculo-adaptado.docx", DOCX_MIME, "texto adaptado");

		assertThat(adapted.resumeId()).isNotEqualTo(base.resumeId());
		assertThat(resumeService.listVersions(base.resumeId())).hasSize(1);
		ResumeSummary summary = resumeService.listResumes().stream()
				.filter(resume -> resume.id().equals(adapted.resumeId()))
				.findFirst()
				.orElseThrow();
		assertThat(summary.origin()).isEqualTo(ResumeOrigin.ADAPTED);
		assertThat(summary.sourceAnalysisId()).isEqualTo(analysisId);
		assertThat(summary.title()).isEqualTo("Ana Silva · adaptado para Dev Java");
	}

	@Test
	void latestAdaptedVersionPointsToTheNewestVersionOfTheAnalysisAdaptedResume() {
		ResumeVersionCreated base = resumeService.storeUpload(
				"Ana Silva", unique("conteudo base"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);
		UUID analysisId = UUID.randomUUID();
		ResumeVersionCreated adapted = resumeService.storeAdapted(base.resumeVersionId(), analysisId,
				"adaptado na avaliação geral", unique("adaptado"), "curriculo-adaptado.docx", DOCX_MIME, "texto");
		ResumeVersionCreated edited = resumeService.storeGeneratedVersion(adapted.resumeVersionId(), unique("editado"),
				"curriculo-adaptado.pdf", "application/pdf", "texto");

		assertThat(resumeService.latestAdaptedVersion(analysisId)).contains(edited.resumeVersionId());
		assertThat(resumeService.latestAdaptedVersion(UUID.randomUUID())).isEmpty();
	}

	@Test
	void hidesTheVersionWhenTheResumeDoesNotMatch() {
		ResumeVersionCreated created = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);

		org.assertj.core.api.Assertions
				.assertThatThrownBy(() -> resumeService.getVersionInfo(UUID.randomUUID(), created.resumeVersionId()))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeVersionNotFoundException.class);
	}

	@Test
	void deleteClearsFileAndExtractedTextFromEveryVersionOfTheResume() {
		ResumeVersionCreated original = resumeService.storeUpload(
				"curriculo-ana-silva.pdf", unique("conteudo original"), "curriculo-ana-silva.pdf",
				"application/pdf", "CPF 111.444.777-35", "CPF 111.444.777-35", 1);
		resumeService.storeGeneratedVersion(original.resumeVersionId(), unique("conteudo reescrito"),
				"curriculo-adaptado.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"texto reescrito");

		resumeService.delete(original.resumeId());

		org.assertj.core.api.Assertions
				.assertThatThrownBy(() -> resumeService.downloadContent(original.resumeVersionId()))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void deleteThrowsWhenTheResumeDoesNotExist() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> resumeService.delete(UUID.randomUUID()))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeNotFoundException.class);
	}

	@Test
	void uploadValidatesExtractsAndPersistsAStandaloneResume() throws Exception {
		ResumeVersionCreated created = resumeService.upload(pdfWithResumeText(), "curriculo.pdf", "application/pdf");

		assertThat(created.resumeId()).isNotNull();
		assertThat(created.resumeVersionId()).isNotNull();
	}

	@Test
	void uploadRejectsAnEmptyFile() {
		assertThatThrownBy(() -> resumeService.upload(new byte[0], "curriculo.pdf", "application/pdf"))
				.isInstanceOf(EmptyResumeFileException.class);
	}

	@Test
	void uploadRejectsAFileLargerThanTheLimit() {
		byte[] tooLarge = new byte[(int) ResumeUploadLimits.MAX_FILE_SIZE_BYTES + 1];

		assertThatThrownBy(() -> resumeService.upload(tooLarge, "curriculo.pdf", "application/pdf"))
				.isInstanceOf(ResumeFileTooLargeException.class);
	}

	@Test
	void listVersionsReturnsTheUploadAndEveryRewriteOfTheSameResume() {
		ResumeVersionCreated original = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo original"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);
		resumeService.storeGeneratedVersion(original.resumeVersionId(), unique("conteudo reescrito"),
				"curriculo-adaptado.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"texto reescrito");

		List<ResumeVersionSummary> versions = resumeService.listVersions(original.resumeId());

		assertThat(versions).hasSize(2).extracting(ResumeVersionSummary::fileName)
				.containsExactlyInAnyOrder("curriculo.pdf", "curriculo-adaptado.docx");
	}

	@Test
	void listResumesReturnsSavedResumesNewestFirstAndHidesDeletedOnes() {
		ResumeVersionCreated kept = resumeService.storeUpload(
				"curriculo-joao.pdf", unique("conteudo joao"), "curriculo-joao.pdf", "application/pdf", "texto",
				"texto", 1);
		ResumeVersionCreated adapted = resumeService.storeGeneratedVersion(kept.resumeVersionId(),
				unique("conteudo adaptado"), "curriculo-adaptado.pdf", "application/pdf", "texto adaptado");
		ResumeVersionCreated deleted = resumeService.storeUpload(
				"curriculo-maria.pdf", unique("conteudo maria"), "curriculo-maria.pdf", "application/pdf", "texto",
				"texto", 1);
		resumeService.delete(deleted.resumeId());

		List<ResumeSummary> resumes = resumeService.listResumes();

		assertThat(resumes).extracting(ResumeSummary::id).contains(kept.resumeId()).doesNotContain(deleted.resumeId());
		ResumeSummary summary =
				resumes.stream().filter(resume -> resume.id().equals(kept.resumeId())).findFirst().orElseThrow();
		assertThat(summary.title()).isEqualTo("curriculo-joao.pdf");
		assertThat(summary.versions()).extracting(ResumeVersionSummary::id)
				.containsExactly(adapted.resumeVersionId(), kept.resumeVersionId());
	}

	@Test
	void favoriteResumeComesFirstAndMarkingAnotherOneUnmarksThePrevious() {
		ResumeVersionCreated first = resumeService.storeUpload(
				"favorito-a.pdf", unique("conteudo a"), "favorito-a.pdf", "application/pdf", "texto", "texto", 1);
		ResumeVersionCreated second = resumeService.storeUpload(
				"favorito-b.pdf", unique("conteudo b"), "favorito-b.pdf", "application/pdf", "texto", "texto", 1);

		resumeService.setFavorite(first.resumeId(), true);
		resumeService.setFavorite(second.resumeId(), true);

		List<ResumeSummary> resumes = resumeService.listResumes();
		assertThat(resumes.getFirst().id()).isEqualTo(second.resumeId());
		assertThat(resumes).filteredOn(ResumeSummary::favorite).extracting(ResumeSummary::id)
				.containsExactly(second.resumeId());

		resumeService.setFavorite(second.resumeId(), false);
		assertThat(resumeService.listResumes()).noneMatch(ResumeSummary::favorite);
	}

	@Test
	void adaptedResumeCannotBeFavorite() {
		ResumeVersionCreated base = resumeService.storeUpload(
				"Ana Silva", unique("conteudo base fav"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);
		ResumeVersionCreated adapted = resumeService.storeAdapted(base.resumeVersionId(), UUID.randomUUID(),
				"adaptado", unique("adaptado fav"), "curriculo-adaptado.docx", DOCX_MIME, "texto");

		assertThatThrownBy(() -> resumeService.setFavorite(adapted.resumeId(), true))
				.isInstanceOf(FavoriteResumeMustBeBaseException.class);
	}

	@Test
	void versionsAreNumberedInCreationOrderAndKeepTheirNumberAfterAnEarlierOneIsDeleted() {
		ResumeVersionCreated first = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo v1"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);
		ResumeVersionCreated second = resumeService.storeGeneratedVersion(first.resumeVersionId(),
				unique("conteudo v2"), "curriculo-v2.pdf", "application/pdf", "texto v2");
		ResumeVersionCreated third = resumeService.storeGeneratedVersion(first.resumeVersionId(),
				unique("conteudo v3"), "curriculo-v3.pdf", "application/pdf", "texto v3");

		resumeService.deleteVersion(first.resumeId(), first.resumeVersionId());

		assertThat(resumeService.listVersions(first.resumeId()))
				.extracting(ResumeVersionSummary::id, ResumeVersionSummary::number)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(third.resumeVersionId(), 3),
						org.assertj.core.groups.Tuple.tuple(second.resumeVersionId(), 2));
	}

	@Test
	void deletingTheLastVersionRemovesTheResumeFromTheList() {
		ResumeVersionCreated only = resumeService.storeUpload(
				"curriculo.pdf", unique("unica versao"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);

		resumeService.deleteVersion(only.resumeId(), only.resumeVersionId());

		assertThat(resumeService.listResumes()).extracting(ResumeSummary::id).doesNotContain(only.resumeId());
	}

	@Test
	void deleteVersionThrowsWhenTheVersionBelongsToAnotherResume() {
		ResumeVersionCreated created = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);
		ResumeVersionCreated other = resumeService.storeUpload(
				"outro.pdf", unique("outro"), "outro.pdf", "application/pdf", "texto", "texto", 1);

		org.assertj.core.api.Assertions
				.assertThatThrownBy(() -> resumeService.deleteVersion(other.resumeId(), created.resumeVersionId()))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeVersionNotFoundException.class);
	}

	@Test
	void listVersionsThrowsWhenTheResumeDoesNotExist() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> resumeService.listVersions(UUID.randomUUID()))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeNotFoundException.class);
	}

	@Test
	void assertExistsPassesSilentlyForAMatchingVersionAndThrowsOtherwise() {
		ResumeVersionCreated created = resumeService.storeUpload(
				"curriculo.pdf", unique("conteudo"), "curriculo.pdf", "application/pdf", "texto", "texto", 1);

		resumeService.assertExists(created.resumeId(), created.resumeVersionId());

		org.assertj.core.api.Assertions
				.assertThatThrownBy(() -> resumeService.assertExists(UUID.randomUUID(), created.resumeVersionId()))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeVersionNotFoundException.class);
	}

	@Test
	void uploadingTheSameFileAgainCreatesAnotherResume() {
		byte[] content = unique("mesmo arquivo");
		ResumeVersionCreated first = resumeService.storeUpload(
				"curriculo.pdf", content, "curriculo.pdf", "application/pdf", "texto", "texto", 1);

		ResumeVersionCreated second = resumeService.storeUpload(
				"curriculo-copia.pdf", content.clone(), "curriculo-copia.pdf", "application/pdf", "texto", "texto", 1);

		assertThat(second.resumeId()).isNotEqualTo(first.resumeId());
		assertThat(second.resumeVersionId()).isNotEqualTo(first.resumeVersionId());
	}

	private static byte[] unique(String content) {
		return (content + " " + UUID.randomUUID()).getBytes();
	}

	private byte[] pdfWithResumeText() throws Exception {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText("Ana Silva - ana.silva@email.com");
				stream.endText();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return out.toByteArray();
		}
	}
}
