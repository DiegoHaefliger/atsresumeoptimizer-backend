package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class CalendarFeedUnavailableException extends BusinessException {

	public CalendarFeedUnavailableException() {
		super("Feed de agenda indisponível.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
