package com.diegohaefliger.atsresumeoptimizer.desktop.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class DesktopDatabaseException extends BusinessException {

	private final HttpStatus status;

	public DesktopDatabaseException(HttpStatus status, String message, Throwable cause) {
		super(message);
		this.status = status;
		initCause(cause);
	}

	public static DesktopDatabaseException unreachable(ExternalDatabase database, Throwable cause) {
		return new DesktopDatabaseException(HttpStatus.BAD_GATEWAY,
				"Não deu pra conectar em " + database.address() + ": " + cause.getMessage(), cause);
	}

	public static DesktopDatabaseException notEmpty(ExternalDatabase database) {
		return new DesktopDatabaseException(HttpStatus.CONFLICT, "O banco " + database.address()
				+ " já tem dados. Para copiar os seus dados, use um banco vazio.", null);
	}

	@Override
	public HttpStatus status() {
		return status;
	}
}
