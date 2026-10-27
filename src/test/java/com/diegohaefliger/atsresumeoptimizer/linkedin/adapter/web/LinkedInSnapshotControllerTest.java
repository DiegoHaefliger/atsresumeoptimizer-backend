package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.linkedin.application.LinkedInSnapshotService;
import com.diegohaefliger.atsresumeoptimizer.linkedin.application.NewLinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.application.PairingTokenService;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidPairingTokenException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LinkedInSnapshotController.class)
@Import(LinkedInSnapshotWebMapperImpl.class)
class LinkedInSnapshotControllerTest {

	private static final String BODY = """
			{"profile":{"fullName":"Ana","headline":"Dev Java","skills":["Java"],
			 "experiences":[{"title":"Dev","organization":"Acme"}]},
			 "photoBase64":"AQID","photoMimeType":"image/png"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkedInSnapshotService snapshotService;

	@MockitoBean
	private PairingTokenService pairingService;

	private LinkedInSnapshot stored(NewLinkedInSnapshot command) {
		return new LinkedInSnapshot(UUID.randomUUID(), Instant.now(), command.source(), command.profile(),
				command.photo() == null ? null : command.photo().data(),
				command.photo() == null ? null : command.photo().mimeType(), null, null);
	}

	@Test
	void capturesTheSnapshotSentByTheExtension() throws Exception {
		when(snapshotService.capture(any())).thenAnswer(invocation -> stored(invocation.getArgument(0)));

		mockMvc.perform(post("/api/v1/linkedin/snapshots").header("X-Pairing-Token", "abc")
						.contentType("application/json").content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.source").value("EXTENSION"))
				.andExpect(jsonPath("$.profile.headline").value("Dev Java"))
				.andExpect(jsonPath("$.profile.experiences[0].organization").value("Acme"))
				.andExpect(jsonPath("$.hasPhoto").value(true))
				.andExpect(jsonPath("$.hasBanner").value(false))
				.andExpect(jsonPath("$.missingSections[0]").value("BANNER"));
	}

	@Test
	void rejectsTheExtensionWithoutAValidPairingToken() throws Exception {
		doThrow(new InvalidPairingTokenException()).when(pairingService).verify(any());

		mockMvc.perform(post("/api/v1/linkedin/snapshots").contentType("application/json").content(BODY))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(snapshotService);
	}

	@Test
	void acceptsPastedTextWithoutPairingToken() throws Exception {
		when(snapshotService.capture(any())).thenAnswer(invocation -> stored(invocation.getArgument(0)));

		mockMvc.perform(post("/api/v1/linkedin/snapshots/manual").contentType("application/json").content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.source").value("TEXT"));

		verifyNoInteractions(pairingService);
	}

	@Test
	void rejectsAnImageThatIsNotBase64() throws Exception {
		mockMvc.perform(post("/api/v1/linkedin/snapshots/manual").contentType("application/json")
						.content("{\"profile\":{\"headline\":\"x\"},\"photoBase64\":\"@@@\",\"photoMimeType\":\"image/png\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void servesTheStoredPhoto() throws Exception {
		UUID id = UUID.randomUUID();
		var profile = new LinkedInProfile(null, null, "x", null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null);
		when(snapshotService.get(id)).thenReturn(new LinkedInSnapshot(id, Instant.now(),
				com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource.EXTENSION, profile,
				new byte[] {1, 2, 3}, "image/png", null, null));

		mockMvc.perform(get("/api/v1/linkedin/snapshots/" + id + "/photo"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andExpect(content().bytes(new byte[] {1, 2, 3}));
		mockMvc.perform(get("/api/v1/linkedin/snapshots/" + id + "/banner")).andExpect(status().isNotFound());
	}
}
