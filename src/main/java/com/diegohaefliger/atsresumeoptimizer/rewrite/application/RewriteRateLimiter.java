package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

/** Ferramenta local, uma instância só — um bucket global basta, sem chave por usuário. */
@Component
class RewriteRateLimiter {

	private final Bucket bucket;

	RewriteRateLimiter(RewriteRateLimitProperties properties) {
		this.bucket = Bucket.builder().addLimit(Bandwidth.simple(properties.capacity(), properties.period())).build();
	}

	boolean tryConsume() {
		return bucket.tryConsume(1);
	}
}
