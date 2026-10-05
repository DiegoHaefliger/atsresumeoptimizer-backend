package com.diegohaefliger.atsresumeoptimizer.language.domain;

import java.util.List;

public record GrammarIssue(String message, int startOffset, int endOffset, List<String> suggestedReplacements) {
}
