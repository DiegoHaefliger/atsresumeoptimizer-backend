package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("desktop")
class FrontendConfig implements WebMvcConfigurer {

	private final String frontendLocation;

	FrontendConfig(@Value("${app.desktop.frontend-location}") String frontendLocation) {
		this.frontendLocation = frontendLocation;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations(frontendLocation)
				.resourceChain(true)
				.addResolver(new SpaResourceResolver());
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("forward:/index.html");
	}
}
