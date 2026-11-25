package com.diegohaefliger.atsresumeoptimizer.diagnostics.domain;

public record ClientError(String message, String stack, String componentStack, String page) {
}
