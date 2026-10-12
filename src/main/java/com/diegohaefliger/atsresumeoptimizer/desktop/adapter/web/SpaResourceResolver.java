package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

class SpaResourceResolver extends PathResourceResolver {

	private static final String INDEX = "index.html";
	private static final List<String> SERVER_PREFIXES = List.of("api/", "v3/", "swagger-ui", "actuator");

	@Override
	protected Resource getResource(String resourcePath, Resource location) throws IOException {
		Resource resource = super.getResource(resourcePath, location);
		if (resource != null || SERVER_PREFIXES.stream().anyMatch(resourcePath::startsWith)) {
			return resource;
		}
		return super.getResource(INDEX, location);
	}
}
