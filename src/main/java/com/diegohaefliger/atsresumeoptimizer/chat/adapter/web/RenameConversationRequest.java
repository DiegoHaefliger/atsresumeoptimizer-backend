package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RenameConversationRequest(@NotBlank @Size(max = ConversationTitle.MAX_LENGTH) String title) {
}
