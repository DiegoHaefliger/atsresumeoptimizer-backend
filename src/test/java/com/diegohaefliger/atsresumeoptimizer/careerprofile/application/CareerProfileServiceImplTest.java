package com.diegohaefliger.atsresumeoptimizer.careerprofile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.domain.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.domain.ProfileTone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareerProfileServiceImplTest {

	@Mock
	private CareerProfileRepository repository;

	private final CareerProfile profile = new CareerProfile("Engenharia de software", "Desenvolvedor Pleno", "Pleno",
			"Tech Lead", "Sênior", "12 meses", List.of("Fintech"), List.of("Nubank"), "São Paulo", List.of("Inglês"),
			List.of("Java"), "Arquitetura", ProfileTone.TECHNICAL, "pt-BR", null);

	private CareerProfileServiceImpl service() {
		return new CareerProfileServiceImpl(repository, new CareerProfileEntityMapperImpl());
	}

	@Test
	void returnsNothingWhenNoProfileWasSavedYet() {
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		assertThat(service().current()).isEmpty();
	}

	@Test
	void createsTheSingleRowOnFirstSave() {
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		CareerProfile saved = service().save(profile);

		ArgumentCaptor<CareerProfileEntity> entity = ArgumentCaptor.forClass(CareerProfileEntity.class);
		verify(repository).save(entity.capture());
		assertThat(entity.getValue().getTargetJobTitle()).isEqualTo("Tech Lead");
		assertThat(entity.getValue().getTone()).isEqualTo("TECHNICAL");
		assertThat(entity.getValue().getUpdatedAt()).isNotNull();
		assertThat(saved).isEqualTo(profile);
	}

	@Test
	void overwritesTheExistingRowInsteadOfCreatingAnother() {
		CareerProfileEntity existing = new CareerProfileEntity(UUID.randomUUID());
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));

		service().save(profile);

		verify(repository).save(existing);
		assertThat(existing.getTargetCompanies()).containsExactly("Nubank");
	}
}
