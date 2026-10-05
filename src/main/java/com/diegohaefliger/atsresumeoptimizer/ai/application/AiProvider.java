package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;

public enum AiProvider {
	OPENAI("OpenAI", true, false, null, "gpt-4o-mini", "gpt-4o", List.of(
			"gpt-5", "gpt-5-mini", "gpt-5-nano", "gpt-4.1", "gpt-4.1-mini", "gpt-4.1-nano", "gpt-4o", "gpt-4o-mini",
			"o3", "o3-mini", "o4-mini")),
	ANTHROPIC("Anthropic (Claude)", true, false, null, "claude-opus-5-5", "claude-opus-5-5", List.of(
			"claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-opus-4-8", "claude-opus-4-7",
			"claude-opus-4-6", "claude-sonnet-5-5", "claude-sonnet-5", "claude-sonnet-4-6", "claude-haiku-4-5")),
	GEMINI("Google Gemini", true, false, null, "gemini-2.5-flash", "gemini-2.5-pro", List.of(
			"gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-2.0-flash-lite")),
	OLLAMA("Ollama (local)", false, true, "http://localhost:11434", "llama3.1:8b", "qwen2.5:14b", List.of(
			"llama3.1:8b", "llama3.1:70b", "llama3.2:3b", "llama3.3:70b", "qwen2.5:7b", "qwen2.5:14b", "qwen2.5:32b",
			"mistral:7b", "gemma2:9b", "deepseek-r1:8b", "phi3:14b")),
	OPENAI_COMPATIBLE("Compatível com OpenAI (Groq, OpenRouter, DeepSeek...)", true, true, null, null, null, List.of());

	private final String label;
	private final boolean requiresApiKey;
	private final boolean requiresBaseUrl;
	private final String defaultBaseUrl;
	private final String defaultModel;
	private final String defaultHeavyModel;
	private final List<String> suggestedModels;

	AiProvider(String label, boolean requiresApiKey, boolean requiresBaseUrl, String defaultBaseUrl, String defaultModel,
			String defaultHeavyModel, List<String> suggestedModels) {
		this.label = label;
		this.requiresApiKey = requiresApiKey;
		this.requiresBaseUrl = requiresBaseUrl;
		this.defaultBaseUrl = defaultBaseUrl;
		this.defaultModel = defaultModel;
		this.defaultHeavyModel = defaultHeavyModel;
		this.suggestedModels = suggestedModels;
	}

	public String label() {
		return label;
	}

	public boolean requiresApiKey() {
		return requiresApiKey;
	}

	public boolean requiresBaseUrl() {
		return requiresBaseUrl;
	}

	public String defaultBaseUrl() {
		return defaultBaseUrl;
	}

	public String defaultModel() {
		return defaultModel;
	}

	public String defaultHeavyModel() {
		return defaultHeavyModel;
	}

	public List<String> suggestedModels() {
		return suggestedModels;
	}
}
