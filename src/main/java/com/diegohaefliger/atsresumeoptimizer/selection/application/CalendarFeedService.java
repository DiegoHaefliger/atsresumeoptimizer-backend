package com.diegohaefliger.atsresumeoptimizer.selection.application;

import java.util.Optional;

public interface CalendarFeedService {

	Optional<String> token();

	String regenerate();

	void disable();

	boolean accepts(String token);
}
