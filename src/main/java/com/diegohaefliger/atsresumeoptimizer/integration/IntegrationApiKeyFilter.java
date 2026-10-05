package com.diegohaefliger.atsresumeoptimizer.integration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class IntegrationApiKeyFilter extends OncePerRequestFilter {

	static final String HEADER = "X-API-Key";
	static final String PATH_PREFIX = "/api/v1/integrations/";
	private static final String UNAUTHORIZED_BODY =
			"{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"API key ausente ou inválida.\"}";

	private final byte[] expectedKey;

	IntegrationApiKeyFilter(@Value("${app.integration.api-key:}") String apiKey) {
		this.expectedKey = apiKey.strip().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith(PATH_PREFIX);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String received = request.getHeader(HEADER);
		if (expectedKey.length == 0 || received == null
				|| !MessageDigest.isEqual(expectedKey, received.strip().getBytes(StandardCharsets.UTF_8))) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.getWriter().write(UNAUTHORIZED_BODY);
			return;
		}
		chain.doFilter(request, response);
	}
}
