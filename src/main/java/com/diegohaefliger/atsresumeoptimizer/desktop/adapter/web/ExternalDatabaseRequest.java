package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.SslMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record ExternalDatabaseRequest(
		@NotBlank @Size(max = 255) String host,
		@Min(1) @Max(65535) int port,
		@NotBlank @Size(max = 63) String database,
		@NotBlank @Size(max = 63) String username,
		@NotNull @Size(max = 255) String password,
		@NotNull SslMode sslMode,
		boolean copyData) {
}
