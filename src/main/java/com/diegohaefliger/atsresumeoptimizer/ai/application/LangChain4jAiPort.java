package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.BulletReview;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.JobFocus;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
class LangChain4jAiPort implements AiPort {

	private static final Logger LOGGER = LoggerFactory.getLogger(LangChain4jAiPort.class);
	private static final String JOB_STRUCTURING_KEY = "job-structuring";
	private static final String BULLET_REVIEW_KEY = "bullet-review";
	private static final String REQUIREMENT_EVIDENCE_KEY = "requirement-evidence";
	private static final String COVER_LETTER_KEY = "cover-letter";
	static final String RESUME_STRUCTURING_KEY = "resume-structuring";
	private static final int MAX_PARSE_ATTEMPTS = 2;
	private static final String NO_PROVIDER_CONFIGURED =
			"Nenhum provedor de IA configurado. Cadastre a chave em Configurações e coloque o provedor na fila.";
	private static final String NO_CORRECTIONS_PLACEHOLDER =
			"Nenhuma correção específica identificada além das regras gerais acima.";
	private static final String NO_JOB_FOCUS_PLACEHOLDER =
			"Nenhuma vaga informada: não reordene nada por aderência e devolva \"removedSkills\" vazio.";
	private static final String UNKNOWN_SENIORITY = "não informada";
	private static final String NO_MATCHED_KEYWORDS = "nenhuma";
	private static final String NO_EVIDENCED_REQUIREMENTS = "- nenhum";
	private static final String UNKNOWN_COMPANY = "não informada";
	private static final String UNKNOWN_JOB_TITLE = "não informado";

	private final AiModelGateway modelGateway;
	private final PromptTemplateRepository promptTemplateRepository;
	private final LlmCacheRepository llmCacheRepository;
	private final ObjectMapper objectMapper;

	LangChain4jAiPort(
			AiModelGateway modelGateway,
			PromptTemplateRepository promptTemplateRepository,
			LlmCacheRepository llmCacheRepository,
			ObjectMapper objectMapper) {
		this.modelGateway = modelGateway;
		this.promptTemplateRepository = promptTemplateRepository;
		this.llmCacheRepository = llmCacheRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public AiResult<JobStructured> structureJob(String jobText) {
		PromptTemplate template = activeTemplate(JOB_STRUCTURING_KEY);
		String prompt = template.getContent().replace("{{jobText}}", jobText);
		return call(template, prompt, JobStructured.class);
	}

	@Override
	public AiResult<List<BulletReview>> reviewBullets(List<String> bullets) {
		PromptTemplate template = activeTemplate(BULLET_REVIEW_KEY);
		String bulletList = bullets.stream().map(bullet -> "- " + bullet + "\n").collect(Collectors.joining());
		String prompt = template.getContent().replace("{{bullets}}", bulletList);
		AiResult<BulletReview[]> result = call(template, prompt, BulletReview[].class);
		return new AiResult<>(List.of(result.value()), result.usage());
	}

	@Override
	public AiResult<List<RequirementEvidence>> findRequirementEvidence(String resumeText, List<String> requirements) {
		PromptTemplate template = activeTemplate(REQUIREMENT_EVIDENCE_KEY);
		String requirementList = requirements.stream().map(requirement -> "- " + requirement).collect(Collectors.joining("\n"));
		String prompt = template.getContent()
				.replace("{{requirements}}", requirementList)
				.replace("{{resumeText}}", resumeText);
		AiResult<RequirementEvidence[]> result = call(template, prompt, RequirementEvidence[].class);
		return new AiResult<>(List.of(result.value()), result.usage());
	}

	@Override
	public AiResult<CoverLetterDraft> writeCoverLetter(CoverLetterBrief brief) {
		PromptTemplate template = activeTemplate(COVER_LETTER_KEY);
		String prompt = template.getContent()
				.replace("{{jobTitle}}", StringUtils.hasText(brief.jobTitle()) ? brief.jobTitle() : UNKNOWN_JOB_TITLE)
				.replace("{{company}}", StringUtils.hasText(brief.company()) ? brief.company() : UNKNOWN_COMPANY)
				.replace("{{priorityKeywords}}",
						brief.priorityKeywords().isEmpty() ? NO_MATCHED_KEYWORDS : String.join(", ", brief.priorityKeywords()))
				.replace("{{jobText}}", brief.jobText())
				.replace("{{resumeText}}", brief.resumeText());
		return call(template, prompt, CoverLetterDraft.class, false);
	}

	@Override
	public AiResult<StructuredResume> structureResume(String resumeText, String correctionInstructions, JobFocus jobFocus) {
		PromptTemplate template = activeTemplate(RESUME_STRUCTURING_KEY);
		String prompt = template.getContent()
				.replace("{{resumeText}}", resumeText)
				.replace("{{correctionInstructions}}",
						StringUtils.hasText(correctionInstructions) ? correctionInstructions : NO_CORRECTIONS_PLACEHOLDER)
				.replace("{{jobFocus}}", jobFocus.enabled() ? jobFocusBlock(jobFocus) : NO_JOB_FOCUS_PLACEHOLDER);
		return call(template, prompt, StructuredResume.class);
	}

	private String jobFocusBlock(JobFocus jobFocus) {
		String seniority = StringUtils.hasText(jobFocus.seniority()) ? jobFocus.seniority() : UNKNOWN_SENIORITY;
		String keywords = jobFocus.matchedKeywords().isEmpty()
				? NO_MATCHED_KEYWORDS
				: String.join(", ", jobFocus.matchedKeywords());
		String evidences = jobFocus.evidencedRequirements().isEmpty()
				? NO_EVIDENCED_REQUIREMENTS
				: jobFocus.evidencedRequirements().stream()
						.map(item -> "- \"%s\" — evidência no original: \"%s\"".formatted(item.requirement(), item.evidence()))
						.collect(Collectors.joining("\n"));
		return """
				Vaga: %s
				Senioridade: %s
				Competências da vaga que o candidato JÁ TEM no currículo original: %s
				Termos da vaga que o candidato tem com outro nome (use o termo da vaga junto da evidência):
				%s
				Principais competências da vaga que o candidato tem:
				%s"""
				.formatted(jobFocus.jobTitle(), seniority, keywords, evidences, prioritySkills(jobFocus));
	}

	private static String prioritySkills(JobFocus jobFocus) {
		if (jobFocus.prioritySkills().isEmpty()) {
			return NO_EVIDENCED_REQUIREMENTS;
		}
		return jobFocus.prioritySkills().stream()
				.map(skill -> skill.usedInExperience()
						? "- %s — JÁ USADA em cargo/projeto: destaque no bullet dele".formatted(skill.term())
						: "- %s — só consta em Competências: cite no resumo e na lista, nunca num cargo".formatted(skill.term()))
				.collect(Collectors.joining("\n"));
	}

	private <T> AiResult<T> call(PromptTemplate template, String prompt, Class<T> responseType) {
		return call(template, prompt, responseType, true);
	}

	private <T> AiResult<T> call(PromptTemplate template, String prompt, Class<T> responseType, boolean cacheable) {
		List<ResolvedModel> chain = modelGateway.resolveChain(template.getKey());
		if (chain.isEmpty()) {
			throw new AiCallException(NO_PROVIDER_CONFIGURED, null);
		}
		AiCallException lastFailure = null;
		for (ResolvedModel resolved : chain) {
			try {
				return callProvider(resolved, template, prompt, responseType, cacheable);
			} catch (AiCallException failure) {
				lastFailure = failure;
				LOGGER.warn("Provedor {} ({}) falhou para {}, tentando o próximo da fila", resolved.provider(),
						resolved.modelName(), template.getKey());
			}
		}
		throw lastFailure;
	}

	private <T> AiResult<T> callProvider(
			ResolvedModel resolved, PromptTemplate template, String prompt, Class<T> responseType, boolean cacheable) {
		String model = resolved.modelName();
		String keyHash = Sha256.hex(
				template.getKey() + ":" + template.getVersion() + ":" + resolved.provider() + ":" + model + ":" + prompt);
		Optional<LlmCacheEntry> cached = cacheable ? llmCacheRepository.findById(keyHash) : Optional.empty();
		if (cached.isPresent()) {
			return new AiResult<>(parse(cached.get().response(), responseType), AiUsage.cached(resolved.provider().label(), model));
		}

		AiCallException lastParseFailure = null;
		for (int attempt = 1; attempt <= MAX_PARSE_ATTEMPTS; attempt++) {
			ChatResponse response;
			try {
				response = resolved.chatModel().chat(ChatRequest.builder().messages(UserMessage.from(prompt)).build());
			} catch (RuntimeException exception) {
				LOGGER.warn("Falha ao chamar {} para {}", resolved.provider(), template.getKey(), exception);
				throw new AiCallException("Falha ao chamar " + resolved.provider().label() + " para " + template.getKey(), exception);
			}

			String rawText = stripMarkdownFences(response.aiMessage().text());
			String parsedText = rawText;
			T value;
			try {
				value = parse(rawText, responseType);
			} catch (AiCallException parseFailure) {
				try {
					parsedText = JsonBracketRepair.repair(rawText);
					value = parse(parsedText, responseType);
					LOGGER.info("JSON da IA pra {} tinha colchete/chave de fechamento trocado, corrigido automaticamente",
							template.getKey());
				} catch (AiCallException stillBroken) {
					lastParseFailure = stillBroken;
					LOGGER.warn("JSON inválido da IA pra {} na tentativa {}/{}, tentando de novo", template.getKey(), attempt,
							MAX_PARSE_ATTEMPTS);
					continue;
				}
			}

			if (cacheable) {
				llmCacheRepository.save(new LlmCacheEntry(keyHash, template.getKey(), template.getVersion(), parsedText));
			}

			TokenUsage tokenUsage = response.tokenUsage();
			int tokensIn = tokenUsage != null && tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0;
			int tokensOut = tokenUsage != null && tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount() : 0;
			AiUsage usage = new AiUsage(resolved.provider().label(), model, tokensIn, tokensOut,
					ModelPricing.costUsd(resolved.provider(), model, tokensIn, tokensOut), false);
			return new AiResult<>(value, usage);
		}
		throw lastParseFailure;
	}

	private <T> T parse(String json, Class<T> responseType) {
		try {
			return objectMapper.readValue(json, responseType);
		} catch (JacksonException exception) {
			throw new AiCallException("Resposta da IA não é o JSON esperado: " + json, exception);
		}
	}

	private PromptTemplate activeTemplate(String key) {
		return promptTemplateRepository.findFirstByKeyOrderByVersionDesc(key)
				.orElseThrow(() -> new AiCallException("Nenhum prompt_template ativo para a chave " + key, null));
	}

	private static String stripMarkdownFences(String text) {
		String trimmed = text.strip();
		if (trimmed.startsWith("```")) {
			trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\R", "").replaceFirst("```\\s*$", "");
		}
		return trimmed.strip();
	}
}
