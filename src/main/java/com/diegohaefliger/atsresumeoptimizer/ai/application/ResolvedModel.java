package com.diegohaefliger.atsresumeoptimizer.ai.application;

import dev.langchain4j.model.chat.ChatModel;

record ResolvedModel(ChatModel chatModel, AiProvider provider, String modelName) {
}
