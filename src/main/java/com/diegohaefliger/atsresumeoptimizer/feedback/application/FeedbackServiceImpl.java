package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackReceipt;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
class FeedbackServiceImpl implements FeedbackService {

	private final FeedbackRepository repository;
	private final FeedbackAttachmentRepository attachmentRepository;
	private final FeedbackEntityMapper mapper;
	private final FeedbackGateway gateway;
	private final FeedbackProperties properties;
	private final Clock clock;

	FeedbackServiceImpl(FeedbackRepository repository, FeedbackAttachmentRepository attachmentRepository,
			FeedbackEntityMapper mapper, FeedbackGateway gateway, FeedbackProperties properties, Clock clock) {
		this.repository = repository;
		this.attachmentRepository = attachmentRepository;
		this.mapper = mapper;
		this.gateway = gateway;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public FeedbackReceipt submit(FeedbackSubmission submission, List<FeedbackAttachment> attachments) {
		FeedbackAttachmentRules.validate(attachments);
		FeedbackEntity saved = repository.save(
				new FeedbackEntity(UuidCreator.getTimeOrderedEpoch(), Instant.now(clock), submission));
		attachmentRepository.saveAll(IntStream.range(0, attachments.size())
				.mapToObj(position -> new FeedbackAttachmentEntity(UuidCreator.getTimeOrderedEpoch(), saved.getId(),
						position, attachments.get(position)))
				.toList());
		Set<UUID> delivered = repository
				.findByDeliveredAtIsNullOrderByCreatedAtDesc(Limit.of(properties.maxPendingRetries()))
				.stream()
				.filter(this::deliver)
				.map(FeedbackEntity::getId)
				.collect(Collectors.toSet());
		return new FeedbackReceipt(saved.getCreatedAt(), delivered.contains(saved.getId()));
	}

	private boolean deliver(FeedbackEntity entity) {
		List<FeedbackAttachmentEntity> stored = attachmentRepository.findByFeedbackIdOrderByPosition(entity.getId());
		List<FeedbackAttachment> attachments = stored.stream().map(mapper::toAttachment).toList();
		if (!gateway.deliver(entity.getId(), mapper.toSubmission(entity), attachments)) {
			return false;
		}
		entity.markDelivered(Instant.now(clock));
		repository.save(entity);
		attachmentRepository.deleteAll(stored);
		return true;
	}
}
