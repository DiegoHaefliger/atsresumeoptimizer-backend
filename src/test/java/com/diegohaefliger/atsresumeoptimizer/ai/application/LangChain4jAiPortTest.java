package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.JobFocus;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.ai.RemovedSkill;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LangChain4jAiPortTest {

	@Mock
	private ChatModel chatModel;
	@Mock
	private PromptTemplateRepository promptTemplateRepository;
	@Mock
	private LlmCacheRepository llmCacheRepository;

	@Mock
	private AiModelGateway modelGateway;

	@org.junit.jupiter.api.BeforeEach
	void resolvesTheChatModel() {
		lenient().when(modelGateway.resolveChain(any()))
				.thenReturn(List.of(new ResolvedModel(chatModel, AiProvider.OPENAI, "gpt-4o-mini")));
	}

	private LangChain4jAiPort aiPort;

	@Test
	void explainsThatNoProviderIsConfiguredWhenTheQueueIsEmpty() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = mock(PromptTemplate.class);
		lenient().when(template.key()).thenReturn("job-structuring");
		lenient().when(template.version()).thenReturn(1);
		lenient().when(template.content()).thenReturn("Estruture: {{jobText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("job-structuring"))
				.thenReturn(Optional.of(template));
		lenient().when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		when(modelGateway.resolveChain("job-structuring")).thenReturn(java.util.List.of());

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> aiPort.structureJob("Vaga de backend Java"))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.ai.AiCallException.class)
				.hasMessageContaining("Nenhum provedor de IA configurado");
	}

	@Test
	void callsTheModelParsesJsonAndSavesToCacheOnMiss() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("job-structuring", "Estruture: {{jobText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("job-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String json = """
				{"title":"Backend Java","seniority":"pleno","minYearsExperience":3,"educationLevel":null,
				 "languages":["inglês"],"requiredKeywords":["Java","Spring"],"keywordEquivalents":{}}""";
		ChatResponse response = ChatResponse.builder()
				.aiMessage(AiMessage.from(json))
				.tokenUsage(new TokenUsage(100, 50))
				.build();
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(response);

		AiResult<JobStructured> result = aiPort.structureJob("Vaga de backend Java pleno");

		assertThat(result.value().title()).isEqualTo("Backend Java");
		assertThat(result.value().requiredKeywords()).containsExactly("Java", "Spring");
		assertThat(result.usage().tokensIn()).isEqualTo(100);
		assertThat(result.usage().tokensOut()).isEqualTo(50);
		assertThat(result.usage().cached()).isFalse();
		verify(llmCacheRepository).save(any());
	}

	@Test
	void reusesCachedResponseWithoutCallingTheModel() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("job-structuring", "Estruture: {{jobText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("job-structuring"))
				.thenReturn(Optional.of(template));
		String cachedJson = """
				{"title":"Backend Java","seniority":null,"minYearsExperience":null,"educationLevel":null,
				 "languages":[],"requiredKeywords":[],"keywordEquivalents":{}}""";
		LlmCacheEntry cachedEntry = new LlmCacheEntry("any-hash", "job-structuring", 1, cachedJson);
		when(llmCacheRepository.findById(any())).thenReturn(Optional.of(cachedEntry));

		AiResult<JobStructured> result = aiPort.structureJob("Vaga de backend Java pleno");

		assertThat(result.value().title()).isEqualTo("Backend Java");
		assertThat(result.usage().cached()).isTrue();
		assertThat(result.usage().tokensIn()).isZero();
		verifyNoInteractions(chatModel);
	}

	@Test
	void structuresResumeIntoRichSections() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("resume-structuring", "Estruture: {{resumeText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String json = """
				{"name":"Ana Silva","headline":"Backend Java",
				 "sections":[{"title":"RESUMO","semanticType":"SUMMARY","kind":"PARAGRAPH","paragraph":"Desenvolvedora backend.",
				 "keyValues":null,"richLines":null,"entries":null}]}""";
		ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(json)).tokenUsage(new TokenUsage(80, 40)).build();
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(response);

		AiResult<StructuredResume> result = aiPort.structureResume("Ana Silva\nRESUMO\nDesenvolvedora backend.", "", JobFocus.NONE);

		assertThat(result.value().name()).isEqualTo("Ana Silva");
		assertThat(result.value().sections()).hasSize(1);
		assertThat(result.value().sections().get(0).paragraph()).isEqualTo("Desenvolvedora backend.");
		assertThat(result.value().sections().get(0).semanticType()).isEqualTo(ResumeSectionSemanticType.SUMMARY);
	}

	@Test
	void sendsTheCorrectionInstructionsInThePromptWhenGiven() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template =
				template("resume-structuring", "Estruture: {{resumeText}} | Correções: {{correctionInstructions}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String json = """
				{"name":"Ana","headline":null,"sections":[]}""";
		ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(json)).tokenUsage(new TokenUsage(10, 5)).build();
		org.mockito.ArgumentCaptor<ChatRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
		when(chatModel.chat(requestCaptor.capture())).thenReturn(response);

		aiPort.structureResume("texto", "- Título de seção \"X\" não é padrão. Sugestão: use \"Experiência Profissional\".", JobFocus.NONE);

		String sentPrompt = ((dev.langchain4j.data.message.UserMessage) requestCaptor.getValue().messages().get(0)).singleText();
		assertThat(sentPrompt).contains("Título de seção \"X\" não é padrão");
	}

	@Test
	void sendsOnlyTheJobKeywordsTheResumeAlreadyHasWhenJobFocusIsEnabled() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("resume-structuring", "Estruture: {{resumeText}} | Vaga: {{jobFocus}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String json = """
				{"name":"Ana","headline":null,"sections":[],
				 "removedSkills":[{"skill":"Preparar café","reason":"Sem relação com desenvolvimento"}]}""";
		ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(json)).tokenUsage(new TokenUsage(10, 5)).build();
		org.mockito.ArgumentCaptor<ChatRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
		when(chatModel.chat(requestCaptor.capture())).thenReturn(response);

		AiResult<StructuredResume> result =
				aiPort.structureResume("texto", "", new JobFocus("Desenvolvedora Backend", "Pleno", List.of("Java", "Docker")));

		String sentPrompt = ((dev.langchain4j.data.message.UserMessage) requestCaptor.getValue().messages().get(0)).singleText();
		assertThat(sentPrompt).contains("Vaga: Desenvolvedora Backend", "Senioridade: Pleno", "JÁ TEM no currículo original: Java, Docker");
		assertThat(result.value().removedSkills()).containsExactly(
				new RemovedSkill("Preparar café", "Sem relação com desenvolvimento"));
	}

	@Test
	void tellsTheModelNotToReorderWhenJobFocusIsDisabled() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("resume-structuring", "Estruture: {{resumeText}} | Vaga: {{jobFocus}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		ChatResponse response = ChatResponse.builder()
				.aiMessage(AiMessage.from("{\"name\":\"Ana\",\"headline\":null,\"sections\":[]}"))
				.tokenUsage(new TokenUsage(10, 5)).build();
		org.mockito.ArgumentCaptor<ChatRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
		when(chatModel.chat(requestCaptor.capture())).thenReturn(response);

		AiResult<StructuredResume> result = aiPort.structureResume("texto", "", JobFocus.NONE);

		String sentPrompt = ((dev.langchain4j.data.message.UserMessage) requestCaptor.getValue().messages().get(0)).singleText();
		assertThat(sentPrompt).contains("Nenhuma vaga informada");
		assertThat(result.value().removedSkills()).isEmpty();
	}

	@Test
	void fallsBackToADefaultMessageWhenThereAreNoCorrectionInstructions() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template =
				template("resume-structuring", "Estruture: {{resumeText}} | Correções: {{correctionInstructions}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String json = """
				{"name":"Ana","headline":null,"sections":[]}""";
		ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(json)).tokenUsage(new TokenUsage(10, 5)).build();
		org.mockito.ArgumentCaptor<ChatRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(ChatRequest.class);
		when(chatModel.chat(requestCaptor.capture())).thenReturn(response);

		aiPort.structureResume("texto", "", JobFocus.NONE);

		String sentPrompt = ((dev.langchain4j.data.message.UserMessage) requestCaptor.getValue().messages().get(0)).singleText();
		assertThat(sentPrompt).contains("Nenhuma correção específica identificada");
	}

	@Test
	void repairsAWrongClosingBracketWithoutSpendingASecondAiCall() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("resume-structuring", "Estruture: {{resumeText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String brokenJson = "{\"name\":\"Ana\",\"headline\":null,"
				+ "\"sections\":[{\"title\":\"FORMACAO\",\"semanticType\":\"EDUCATION\",\"kind\":\"RICH_LINES\","
				+ "\"richLines\":[[{\"text\":\"Curso\",\"bold\":true}]],\"paragraph\":null,\"keyValues\":null,\"entries\":null}}}";
		ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(brokenJson)).tokenUsage(new TokenUsage(10, 5)).build();
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(response);

		AiResult<StructuredResume> result = aiPort.structureResume("texto", "", JobFocus.NONE);

		assertThat(result.value().name()).isEqualTo("Ana");
		assertThat(result.value().sections().get(0).richLines()).hasSize(1);
		verify(chatModel, org.mockito.Mockito.times(1)).chat(any(ChatRequest.class));
		verify(llmCacheRepository).save(any());
	}

	@Test
	void retriesOnceWhenTheModelReturnsInvalidJsonThenSucceeds() {
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("resume-structuring", "Estruture: {{resumeText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		String brokenJson = "{\"name\":\"Ana\",\"sections\":[}";
		String fixedJson = """
				{"name":"Ana","headline":null,"sections":[]}""";
		ChatResponse brokenResponse = ChatResponse.builder().aiMessage(AiMessage.from(brokenJson)).tokenUsage(new TokenUsage(10, 5)).build();
		ChatResponse fixedResponse = ChatResponse.builder().aiMessage(AiMessage.from(fixedJson)).tokenUsage(new TokenUsage(10, 5)).build();
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(brokenResponse).thenReturn(fixedResponse);

		AiResult<StructuredResume> result = aiPort.structureResume("texto", "", JobFocus.NONE);

		assertThat(result.value().name()).isEqualTo("Ana");
		verify(llmCacheRepository).save(any());
	}

	private PromptTemplate template(String key, String content) {
		PromptTemplate template = mock(PromptTemplate.class);
		when(template.key()).thenReturn(key);
		when(template.version()).thenReturn(1);
		when(template.content()).thenReturn(content);
		return template;
	}

	@Test
	void fallsBackToTheNextProviderWhenTheFirstOneFails() {
		ChatModel backup = mock(ChatModel.class);
		when(modelGateway.resolveChain(any())).thenReturn(List.of(
				new ResolvedModel(chatModel, AiProvider.OPENAI, "gpt-4o-mini"),
				new ResolvedModel(backup, AiProvider.ANTHROPIC, "claude-sonnet-5-5")));
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("job-structuring", "Estruture: {{jobText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("job-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		when(chatModel.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("429 rate limit"));
		when(backup.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
				.aiMessage(AiMessage.from("""
						{"title":"Backend Java","requiredKeywords":["Java"],"keywordEquivalents":{}}"""))
				.tokenUsage(new TokenUsage(10, 5))
				.build());

		AiResult<JobStructured> result = aiPort.structureJob("vaga");

		assertThat(result.value().title()).isEqualTo("Backend Java");
		assertThat(result.usage().model()).isEqualTo("claude-sonnet-5-5");
	}

	@Test
	void failsOnlyWhenEveryProviderInTheChainFails() {
		ChatModel backup = mock(ChatModel.class);
		when(modelGateway.resolveChain(any())).thenReturn(List.of(
				new ResolvedModel(chatModel, AiProvider.OPENAI, "gpt-4o-mini"),
				new ResolvedModel(backup, AiProvider.GEMINI, "gemini-2.5-flash")));
		aiPort = new LangChain4jAiPort(modelGateway, promptTemplateRepository, llmCacheRepository, new ObjectMapper());
		PromptTemplate template = template("job-structuring", "Estruture: {{jobText}}");
		when(promptTemplateRepository.findFirstByKeyAndActiveTrueOrderByVersionDesc("job-structuring"))
				.thenReturn(Optional.of(template));
		when(llmCacheRepository.findById(any())).thenReturn(Optional.empty());
		when(chatModel.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("timeout"));
		when(backup.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("403"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> aiPort.structureJob("vaga"))
				.isInstanceOf(com.diegohaefliger.atsresumeoptimizer.ai.AiCallException.class)
				.hasMessageContaining("Google Gemini");
	}
}
