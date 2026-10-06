package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.Notification;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository repository;
	private final NotificationEntityMapper mapper;
	private final Clock clock;

	NotificationServiceImpl(NotificationRepository repository, NotificationEntityMapper mapper, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> list(boolean unreadOnly, int limit) {
		Limit page = Limit.of(limit);
		List<NotificationEntity> entities = unreadOnly
				? repository.findByReadAtIsNullOrderByCreatedAtDesc(page)
				: repository.findAllByOrderByCreatedAtDesc(page);
		return entities.stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public long unreadCount() {
		return repository.countByReadAtIsNull();
	}

	@Override
	@Transactional
	public Notification markRead(UUID id) {
		NotificationEntity entity = find(id);
		if (entity.getReadAt() == null) {
			entity.setReadAt(Instant.now(clock));
		}
		return mapper.toDomain(entity);
	}

	@Override
	@Transactional
	public int markAllRead() {
		return repository.markAllRead(Instant.now(clock));
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		repository.delete(find(id));
	}

	private NotificationEntity find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
	}
}
