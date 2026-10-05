package com.diegohaefliger.atsresumeoptimizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ProblemDetail handleBusinessException(BusinessException exception) {
		LOG.warn("Erro de negócio {}: {}", exception.status(), exception.getMessage(), exception.getCause());
		return ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
	}
}
