package com.diegohaefliger.atsresumeoptimizer.ai.application;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class AiModelGateway {

	private final AiRuntimeSettingsProvider settingsProvider;
	private final ChatModelFactory factory;
	private final AiProperties properties;
	private final Map<ModelKey, ChatModel> models = new ConcurrentHashMap<>();
	private final Map<ModelKey, StreamingChatModel> streamingModels = new ConcurrentHashMap<>();

	AiModelGateway(AiRuntimeSettingsProvider settingsProvider, ChatModelFactory factory, AiProperties properties) {
		this.settingsProvider = settingsProvider;
		this.factory = factory;
		this.properties = properties;
	}

	List<ResolvedModel> resolveChain(String task) {
		return settingsProvider.chain().stream().map(settings -> resolve(settings, task)).toList();
	}

	List<ResolvedStreamingModel> resolveStreamingChain(String task) {
		return settingsProvider.chain().stream().map(settings -> resolveStreaming(settings, task)).toList();
	}

	private ResolvedStreamingModel resolveStreaming(AiRuntimeSettings settings, String task) {
		String modelName = settings.modelFor(false);
		int maxOutputTokens = properties.maxOutputTokensFor(task, settings.maxOutputTokens());
		StreamingChatModel chatModel = streamingModels.computeIfAbsent(new ModelKey(settings, modelName, maxOutputTokens),
				key -> factory.createStreaming(key.settings(), key.modelName(), key.maxOutputTokens()));
		return new ResolvedStreamingModel(chatModel, settings.provider(), modelName);
	}

	private ResolvedModel resolve(AiRuntimeSettings settings, String task) {
		String modelName = settings.modelFor(LangChain4jAiPort.RESUME_STRUCTURING_KEY.equals(task));
		int maxOutputTokens = properties.maxOutputTokensFor(task, settings.maxOutputTokens());
		ChatModel chatModel = models.computeIfAbsent(new ModelKey(settings, modelName, maxOutputTokens),
				key -> factory.create(key.settings(), key.modelName(), key.maxOutputTokens()));
		return new ResolvedModel(chatModel, settings.provider(), modelName);
	}

	void evictAll() {
		models.clear();
		streamingModels.clear();
		settingsProvider.refresh();
	}

	private record ModelKey(AiRuntimeSettings settings, String modelName, int maxOutputTokens) {
	}
}
