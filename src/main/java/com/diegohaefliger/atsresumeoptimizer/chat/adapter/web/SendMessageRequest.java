package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SendMessageRequest(@NotBlank @Size(max = SendMessageRequest.MAX_CONTENT) String content) {

	static final int MAX_CONTENT = 8000;
}
