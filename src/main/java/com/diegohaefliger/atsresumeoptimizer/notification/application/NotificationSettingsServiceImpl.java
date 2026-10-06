package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.InvalidNotificationSettingsException;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationSettingsServiceImpl implements NotificationSettingsService {

	static final int MAX_LEAD_ENTRIES = 5;
	static final int MAX_LEAD_MINUTES = 43_200;

	private final NotificationSettingsRepository repository;
	private final NotificationSettingsEntityMapper mapper;
	private final Clock clock;

	NotificationSettingsServiceImpl(
			NotificationSettingsRepository repository, NotificationSettingsEntityMapper mapper, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public NotificationSettings current() {
		return repository.findFirstByOrderByUpdatedAtDesc().map(mapper::toDomain).orElse(NotificationSettings.DEFAULT);
	}

	@Override
	@Transactional
	public NotificationSettings save(NotificationSettings settings) {
		validate(settings);
		NotificationSettingsEntity entity = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new NotificationSettingsEntity(UuidCreator.getTimeOrderedEpoch()));
		mapper.update(settings, entity);
		entity.setUpdatedAt(Instant.now(clock));
		repository.save(entity);
		return mapper.toDomain(entity);
	}

	private static void validate(NotificationSettings settings) {
		if (settings.leadMinutes().isEmpty() || settings.leadMinutes().size() > MAX_LEAD_ENTRIES) {
			throw new InvalidNotificationSettingsException(
					"Informe de 1 a %d antecedências.".formatted(MAX_LEAD_ENTRIES));
		}
		if (settings.leadMinutes().stream().anyMatch(minutes -> minutes < 1 || minutes > MAX_LEAD_MINUTES)) {
			throw new InvalidNotificationSettingsException(
					"A antecedência deve ficar entre 1 e %d minutos.".formatted(MAX_LEAD_MINUTES));
		}
		if (settings.leadMinutes().stream().distinct().count() != settings.leadMinutes().size()) {
			throw new InvalidNotificationSettingsException("Antecedências repetidas.");
		}
		if (settings.channels().isEmpty()) {
			throw new InvalidNotificationSettingsException("Habilite ao menos um canal.");
		}
		String unavailable = settings.channels().stream()
				.filter(channel -> !channel.available())
				.map(NotificationChannel::name)
				.sorted()
				.collect(Collectors.joining(", "));
		if (!unavailable.isEmpty()) {
			throw new InvalidNotificationSettingsException("Canal ainda indisponível: " + unavailable + ".");
		}
	}
}
