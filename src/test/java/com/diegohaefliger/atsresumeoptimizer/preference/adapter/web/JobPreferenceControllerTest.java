package com.diegohaefliger.atsresumeoptimizer.preference.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.application.JobPreferenceService;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobPreferenceController.class)
@Import(JobPreferenceWebMapperImpl.class)
class JobPreferenceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobPreferenceService preferenceService;

	@Test
	void returnsTheCurrentPreferences() throws Exception {
		when(preferenceService.current()).thenReturn(new JobPreference(List.of(WorkModel.REMOTE, WorkModel.HYBRID),
				List.of(ContractType.CLT), new BigDecimal("8000"), new BigDecimal("10000"), List.of("PLR"), List.of(),
				List.of(), List.of(), List.of()));

		mockMvc.perform(get("/api/v1/job-preference"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workModels[0]").value("REMOTE"))
				.andExpect(jsonPath("$.workModels[1]").value("HYBRID"))
				.andExpect(jsonPath("$.minSalary").value(8000))
				.andExpect(jsonPath("$.benefits[0]").value("PLR"));
	}

	@Test
	void savesThePreferencesAndReturnsTheStoredVersion() throws Exception {
		when(preferenceService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(put("/api/v1/job-preference")
						.contentType("application/json")
						.content("""
								{"workModels":["REMOTE"],"contractTypes":["PJ"],"minSalary":9000,
								 "benefits":["  PLR  ",""],"avoidedCompanies":["Acme"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contractTypes[0]").value("PJ"))
				.andExpect(jsonPath("$.benefits.length()").value(1))
				.andExpect(jsonPath("$.benefits[0]").value("PLR"))
				.andExpect(jsonPath("$.seniorities.length()").value(0));
	}

	@Test
	void rejectsADesiredSalaryBelowTheMinimum() throws Exception {
		mockMvc.perform(put("/api/v1/job-preference")
						.contentType("application/json")
						.content("{\"minSalary\":9000,\"desiredSalary\":5000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Salário desejado não pode ser menor que o salário mínimo."));

		verifyNoInteractions(preferenceService);
	}

	@Test
	void rejectsANegativeSalary() throws Exception {
		mockMvc.perform(put("/api/v1/job-preference")
						.contentType("application/json")
						.content("{\"minSalary\":-1}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(preferenceService);
	}

	@Test
	void savesTheSearchSettingsAlongWithThePreferences() throws Exception {
		when(preferenceService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(put("/api/v1/job-preference")
						.contentType("application/json")
						.content("""
								{"searchKeywords":["  Java  ",""],"searchPostedWithin":"PAST_MONTH",
								 "searchMaxResults":40,"searchMinAdherence":60,"searchEnabled":true,"searchIntervalHours":24}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.searchKeywords.length()").value(1))
				.andExpect(jsonPath("$.searchKeywords[0]").value("Java"))
				.andExpect(jsonPath("$.searchPostedWithin").value("PAST_MONTH"))
				.andExpect(jsonPath("$.searchMaxResults").value(40))
				.andExpect(jsonPath("$.searchMinAdherence").value(60))
				.andExpect(jsonPath("$.searchEnabled").value(true))
				.andExpect(jsonPath("$.searchIntervalHours").value(24));
	}

	@Test
	void appliesTheSearchDefaultsWhenTheyAreOmitted() throws Exception {
		when(preferenceService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(put("/api/v1/job-preference").contentType("application/json").content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.searchPostedWithin").value("PAST_WEEK"))
				.andExpect(jsonPath("$.searchMaxResults").value(50))
				.andExpect(jsonPath("$.searchMinAdherence").value(70))
				.andExpect(jsonPath("$.searchEnabled").value(false))
				.andExpect(jsonPath("$.searchIntervalHours").value(12));
	}

	@Test
	void rejectsEnablingTheSearchWithoutKeywords() throws Exception {
		mockMvc.perform(put("/api/v1/job-preference")
						.contentType("application/json")
						.content("{\"searchEnabled\":true}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Informe ao menos um cargo ou palavra-chave para ativar a busca."));

		verifyNoInteractions(preferenceService);
	}

	@Test
	void rejectsAnUnsupportedSearchInterval() throws Exception {
		mockMvc.perform(put("/api/v1/job-preference").contentType("application/json").content("{\"searchIntervalHours\":7}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(preferenceService);
	}

	@Test
	void rejectsSearchLimitsOutOfRange() throws Exception {
		mockMvc.perform(put("/api/v1/job-preference").contentType("application/json").content("{\"searchMaxResults\":500}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/v1/job-preference").contentType("application/json").content("{\"searchMinAdherence\":101}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(preferenceService);
	}
}
