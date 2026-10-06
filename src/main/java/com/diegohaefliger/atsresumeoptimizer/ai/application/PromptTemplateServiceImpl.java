package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.InvalidPromptTemplateException;
import com.diegohaefliger.atsresumeoptimizer.ai.domain.PromptTemplateNotFoundException;
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class PromptTemplateServiceImpl implements PromptTemplateService {

	private static final Pattern KEY_FORMAT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{[A-Za-z]+}}");
	private static final int MAX_KEY_LENGTH = 100;
	private static final int FIRST_VERSION = 1;

	private final PromptTemplateRepository repository;
	private final PromptTemplateMapper mapper;

	PromptTemplateServiceImpl(PromptTemplateRepository repository, PromptTemplateMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromptTemplateSummary> listLatest() {
		return mapper.toSummaries(repository.findLatestOfEachKey());
	}

	@Override
	@Transactional(readOnly = true)
	public PromptTemplateView current(String key) {
		return mapper.toView(latest(key));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromptTemplateSummary> history(String key) {
		List<PromptTemplate> versions = repository.findByKeyOrderByVersionDesc(key);
		if (versions.isEmpty()) {
			throw notFound(key);
		}
		return mapper.toSummaries(versions);
	}

	@Override
	@Transactional(readOnly = true)
	public PromptTemplateView version(String key, int version) {
		return repository.findByKeyAndVersion(key, version)
				.map(mapper::toView)
				.orElseThrow(() -> new PromptTemplateNotFoundException(
						"Instrução '%s' não tem a versão %d.".formatted(key, version)));
	}

	@Override
	@Transactional
	public PromptTemplateView publish(String key, PromptTemplateUpdate update) {
		validateKey(key);
		if (!StringUtils.hasText(update.content())) {
			throw new InvalidPromptTemplateException("O texto da instrução não pode ficar vazio.");
		}
		return repository.findFirstByKeyOrderByVersionDesc(key)
				.map(latest -> publishAfter(latest, update.content()))
				.orElseGet(() -> mapper.toView(save(key, FIRST_VERSION, update.content(), null)));
	}

	private PromptTemplateView publishAfter(PromptTemplate latest, String content) {
		if (latest.getContent().equals(content)) {
			return mapper.toView(latest);
		}
		Set<String> missing = placeholders(latest.getContent());
		missing.removeAll(placeholders(content));
		if (!missing.isEmpty()) {
			throw new InvalidPromptTemplateException(
					"A instrução precisa manter os marcadores usados pelo sistema: " + String.join(", ", missing));
		}
		return mapper.toView(save(latest.getKey(), latest.getVersion() + 1, content, latest.getModel()));
	}

	private PromptTemplate save(String key, int version, String content, String model) {
		return repository.save(new PromptTemplate(UuidCreator.getTimeOrderedEpoch(), key, version, content, model));
	}

	private PromptTemplate latest(String key) {
		return repository.findFirstByKeyOrderByVersionDesc(key).orElseThrow(() -> notFound(key));
	}

	private static PromptTemplateNotFoundException notFound(String key) {
		return new PromptTemplateNotFoundException("Instrução '%s' não encontrada.".formatted(key));
	}

	private static void validateKey(String key) {
		if (key.length() > MAX_KEY_LENGTH || !KEY_FORMAT.matcher(key).matches()) {
			throw new InvalidPromptTemplateException(
					"O identificador da instrução usa só letras minúsculas, números e hífen.");
		}
	}

	private static Set<String> placeholders(String content) {
		Set<String> found = new LinkedHashSet<>();
		Matcher matcher = PLACEHOLDER.matcher(content);
		while (matcher.find()) {
			found.add(matcher.group());
		}
		return found;
	}
}
