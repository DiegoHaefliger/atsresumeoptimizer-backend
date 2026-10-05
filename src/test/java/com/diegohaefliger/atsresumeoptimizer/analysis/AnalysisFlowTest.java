package com.diegohaefliger.atsresumeoptimizer.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.TestcontainersConfiguration;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.BulletReview;
import com.diegohaefliger.atsresumeoptimizer.ai.JobConditions;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.awaitility.Awaitility;
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
 * Ponta a ponta real (Postgres + MinIO via Testcontainers), com a IA mockada — sem chamar
 * OpenAI de verdade. Cobre o "pronto quando" da fase 1b: POST com vaga → GET devolvendo
 * JOB_MATCH completo com score, dimensões e keywords.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AnalysisFlowTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@MockitoBean
	private AiPort aiPort;

	@Test
	void createsAndCompletesAJobMatchAnalysisEndToEnd() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		JobStructured structured = new JobStructured("Backend Java", "pleno", 1, null, List.of(),
				List.of("Java", "Kafka"), Map.of());
		when(aiPort.structureJob(any()))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 120, 60, BigDecimal.valueOf(0.001), false)));
		when(aiPort.reviewBullets(any())).thenReturn(new AiResult<>(List.of(new BulletReview("bullet", 4, 4, 4, "ok")),
				new AiUsage("gpt-4o-mini", 40, 20, BigDecimal.valueOf(0.0005), false)));

		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", pdfWithResumeText());

		String responseBody = mockMvc.perform(multipart("/api/v1/analyses")
						.file(file)
						.param("jobDescription", "Vaga de backend Java pleno, exige Java e Kafka"))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();

		String analysisId = new tools.jackson.databind.ObjectMapper().readTree(responseBody).get("id").asString();

		Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mockMvc
				.perform(get("/api/v1/analyses/{id}", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.header.status").value("COMPLETED")));

		mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.header.mode").value("JOB_MATCH"))
				.andExpect(jsonPath("$.header.status").value("COMPLETED"))
				.andExpect(jsonPath("$.score.overall").exists())
				.andExpect(jsonPath("$.keywords.found", org.hamcrest.Matchers.hasItem("Java")))
				.andExpect(jsonPath("$.keywords.missing", org.hamcrest.Matchers.hasItem("Kafka")));

		mockMvc.perform(post("/api/v1/analyses/{id}/feedback", analysisId)
						.contentType("application/json")
						.content("{\"rating\":5,\"comment\":\"score bateu com a expectativa\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/analyses/{id}/ats-view", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rawText", org.hamcrest.Matchers.containsString("Ana Silva")));

		mockMvc.perform(get("/api/v1/analyses/cost-report"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.analysesWithAiUsage", org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.byModel[0].model").value("gpt-4o-mini"));

		mockMvc.perform(get("/api/v1/analyses/recent-jobs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.jobDescription == 'Vaga de backend Java pleno, exige Java e Kafka')].title")
						.value(org.hamcrest.Matchers.hasItem("Backend Java")));
	}

	@Test
	void refusesAPdfWithMoreThan5Pages() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", pdfWithPageCount(6));

		mockMvc.perform(multipart("/api/v1/analyses").file(file)).andExpect(status().is(413));
	}

	@Test
	void analyzesAnExistingResumeVersionAgainstAnExistingJobWithoutReuploadingEither() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		JobStructured structured = new JobStructured("Backend Java", "pleno", 1, null, List.of(),
				List.of("Java", "Kafka"), Map.of());
		when(aiPort.structureJob(any()))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 120, 60, BigDecimal.valueOf(0.001), false)));
		when(aiPort.reviewBullets(any())).thenReturn(new AiResult<>(List.of(new BulletReview("bullet", 4, 4, 4, "ok")),
				new AiUsage("gpt-4o-mini", 40, 20, BigDecimal.valueOf(0.0005), false)));

		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", pdfWithResumeText());
		String resumeBody = mockMvc.perform(multipart("/api/v1/resumes").file(file))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		var resumeJson = new tools.jackson.databind.ObjectMapper().readTree(resumeBody);
		String resumeId = resumeJson.get("resumeId").asString();
		String resumeVersionId = resumeJson.get("resumeVersionId").asString();

		String jobBody = mockMvc
				.perform(post("/api/v1/jobs").contentType("application/json")
						.content("{\"text\":\"Vaga de backend Java pleno, exige Java e Kafka\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String jobId = new tools.jackson.databind.ObjectMapper().readTree(jobBody).get("id").asString();

		String analysisBody = mockMvc
				.perform(post("/api/v1/resumes/{resumeId}/analyses", resumeId)
						.contentType("application/json")
						.content("{\"resumeVersionId\":\"%s\",\"jobId\":\"%s\"}".formatted(resumeVersionId, jobId)))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
		String analysisId = new tools.jackson.databind.ObjectMapper().readTree(analysisBody).get("id").asString();

		Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mockMvc
				.perform(get("/api/v1/analyses/{id}", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.header.status").value("COMPLETED")));

		mockMvc.perform(get("/api/v1/resumes/{resumeId}/versions", resumeId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].fileName").value("curriculo.pdf"));

		mockMvc.perform(get("/api/v1/resumes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '%s')].versions[0].id".formatted(resumeId)).value(resumeVersionId));
	}

	private byte[] pdfWithPageCount(int pageCount) throws Exception {
		try (PDDocument document = new PDDocument()) {
			for (int i = 0; i < pageCount; i++) {
				document.addPage(new PDPage(PDRectangle.LETTER));
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return out.toByteArray();
		}
	}

	@Test
	void scoresTheJobAgainstTheSavedPreferencesUsingTheTypedDetailsAndTheConditionsExtractedByAi() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		JobStructured structured = new JobStructured("Engenheiro de Dados", "sênior", 5, null, List.of(), List.of("Python"),
				Map.of(), new JobConditions("Acme", "ON_SITE", "CLT", new BigDecimal("12000"), new BigDecimal("15000"),
						List.of("Plano de saúde", "Vale-refeição"), "São Paulo - SP"));
		when(aiPort.structureJob(any()))
				.thenReturn(new AiResult<>(structured, new AiUsage("gpt-4o-mini", 120, 60, BigDecimal.valueOf(0.001), false)));
		when(aiPort.reviewBullets(any())).thenReturn(new AiResult<>(List.of(new BulletReview("bullet", 4, 4, 4, "ok")),
				new AiUsage("gpt-4o-mini", 40, 20, BigDecimal.valueOf(0.0005), false)));

		mockMvc.perform(put("/api/v1/job-preference")
						.contentType("application/json")
						.content("""
								{"workModels":["REMOTE"],"contractTypes":["CLT"],"minSalary":10000,
								 "benefits":["plano de saúde","PLR"]}
								"""))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/job-preference"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workModels[0]").value("REMOTE"))
				.andExpect(jsonPath("$.benefits[1]").value("PLR"));

		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", pdfWithResumeText());
		String responseBody = mockMvc.perform(multipart("/api/v1/analyses")
						.file(file)
						.param("jobDescription", "Vaga de engenharia de dados sênior em Python, híbrido")
						.param("jobCompany", "Acme Brasil")
						.param("jobUrl", "https://acme.com/vagas/42")
						.param("jobWorkModel", "HYBRID"))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
		String analysisId = new tools.jackson.databind.ObjectMapper().readTree(responseBody).get("id").asString();

		Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> mockMvc
				.perform(get("/api/v1/analyses/{id}", analysisId))
				.andExpect(jsonPath("$.header.status").value("COMPLETED")));

		mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.job.company").value("Acme Brasil"))
				.andExpect(jsonPath("$.job.sourceUrl").value("https://acme.com/vagas/42"))
				.andExpect(jsonPath("$.job.workModel").value("HYBRID"))
				.andExpect(jsonPath("$.preferenceMatch.criteria[0].criterion").value("WORK_MODEL"))
				.andExpect(jsonPath("$.preferenceMatch.criteria[0].status").value("PARTIAL"))
				.andExpect(jsonPath("$.preferenceMatch.criteria[1].status").value("MATCH"))
				.andExpect(jsonPath("$.preferenceMatch.criteria[3].status").value("PARTIAL"))
				.andExpect(jsonPath("$.preferenceMatch.score").value(75));

		mockMvc.perform(get("/api/v1/analyses/recent-jobs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.company == 'Acme Brasil')].preferenceScore", org.hamcrest.Matchers.contains(75)));
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
				stream.showText("Desenvolvedora Backend Java na Empresa X, 2020 a 2023");
				stream.endText();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			assertThat(out.toByteArray()).isNotEmpty();
			return out.toByteArray();
		}
	}
}
