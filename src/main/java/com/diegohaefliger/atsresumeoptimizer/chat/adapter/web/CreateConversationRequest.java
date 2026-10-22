package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import jakarta.validation.constraints.Size;

record CreateConversationRequest(@Size(max = ConversationTitle.MAX_LENGTH) String title) {
}
