package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ComposedResumeWithoutNameException;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeCompositionServiceImplTest {

	private static final ResumeContact CONTACT = new ResumeContact("ana@email.com", null, null, null, null, null);

	@Mock
	private ResumeDocumentWriter documentWriter;

	@InjectMocks
	private ResumeCompositionServiceImpl service;

	@Test
	void storesTheComposedContentAsANewResumeTitledWithTheCandidateName() {
		UUID resumeId = UUID.randomUUID();
		UUID docxId = UUID.randomUUID();
		UUID pdfId = UUID.randomUUID();
		when(documentWriter.writeNew(eq("Ana Silva"), eq(ResumeTemplate.MODERN_BLUE), any(), eq(CONTACT)))
				.thenReturn(new StoredResumeDocuments(resumeId, docxId, pdfId, new byte[0]));

		ComposedResume composed = service.compose(ResumeTemplate.MODERN_BLUE,
				new StructuredResume("Ana  Silva", "Backend", List.of()), CONTACT);

		assertThat(composed).isEqualTo(new ComposedResume(resumeId, docxId, pdfId));
	}

	@Test
	void rejectsAResumeWithoutTheCandidateName() {
		assertThatThrownBy(() -> service.compose(ResumeTemplate.CLASSIC, new StructuredResume("  ", null, List.of()), CONTACT))
				.isInstanceOf(ComposedResumeWithoutNameException.class);
		verifyNoInteractions(documentWriter);
	}
}
