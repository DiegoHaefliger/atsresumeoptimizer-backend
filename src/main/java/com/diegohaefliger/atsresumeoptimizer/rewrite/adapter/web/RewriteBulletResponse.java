package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

public record RewriteBulletResponse(String original, String rewritten, boolean needsConfirmation) {
}
