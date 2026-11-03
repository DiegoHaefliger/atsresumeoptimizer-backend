package com.diegohaefliger.atsresumeoptimizer.feedback.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.feedback.application.FeedbackService;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackKind;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.InvalidFeedbackAttachmentException;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackReceipt;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(FeedbackController.class)
@Import(FeedbackWebMapperImpl.class)
class FeedbackControllerTest {

	private static final String URL = "/api/v1/feedback";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FeedbackService service;

	private static MockMultipartHttpServletRequestBuilder form(String kind, String rating) {
		MockMultipartHttpServletRequestBuilder request = multipart(URL);
		if (kind != null) {
			request.param("kind", kind);
		}
		if (rating != null) {
			request.param("rating", rating);
		}
		return request;
	}

	@Test
	void forwardsTheSubmissionWithItsImagesAndReportsDelivery() throws Exception {
		FeedbackSubmission submission = new FeedbackSubmission(FeedbackKind.TESTIMONIAL, 5, "Me ajudou muito",
				"ana@exemplo.com", true, "1.4.0");
		byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1};
		when(service.submit(eq(submission), any())).thenReturn(new FeedbackReceipt(Instant.parse("2026-11-03T12:00:00Z"), true));

		mockMvc.perform(form("TESTIMONIAL", "5")
				.param("message", "Me ajudou muito").param("contact", "ana@exemplo.com")
				.param("allowPublish", "true").param("appVersion", "1.4.0")
				.file(new MockMultipartFile("files", "tela.png", "image/png", png)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.delivered").value(true));

		ArgumentCaptor<List<FeedbackAttachment>> attachments = ArgumentCaptor.captor();
		verify(service).submit(eq(submission), attachments.capture());
		assertThat(attachments.getValue()).singleElement().satisfies(image -> {
			assertThat(image.contentType()).isEqualTo("image/png");
			assertThat(image.content()).isEqualTo(png);
		});
	}

	@Test
	void acceptsARatingWithoutTextOrImages() throws Exception {
		when(service.submit(any(), any())).thenReturn(new FeedbackReceipt(Instant.now(), false));

		mockMvc.perform(form("GENERAL", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.delivered").value(false));
	}

	@Test
	void answersBadRequestWhenTheServiceRefusesAnImage() throws Exception {
		when(service.submit(any(), any())).thenThrow(new InvalidFeedbackAttachmentException("Use imagens PNG, JPG ou WebP."));

		mockMvc.perform(form("GENERAL", "4")
				.file(new MockMultipartFile("files", "x.png", "image/png", new byte[] {1})))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsRatingsOutsideOneToFive() throws Exception {
		for (int rating : new int[] {0, 6}) {
			mockMvc.perform(form("GENERAL", String.valueOf(rating)))
					.andExpect(status().isBadRequest());
		}
		verifyNoInteractions(service);
	}

	@Test
	void rejectsAMissingRatingOrKind() throws Exception {
		mockMvc.perform(form("GENERAL", null)).andExpect(status().isBadRequest());
		mockMvc.perform(form(null, "3")).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAnInvalidContactAndATextOverTheLimit() throws Exception {
		mockMvc.perform(form("GENERAL", "3").param("contact", "não é e-mail"))
				.andExpect(status().isBadRequest());

		String tooLong = "a".repeat(FeedbackRequest.MAX_MESSAGE + 1);
		mockMvc.perform(form("GENERAL", "3").param("message", tooLong))
				.andExpect(status().isBadRequest());
	}
}
