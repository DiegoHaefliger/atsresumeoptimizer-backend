package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import java.util.UUID;

public record ChatFile(UUID id, ChatFileKind kind, String fileName, String mimeType, long sizeBytes) {
}
