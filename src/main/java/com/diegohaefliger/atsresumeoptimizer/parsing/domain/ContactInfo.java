package com.diegohaefliger.atsresumeoptimizer.parsing.domain;

import java.util.Optional;

public record ContactInfo(
		Optional<String> email,
		Optional<String> phone,
		Optional<String> linkedInProfile,
		Optional<String> githubProfile,
		Optional<String> location,
		Optional<String> portfolio) {
}
