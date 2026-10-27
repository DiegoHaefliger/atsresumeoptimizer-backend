package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidPairingTokenException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PairingTokenServiceImplTest {

	@Mock
	private PairingTokenRepository repository;

	@Test
	void createsATokenOnFirstUse() {
		when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(new PairingTokenServiceImpl(repository).current()).hasSizeGreaterThanOrEqualTo(40);
	}

	@Test
	void replacesTheTokenOnRegenerate() {
		var existing = new PairingTokenEntity(UUID.randomUUID(), "old");
		when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(new PairingTokenServiceImpl(repository).regenerate()).isNotEqualTo("old");
	}

	@Test
	void acceptsOnlyTheCurrentToken() {
		when(repository.findFirstByOrderByCreatedAtDesc())
				.thenReturn(Optional.of(new PairingTokenEntity(UUID.randomUUID(), "secret")));
		var service = new PairingTokenServiceImpl(repository);

		assertThatCode(() -> service.verify(" secret ")).doesNotThrowAnyException();
		assertThatThrownBy(() -> service.verify("other")).isInstanceOf(InvalidPairingTokenException.class);
		assertThatThrownBy(() -> service.verify(null)).isInstanceOf(InvalidPairingTokenException.class);
	}
}
