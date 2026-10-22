package com.diegohaefliger.atsresumeoptimizer.ai.application;

import dev.langchain4j.model.chat.StreamingChatModel;

record ResolvedStreamingModel(StreamingChatModel chatModel, AiProvider provider, String modelName) {
}
