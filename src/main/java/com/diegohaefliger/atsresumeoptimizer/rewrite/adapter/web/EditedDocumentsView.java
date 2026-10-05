package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import java.util.UUID;

record EditedDocumentsView(UUID resumeId, DocumentDownloadsView documents) {
}
