package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileKind;

record PreparedChatFile(ChatFileKind kind, String fileName, String mimeType, long sizeBytes, byte[] content, String text) {
}
