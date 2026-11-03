package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackKind;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.InvalidFeedbackAttachmentException;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackReceipt;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
	private static final FeedbackSubmission SUBMISSION = new FeedbackSubmission(FeedbackKind.SUGGESTION, 4,
			"Poderia exportar em Word", null, false, "1.4.0");

	@Mock
	private FeedbackRepository repository;

	@Mock
	private FeedbackAttachmentRepository attachmentRepository;

	@Mock
	private FeedbackGateway gateway;

	private final AtomicReference<FeedbackEntity> saved = new AtomicReference<>();

	@BeforeEach
	void rememberTheFirstSavedEntity() {
		org.mockito.Mockito.lenient().when(repository.save(any(FeedbackEntity.class))).thenAnswer(call -> {
			saved.compareAndSet(null, call.getArgument(0));
			return call.getArgument(0);
		});
	}

	private FeedbackServiceImpl service() {
		FeedbackProperties properties = new FeedbackProperties("https://projeto.supabase.co", "chave", "feedback", "feedback-screenshots",
				Duration.ofSeconds(5), 20);
		return new FeedbackServiceImpl(repository, attachmentRepository, Mappers.getMapper(FeedbackEntityMapper.class), gateway,
				properties, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void pendingIs(java.util.function.Supplier<List<FeedbackEntity>> pending) {
		when(repository.findByDeliveredAtIsNullOrderByCreatedAtDesc(any(Limit.class)))
				.thenAnswer(call -> pending.get());
	}

	@Test
	void marksTheFeedbackAsDeliveredWhenTheSendWorks() {
		pendingIs(() -> List.of(saved.get()));
		when(gateway.deliver(any(), eq(SUBMISSION), any())).thenReturn(true);

		FeedbackReceipt receipt = service().submit(SUBMISSION, List.of());

		assertThat(receipt.delivered()).isTrue();
		assertThat(receipt.createdAt()).isEqualTo(NOW);
		assertThat(saved.get().isDelivered()).isTrue();
	}

	@Test
	void keepsTheFeedbackLocalWhenTheSendFails() {
		pendingIs(() -> List.of(saved.get()));
		when(gateway.deliver(any(), eq(SUBMISSION), any())).thenReturn(false);

		FeedbackReceipt receipt = service().submit(SUBMISSION, List.of());

		assertThat(receipt.delivered()).isFalse();
		assertThat(saved.get().isDelivered()).isFalse();
		verify(repository, times(1)).save(any(FeedbackEntity.class));
	}

	@Test
	void retriesOlderPendingFeedbackWithoutLosingTheNewOne() {
		FeedbackEntity older = new FeedbackEntity(UUID.randomUUID(), NOW.minusSeconds(3600), SUBMISSION);
		pendingIs(() -> List.of(saved.get(), older));
		when(gateway.deliver(any(), eq(SUBMISSION), any())).thenReturn(false, true);

		FeedbackReceipt receipt = service().submit(SUBMISSION, List.of());

		assertThat(receipt.delivered()).isFalse();
		assertThat(older.isDelivered()).isTrue();
		verify(gateway, times(2)).deliver(any(), eq(SUBMISSION), any());
	}

	@Test
	void doesNotCallTheGatewayWhenNothingIsPending() {
		pendingIs(List::of);

		assertThat(service().submit(SUBMISSION, List.of()).delivered()).isFalse();
		verify(gateway, never()).deliver(any(), any(), any());
	}

	@Test
	void keepsTheImagesAndDropsThemLocallyOnceDelivered() {
		FeedbackAttachment image = new FeedbackAttachment("tela.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 1});
		FeedbackAttachmentEntity stored = new FeedbackAttachmentEntity(UUID.randomUUID(), UUID.randomUUID(), 0, image);
		pendingIs(() -> List.of(saved.get()));
		when(attachmentRepository.findByFeedbackIdOrderByPosition(any())).thenReturn(List.of(stored));
		when(gateway.deliver(any(), eq(SUBMISSION), any())).thenReturn(true);

		service().submit(SUBMISSION, List.of(image));

		verify(attachmentRepository).saveAll(any());
		verify(attachmentRepository).deleteAll(List.of(stored));
	}

	@Test
	void keepsTheImagesLocallyWhenTheSendFails() {
		pendingIs(() -> List.of(saved.get()));
		when(attachmentRepository.findByFeedbackIdOrderByPosition(any())).thenReturn(List.of());
		when(gateway.deliver(any(), eq(SUBMISSION), any())).thenReturn(false);

		service().submit(SUBMISSION, List.of());

		verify(attachmentRepository, never()).deleteAll(any());
	}

	@Test
	void refusesTooManyOrNonImageFiles() {
		FeedbackAttachment fake = new FeedbackAttachment("x.png", "image/png", "não sou imagem".getBytes());
		FeedbackAttachment ok = new FeedbackAttachment("a.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 1});

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().submit(SUBMISSION, List.of(fake)))
				.isInstanceOf(InvalidFeedbackAttachmentException.class);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().submit(SUBMISSION, List.of(ok, ok, ok, ok)))
				.isInstanceOf(InvalidFeedbackAttachmentException.class);
		verify(repository, never()).save(any());
	}
}
