package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Em memória: MVP roda numa instância só, sem Redis. */
@Component
class AnalysisEventEmitterRegistry {

	private static final long TIMEOUT_MILLIS = Duration.ofMinutes(5).toMillis();

	private final Map<UUID, List<SseEmitter>> emittersByAnalysis = new ConcurrentHashMap<>();

	SseEmitter register(UUID analysisId) {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
		List<SseEmitter> emitters = emittersByAnalysis.computeIfAbsent(analysisId, key -> new CopyOnWriteArrayList<>());
		emitters.add(emitter);
		emitter.onCompletion(() -> remove(analysisId, emitter));
		emitter.onTimeout(() -> remove(analysisId, emitter));
		return emitter;
	}

	void publish(UUID analysisId, AnalysisStatus status) {
		List<SseEmitter> emitters = emittersByAnalysis.get(analysisId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name("status").data(status.name()));
			} catch (IOException exception) {
				emitter.completeWithError(exception);
			}
		}
	}

	void complete(UUID analysisId) {
		List<SseEmitter> emitters = emittersByAnalysis.remove(analysisId);
		if (emitters == null) {
			return;
		}
		emitters.forEach(SseEmitter::complete);
	}

	private void remove(UUID analysisId, SseEmitter emitter) {
		List<SseEmitter> emitters = emittersByAnalysis.get(analysisId);
		if (emitters != null) {
			emitters.remove(emitter);
		}
	}
}
