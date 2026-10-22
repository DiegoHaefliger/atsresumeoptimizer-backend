package com.diegohaefliger.atsresumeoptimizer.chat.domain;

public record ChatFileContent(String mimeType, String fileName, byte[] content) {
}
