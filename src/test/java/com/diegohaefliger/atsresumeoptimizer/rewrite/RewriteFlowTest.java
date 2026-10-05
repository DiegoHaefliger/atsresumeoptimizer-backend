package com.diegohaefliger.atsresumeoptimizer.rewrite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.TestcontainersConfiguration;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.BulletReview;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Ponta a ponta real (Postgres + MinIO via Testcontainers), IA mockada. Cobre o "pronto
 * quando" da fase 3: POST /analyses/{id}/rewrite devolve currículo adaptado (DOCX e PDF)
 * baixável, sem fato inventado passar batido — bullet com número novo vem com needsConfirmation.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RewriteFlowTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@MockitoBean
	private AiPort aiPort;

	@Test
	void rewritesTheResumeStoringOnlyTheDocxAndExportingThePdfFromIt() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		JobStructured structured =
				new JobStructured("Backend Java", "pleno", 1, null, List.of(), List.of("Java"), Map.of());
		when(aiPort.structureJob(any()))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 120, 60, BigDecimal.valueOf(0.001), false)));
		when(aiPort.reviewBullets(any())).thenReturn(new AiResult<>(List.of(new BulletReview("bullet", 4, 4, 4, "ok")),
				new AiUsage("gpt-4o-mini", 40, 20, BigDecimal.valueOf(0.0005), false)));
		ResumeEntry entry = new ResumeEntry("Desenvolvedora Backend Java", "2020 - 2023", "Empresa X", null,
				List.of(List.of(new TextSpan("Reduzi bugs em 40% liderando desenvolvimento backend Java na Empresa X", false))),
				null);
		ResumeSection experience = new ResumeSection("Experiência Profissional", ResumeSectionSemanticType.EXPERIENCE,
				ResumeSectionKind.ENTRIES, null, null, null, List.of(entry));
		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana Silva", null, List.of(experience)),
				new AiUsage("gpt-4o-mini", 60, 30, BigDecimal.valueOf(0.0008), false)));

		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", pdfWithResumeText());

		String createBody = mockMvc.perform(multipart("/api/v1/analyses")
						.file(file)
						.param("jobDescription", "Vaga de backend Java pleno, exige Java"))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
		String analysisId = new tools.jackson.databind.ObjectMapper().readTree(createBody).get("id").asString();

		org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> mockMvc
				.perform(get("/api/v1/analyses/{id}", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.header.status").value("COMPLETED")));

		String rewriteBody = mockMvc
				.perform(post("/api/v1/analyses/{id}/rewrite", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeId").exists())
				.andExpect(jsonPath("$.documents.docxVersionId").exists())
				.andExpect(jsonPath("$.documents.pdfVersionId").doesNotExist())
				.andExpect(jsonPath("$.bullets.items[0].needsConfirmation").value(true))
				.andExpect(jsonPath("$.scoreComparison.before").exists())
				.andExpect(jsonPath("$.scoreComparison.after").exists())
				.andReturn().getResponse().getContentAsString();

		var json = new tools.jackson.databind.ObjectMapper().readTree(rewriteBody);
		String resumeId = json.get("resumeId").asString();
		String docxVersionId = json.get("documents").get("docxVersionId").asString();

		byte[] docxContent = mockMvc
				.perform(get("/api/v1/resumes/{resumeId}/versions/{versionId}/download", resumeId, docxVersionId))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(docxContent).isNotEmpty();

		byte[] pdfContent = mockMvc
				.perform(get("/api/v1/resumes/{resumeId}/versions/{versionId}/export", resumeId, docxVersionId)
						.param("format", "PDF"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andReturn().getResponse().getContentAsByteArray();
		assertThat(pdfContent).isNotEmpty();
	}

	private byte[] pdfWithResumeText() throws Exception {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText("Ana Silva - ana.silva@email.com");
				stream.endText();
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 680);
				stream.showText("EXPERIENCIA PROFISSIONAL");
				stream.endText();
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 660);
				stream.showText("• Desenvolvedora Backend Java na Empresa X, 2020 a 2023");
				stream.endText();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			assertThat(out.toByteArray()).isNotEmpty();
			return out.toByteArray();
		}
	}
}
