package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ExportedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeFormat;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeCompositionService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeEditingService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumeCompositionController.class)
@Import(RewriteWebMapperImpl.class)
class ResumeCompositionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResumeCompositionService compositionService;

	@MockitoBean
	private ResumeEditingService editingService;

	@Test
	void createsTheResumeAndReturnsBothGeneratedVersions() throws Exception {
		UUID resumeId = UUID.randomUUID();
		UUID docxId = UUID.randomUUID();
		UUID pdfId = UUID.randomUUID();
		when(compositionService.compose(eq(ResumeTemplate.CLASSIC), any(), any()))
				.thenReturn(new ComposedResume(resumeId, docxId, pdfId));

		mockMvc.perform(post("/api/v1/resumes/composed").contentType("application/json").content("""
				{"template":"CLASSIC","content":{"name":"Ana Silva","sections":[]},"contact":{"email":"ana@email.com"}}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.resumeId").value(resumeId.toString()))
				.andExpect(jsonPath("$.documents.docxVersionId").value(docxId.toString()))
				.andExpect(jsonPath("$.documents.pdfVersionId").value(pdfId.toString()));
	}

	@Test
	void rejectsARequestWithoutContent() throws Exception {
		mockMvc.perform(post("/api/v1/resumes/composed").contentType("application/json")
				.content("{\"template\":\"CLASSIC\",\"contact\":{}}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsTheEditableContentOfASavedVersion() throws Exception {
		UUID resumeId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		when(editingService.load(resumeId, versionId)).thenReturn(new EditableResume("Ana.pdf", ResumeTemplate.MODERN_BLUE,
				new StructuredResume("Ana Silva", null, List.of()), new ResumeContact(null, null, null, null, null, null),
				true));

		mockMvc.perform(get("/api/v1/resumes/{resumeId}/versions/{versionId}/editable", resumeId, versionId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Ana.pdf"))
				.andExpect(jsonPath("$.template").value("MODERN_BLUE"))
				.andExpect(jsonPath("$.content.name").value("Ana Silva"))
				.andExpect(jsonPath("$.importedFromFile").value(true));
	}

	@Test
	void savesTheEditionAndReturnsTheNewVersions() throws Exception {
		UUID resumeId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		UUID pdfId = UUID.randomUUID();
		when(editingService.save(eq(resumeId), eq(versionId), eq("Novo nome"), eq(ResumeTemplate.CLASSIC), any(), any()))
				.thenReturn(new ComposedResume(resumeId, UUID.randomUUID(), pdfId));

		mockMvc.perform(put("/api/v1/resumes/{resumeId}/versions/{versionId}/editable", resumeId, versionId)
				.contentType("application/json")
				.content("{\"title\":\"Novo nome\",\"template\":\"CLASSIC\",\"content\":{\"name\":\"Ana Silva\"},\"contact\":{}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documents.pdfVersionId").value(pdfId.toString()));
	}

	@Test
	void exportsTheVersionInTheRequestedFormat() throws Exception {
		UUID resumeId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		when(editingService.export(resumeId, versionId, ResumeFormat.PDF))
				.thenReturn(new ExportedResume(new byte[] {1, 2}, "Ana.pdf", "application/pdf"));

		mockMvc.perform(get("/api/v1/resumes/{resumeId}/versions/{versionId}/export?format=PDF", resumeId, versionId))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Ana.pdf")))
				.andExpect(content().bytes(new byte[] {1, 2}));
	}

	@Test
	void rendersThePdfPreviewOfTheDraft() throws Exception {
		when(compositionService.previewPdf(eq(ResumeTemplate.CLASSIC), any(), any())).thenReturn(new byte[] {9});

		mockMvc.perform(post("/api/v1/resumes/preview").contentType("application/json")
				.content("{\"template\":\"CLASSIC\",\"content\":{\"name\":\"Ana\"},\"contact\":{}}"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pdf"))
				.andExpect(content().bytes(new byte[] {9}));
	}
}
