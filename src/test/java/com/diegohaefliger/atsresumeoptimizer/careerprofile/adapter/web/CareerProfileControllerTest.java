package com.diegohaefliger.atsresumeoptimizer.careerprofile.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfileService;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CareerProfileController.class)
@Import(CareerProfileWebMapperImpl.class)
class CareerProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CareerProfileService service;

	@Test
	void returnsNotFoundWhenTheProfileWasNeverConfigured() throws Exception {
		when(service.current()).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/career-profile")).andExpect(status().isNotFound());
	}

	@Test
	void returnsTheCurrentProfile() throws Exception {
		when(service.current()).thenReturn(Optional.of(new CareerProfile("Engenharia de software", null, null,
				"Tech Lead", null, null, List.of(), List.of(), null, List.of(), List.of("Java"), null, null, null, null)));

		mockMvc.perform(get("/api/v1/career-profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetJobTitle").value("Tech Lead"))
				.andExpect(jsonPath("$.strengths[0]").value("Java"))
				.andExpect(jsonPath("$.tone").value("TECHNICAL"));
	}

	@Test
	void savesAndReturnsTheStoredVersion() throws Exception {
		when(service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(put("/api/v1/career-profile")
						.contentType("application/json")
						.content("""
								{"areaOfExpertise":"  Engenharia de software ","targetJobTitle":"Tech Lead",
								 "tone":"EXECUTIVE","strengths":[" Java ",""]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.areaOfExpertise").value("Engenharia de software"))
				.andExpect(jsonPath("$.tone").value("EXECUTIVE"))
				.andExpect(jsonPath("$.strengths.length()").value(1));
	}

	@Test
	void rejectsAProfileWithoutTheTargetJobTitle() throws Exception {
		mockMvc.perform(put("/api/v1/career-profile")
						.contentType("application/json")
						.content("{\"areaOfExpertise\":\"Engenharia de software\"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}
}
