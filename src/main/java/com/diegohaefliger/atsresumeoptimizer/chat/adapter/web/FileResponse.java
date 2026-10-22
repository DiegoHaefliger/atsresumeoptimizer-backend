package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;
import java.util.UUID;

record FileResponse(UUID id, ChatFileKind kind, String fileName, String mimeType, long sizeBytes) {
}
