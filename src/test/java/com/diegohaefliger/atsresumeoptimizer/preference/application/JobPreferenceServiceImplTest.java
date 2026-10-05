package com.diegohaefliger.atsresumeoptimizer.preference.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobPreferenceServiceImplTest {

	@Mock
	private JobPreferenceRepository repository;

	private final JobPreference preference = new JobPreference(List.of(WorkModel.REMOTE), List.of(ContractType.PJ),
			new BigDecimal("9000"), null, List.of("PLR"), List.of(), List.of(), List.of(), List.of("Acme"));

	@Test
	void returnsEmptyPreferencesWhenNothingWasSavedYet() {
		var service = new JobPreferenceServiceImpl(repository, new JobPreferenceEntityMapperImpl());
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		assertThat(service.current()).isEqualTo(JobPreference.EMPTY);
	}

	@Test
	void createsTheSingleRowOnFirstSaveAndReturnsWhatWasStored() {
		var service = new JobPreferenceServiceImpl(repository, new JobPreferenceEntityMapperImpl());
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		JobPreference saved = service.save(preference);

		ArgumentCaptor<JobPreferenceEntity> entity = ArgumentCaptor.forClass(JobPreferenceEntity.class);
		verify(repository).save(entity.capture());
		assertThat(entity.getValue().getWorkModels()).containsExactly("REMOTE");
		assertThat(entity.getValue().getUpdatedAt()).isNotNull();
		assertThat(saved).isEqualTo(preference);
	}

	@Test
	void overwritesTheExistingRowInsteadOfCreatingAnother() {
		var service = new JobPreferenceServiceImpl(repository, new JobPreferenceEntityMapperImpl());
		JobPreferenceEntity existing = new JobPreferenceEntity(UUID.randomUUID());
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));

		service.save(preference);

		verify(repository).save(existing);
		assertThat(existing.getAvoidedCompanies()).containsExactly("Acme");
		assertThat(existing.getContractTypes()).containsExactly("PJ");
	}
}
