package com.diegohaefliger.atsresumeoptimizer.job.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.JobConditions;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringResult;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.job.domain.DuplicateJobPostingException;
import com.diegohaefliger.atsresumeoptimizer.job.domain.JobPostingNotFoundException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
class JobStructuringServiceImpl implements JobStructuringService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobStructuringServiceImpl.class);

	private final AiPort aiPort;
	private final JobPostingRepository repository;
	private final ObjectMapper objectMapper;

	JobStructuringServiceImpl(AiPort aiPort, JobPostingRepository repository, ObjectMapper objectMapper) {
		this.aiPort = aiPort;
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Override
	public JobStructuringResult structureFromText(String jobText) {
		return structure(jobText, JobDetails.NONE, false);
	}

	@Override
	public JobStructuringResult structureFromText(String jobText, JobDetails details) {
		return structure(jobText, details, false);
	}

	@Override
	public JobStructuringResult structureFromTargetRole(String targetRole) {
		String syntheticJobText = """
				Nenhuma vaga real foi colada. O candidato só informou o cargo-alvo abaixo.
				Gere um perfil típico de vaga para esse cargo, com requisitos e palavras-chave
				comuns nesse tipo de posição no mercado brasileiro.

				Cargo-alvo: %s""".formatted(targetRole);
		return structure(syntheticJobText, JobDetails.NONE, true);
	}

	@Override
	public UUID register(String jobText, JobDetails details) {
		String textHash = Sha256.hex(jobText);
		JobPosting posting = repository.findByTextHash(textHash)
				.orElseGet(() -> JobPosting.unstructured(jobText, textHash));
		posting.applyDetails(details);
		posting.showInRecent();
		repository.saveAndFlush(posting);
		return posting.id();
	}

	@Override
	@Transactional
	public void hideFromRecent(UUID jobPostingId) {
		JobPosting posting = find(jobPostingId);
		posting.hideFromRecent();
		repository.save(posting);
	}

	@Override
	public JobRegistration registerAndStructure(String jobText, JobDetails details) {
		return structureRegistered(register(jobText, details), jobText, details);
	}

	private JobRegistration structureRegistered(UUID id, String jobText, JobDetails details) {
		try {
			JobStructured structured = structureFromText(jobText, details).structured();
			return new JobRegistration(id, structured.title(), structured.seniority(), structured.requiredKeywords());
		} catch (AiCallException exception) {
			LOGGER.warn("Vaga {} cadastrada sem estruturação: IA indisponível", id, exception);
			return new JobRegistration(id, null, null, List.of());
		}
	}

	@Override
	public JobRegistration update(UUID jobPostingId, String jobText, JobDetails details) {
		JobPosting posting = find(jobPostingId);
		String textHash = Sha256.hex(jobText);
		if (!textHash.equals(posting.textHash())) {
			repository.findByTextHash(textHash)
					.filter(other -> !other.id().equals(jobPostingId))
					.ifPresent(other -> {
						throw new DuplicateJobPostingException();
					});
			posting.replaceText(jobText, textHash);
		}
		posting.replaceDetails(details);
		repository.saveAndFlush(posting);
		return structureRegistered(posting.id(), jobText, details);
	}

	@Override
	@Transactional(readOnly = true)
	public List<JobListing> listJobs() {
		return repository.findTop100BySyntheticFalseAndHiddenAtIsNullOrderByCreatedAtDesc().stream()
				.map(posting -> new JobListing(toOffer(posting), posting.createdAt()))
				.toList();
	}

	private JobStructuringResult structure(String jobText, JobDetails details, boolean synthetic) {
		String textHash = Sha256.hex(jobText);
		Optional<JobPosting> existing = repository.findByTextHash(textHash);
		AiResult<JobStructured> aiResult;
		try {
			aiResult = aiPort.structureJob(jobText);
		} catch (AiCallException exception) {
			return existing.filter(JobPosting::isStructured)
					.map(posting -> storedResult(posting, details))
					.orElseThrow(() -> exception);
		}
		JobPosting posting = existing.orElseGet(
				() -> JobPosting.unstructured(jobText, textHash));
		posting.structure(aiResult.value().title(), serialize(aiResult.value()));
		if (synthetic) {
			posting.markSynthetic();
		}
		posting.applyDetails(details);
		try {
			repository.saveAndFlush(posting);
		} catch (DataIntegrityViolationException exception) {
			JobPosting concurrent = repository.findByTextHash(textHash).orElseThrow(() -> exception);
			return new JobStructuringResult(concurrent.id(), withUserDetails(concurrent, aiResult.value()), aiResult.usage());
		}
		return new JobStructuringResult(posting.id(), withUserDetails(posting, aiResult.value()), aiResult.usage());
	}

	private JobStructuringResult storedResult(JobPosting posting, JobDetails details) {
		if (!JobDetails.NONE.equals(details)) {
			posting.applyDetails(details);
			repository.save(posting);
		}
		return new JobStructuringResult(posting.id(), withUserDetails(posting, parse(posting.structured())),
				AiUsage.cached(null, null));
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> keywords(UUID jobPostingId) {
		JobPosting posting = find(jobPostingId);
		return posting.customKeywords()
				.orElseGet(() -> posting.isStructured() ? parse(posting.structured()).requiredKeywords() : List.of());
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> selectedKeywords(UUID jobPostingId) {
		JobPosting posting = find(jobPostingId);
		return posting.selectedKeywords()
				.orElseGet(() -> keywords(jobPostingId).stream().limit(JobStructured.DEFAULT_PRIORITY_COUNT).toList());
	}

	@Override
	@Transactional
	public void replaceKeywords(UUID jobPostingId, List<String> keywords, List<String> selected) {
		JobPosting posting = find(jobPostingId);
		List<String> terms = distinctTerms(keywords);
		Set<String> chosen = distinctTerms(selected).stream().map(term -> term.toLowerCase(Locale.ROOT))
				.collect(Collectors.toSet());
		posting.replaceKeywords(terms,
				terms.stream().filter(term -> chosen.contains(term.toLowerCase(Locale.ROOT))).toList());
		repository.save(posting);
	}

	private static List<String> distinctTerms(List<String> keywords) {
		Map<String, String> byLowerCase = new LinkedHashMap<>();
		keywords.stream().map(String::strip).filter(term -> !term.isEmpty())
				.forEach(term -> byLowerCase.putIfAbsent(term.toLowerCase(Locale.ROOT), term));
		return List.copyOf(byLowerCase.values());
	}

	@Override
	public String getRawText(UUID jobPostingId) {
		return find(jobPostingId).rawText();
	}

	@Override
	public Optional<JobOffer> offer(UUID jobPostingId) {
		return repository.findById(jobPostingId).map(this::toOffer);
	}

	@Override
	public Map<UUID, JobOffer> offers(Collection<UUID> jobPostingIds) {
		return repository.findAllById(jobPostingIds).stream()
				.map(this::toOffer)
				.collect(Collectors.toUnmodifiableMap(JobOffer::jobPostingId, Function.identity()));
	}

	private JobOffer toOffer(JobPosting posting) {
		JobStructured structured = posting.isStructured() ? parse(posting.structured()) : null;
		JobConditions conditions = structured != null ? structured.conditions() : JobConditions.NONE;
		WorkModel workModel = posting.workModel() != null
				? posting.workModel()
				: WorkModel.fromCode(conditions.workModel()).orElse(null);
		String company = posting.company() != null ? posting.company() : conditions.company();
		BigDecimal salaryMin = posting.salary() != null ? posting.salary() : conditions.salaryMin();
		BigDecimal salaryMax = posting.salary() != null ? posting.salary() : conditions.salaryMax();
		List<String> benefits = posting.benefits().isEmpty() ? conditions.benefits() : posting.benefits();
		String title = posting.customTitle() != null ? posting.customTitle() : posting.title();
		String seniority = posting.seniority() != null
				? posting.seniority()
				: structured != null ? structured.seniority() : null;
		ContractType contractType = posting.contractType() != null
				? posting.contractType()
				: ContractType.fromCode(conditions.contractType()).orElse(null);
		return new JobOffer(posting.id(), posting.code(), title, company, posting.sourceUrl(), posting.interviewUrl(), workModel,
				contractType, salaryMin, salaryMax, benefits,
				conditions.location(),
				seniority, posting.rawText());
	}

	private static JobStructured withUserDetails(JobPosting posting, JobStructured structured) {
		String title = posting.customTitle() != null ? posting.customTitle() : structured.title();
		String seniority = posting.seniority() != null ? posting.seniority() : structured.seniority();
		List<String> keywords = posting.customKeywords().orElse(structured.requiredKeywords());
		return new JobStructured(title, seniority, structured.minYearsExperience(), structured.educationLevel(),
				structured.languages(), keywords, structured.keywordEquivalents(), structured.conditions(),
				posting.selectedKeywords().orElse(null));
	}

	private JobStructured parse(String json) {
		try {
			return objectMapper.readValue(json, JobStructured.class);
		} catch (JacksonException exception) {
			throw new IllegalStateException("job_posting.structured corrompido", exception);
		}
	}

	private String serialize(JobStructured structured) {
		return objectMapper.writeValueAsString(structured);
	}

	private JobPosting find(UUID jobPostingId) {
		return repository.findById(jobPostingId).orElseThrow(() -> new JobPostingNotFoundException(jobPostingId));
	}
}
