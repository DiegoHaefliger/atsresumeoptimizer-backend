package com.diegohaefliger.atsresumeoptimizer;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	static final String ERROR_ID = "errorId";

	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String UNEXPECTED_DETAIL =
			"Algo deu errado no servidor. Se continuar acontecendo, envie o código do erro para o suporte.";

	@ExceptionHandler(BusinessException.class)
	ProblemDetail handleBusinessException(BusinessException exception) {
		LOG.warn("Erro de negócio {}: {}", exception.status(), exception.getMessage(), exception.getCause());
		return withErrorId(ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage()));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
		return withErrorId(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
		if (exception instanceof ErrorResponse known) {
			if (known.getStatusCode().is5xxServerError()) {
				LOG.error("Falha {} na requisição", known.getStatusCode(), exception);
			}
			return ResponseEntity.status(known.getStatusCode()).headers(known.getHeaders()).body(withErrorId(known.getBody()));
		}
		LOG.error("Erro inesperado", exception);
		return ResponseEntity.internalServerError()
				.body(withErrorId(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_DETAIL)));
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
			WebRequest request) {
		if (body instanceof ProblemDetail problem) {
			withErrorId(problem);
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	private static ProblemDetail withErrorId(ProblemDetail problem) {
		RequestId.current().ifPresent(id -> problem.setProperty(ERROR_ID, id));
		return problem;
	}
}
