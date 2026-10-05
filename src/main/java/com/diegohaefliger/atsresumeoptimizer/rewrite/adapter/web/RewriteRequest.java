package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;

record RewriteRequest(ResumeTemplate template, Boolean highlightJob) {
}
