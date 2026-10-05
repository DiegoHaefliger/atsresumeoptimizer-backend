package com.diegohaefliger.atsresumeoptimizer.parsing.domain;

/** {@code rawText} é a leitura ingênua de um ATS simples; divergir do {@code structuredText} é sinal de risco de parse. */
public record NormalizedDocument(SourceFormat sourceFormat, String rawText, String structuredText, Integer pageCount) {
}
