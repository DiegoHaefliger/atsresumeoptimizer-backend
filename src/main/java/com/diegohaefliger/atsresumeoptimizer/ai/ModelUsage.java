package com.diegohaefliger.atsresumeoptimizer.ai;

import java.math.BigDecimal;

/** Consumo de IA persistido de um modelo; {@code cachedCalls} são as chamadas respondidas pelo cache, sem token nem custo. */
public record ModelUsage(String model, long tokensIn, long tokensOut, BigDecimal costUsd, long calls, long cachedCalls) {
}
