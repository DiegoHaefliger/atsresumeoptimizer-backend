package com.diegohaefliger.atsresumeoptimizer.ai.application;

public record AiConnectionTestResult(boolean success, AiProvider provider, String model, long latencyMs, String message) {
}
