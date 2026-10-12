package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Contra DNS rebinding: um site que aponte o próprio domínio para 127.0.0.1 chega aqui com outro Host.
@Component
@Profile("desktop")
class LocalHostOnlyFilter extends OncePerRequestFilter {

	private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!LOCAL_HOSTS.contains(request.getServerName())) {
			response.sendError(HttpStatus.FORBIDDEN.value());
			return;
		}
		chain.doFilter(request, response);
	}
}
