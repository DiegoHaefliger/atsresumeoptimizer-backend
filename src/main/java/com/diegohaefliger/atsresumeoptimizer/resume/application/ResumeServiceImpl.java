package com.diegohaefliger.atsresumeoptimizer.resume.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeUploadLimits;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.FavoriteResumeMustBeBaseException;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.ResumeVersionNotFoundException;
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ResumeServiceImpl implements ResumeService {

	private static final String TITLE_SEPARATOR = " · ";
	private static final int MAX_TITLE_LENGTH = 255;

	private final ResumeRepository resumeRepository;
	private final ResumeVersionRepository resumeVersionRepository;
	private final ResumeAnalysisPipeline parsingPipeline;

	ResumeServiceImpl(
			ResumeRepository resumeRepository,
			ResumeVersionRepository resumeVersionRepository,
			ResumeAnalysisPipeline parsingPipeline) {
		this.resumeRepository = resumeRepository;
		this.resumeVersionRepository = resumeVersionRepository;
		this.parsingPipeline = parsingPipeline;
	}

	@Override
	@Transactional
	public ResumeVersionCreated storeUpload(String title, byte[] content, String fileName, String mimeType, String text) {
		Resume resume = resumeRepository.save(Resume.base(UuidCreator.getTimeOrderedEpoch(), title));
		return storeGenerated(resume.id(), content, fileName, mimeType, text);
	}

	@Override
	@Transactional
	public ResumeVersionCreated storeAdapted(UUID sourceVersionId, UUID analysisId, String titleSuffix, byte[] content,
			String fileName, String mimeType, String rawText) {
		Resume source = resumeRepository.findById(version(sourceVersionId).resumeId())
				.orElseThrow(() -> new ResumeVersionNotFoundException(sourceVersionId));
		String title = source.title() + TITLE_SEPARATOR + titleSuffix;
		title = title.substring(0, Math.min(title.length(), MAX_TITLE_LENGTH));
		Resume resume = resumeRepository.save(Resume.adapted(UuidCreator.getTimeOrderedEpoch(), title, analysisId));
		return storeGenerated(resume.id(), content, fileName, mimeType, rawText);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UUID> latestAdaptedVersion(UUID analysisId) {
		return resumeRepository
				.findFirstBySourceAnalysisIdAndTitleNotOrderByCreatedAtDesc(analysisId, Resume.SCRUBBED_PLACEHOLDER)
				.flatMap(resume -> resumeVersionRepository.findByResumeIdOrderByCreatedAtAsc(resume.id()).reversed().stream()
						.filter(version -> !version.isScrubbed())
						.map(ResumeVersion::id)
						.findFirst());
	}

	private ResumeVersionCreated storeGenerated(UUID resumeId, byte[] content, String fileName, String mimeType,
			String text) {
		ParsingResult parsing = parsingPipeline.analyze(content);
		NormalizedDocument document =
				new NormalizedDocument(parsing.document().sourceFormat(), text, text, parsing.document().pageCount());
		return storeVersion(resumeId, content, fileName, mimeType, document, parsing.signals());
	}

	private ResumeVersionCreated storeVersion(UUID resumeId, byte[] content, String fileName, String mimeType,
			NormalizedDocument document, ParsingSignals signals) {
		ResumeVersion version = resumeVersionRepository.save(new ResumeVersion(
				UuidCreator.getTimeOrderedEpoch(), resumeId, fileName, mimeType, content.length, Sha256.hex(content),
				document, signals));
		return new ResumeVersionCreated(resumeId, version.id(), version.sizeBytes(), version.sha256(), version.pageCount());
	}

	@Override
	@Transactional
	public ResumeVersionCreated upload(byte[] content, String fileName, String mimeType, String title) {
		ResumeUploadLimits.validateSize(content.length);
		ParsingResult parsingResult = parsingPipeline.analyze(content);
		ResumeUploadLimits.validatePageCount(parsingResult.document().pageCount());
		Resume resume = resumeRepository.save(Resume.base(UuidCreator.getTimeOrderedEpoch(), resolveTitle(title, fileName)));
		return storeVersion(resume.id(), content, fileName, mimeType, parsingResult.document(), parsingResult.signals());
	}

	private static String resolveTitle(String title, String fileName) {
		String chosen = title == null || title.isBlank() ? fileName : title.trim();
		return chosen.substring(0, Math.min(chosen.length(), MAX_TITLE_LENGTH));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResumeVersionSummary> listVersions(UUID resumeId) {
		if (!resumeRepository.existsById(resumeId)) {
			throw new ResumeNotFoundException(resumeId);
		}
		return liveSummariesNewestFirst(resumeVersionRepository.findByResumeIdOrderByCreatedAtAsc(resumeId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResumeSummary> listResumes() {
		List<Resume> listed = new ArrayList<>(resumeRepository.findByOriginAndTitleNot(ResumeOrigin.BASE, Resume.SCRUBBED_PLACEHOLDER));
		listed.addAll(resumeRepository.findTop50ByOriginAndTitleNotOrderByCreatedAtDesc(ResumeOrigin.ADAPTED, Resume.SCRUBBED_PLACEHOLDER));
		listed.sort(Comparator.comparing(Resume::createdAt).reversed());
		return summarize(listed).stream()
				.sorted(Comparator.comparing(ResumeSummary::favorite).reversed())
				.toList();
	}

	@Override
	@Transactional
	public void updateAtsScore(UUID resumeId, Integer atsScore) {
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));
		resume.updateAtsScore(atsScore);
		resumeRepository.save(resume);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResumeSummary> listAdaptedFromAnalyses(Collection<UUID> analysisIds) {
		if (analysisIds.isEmpty()) {
			return List.of();
		}
		return summarize(resumeRepository.findByOriginAndSourceAnalysisIdInAndTitleNotOrderByCreatedAtDesc(
				ResumeOrigin.ADAPTED, analysisIds, Resume.SCRUBBED_PLACEHOLDER));
	}

	private List<ResumeSummary> summarize(List<Resume> resumes) {
		Map<UUID, List<ResumeVersionSummary>> versionsByResume = resumeVersionRepository
				.findByResumeIdInOrderByCreatedAtAsc(resumes.stream().map(Resume::id).toList())
				.stream()
				.collect(Collectors.groupingBy(ResumeVersion::resumeId,
						Collectors.collectingAndThen(Collectors.toList(), ResumeServiceImpl::liveSummariesNewestFirst)));
		return resumes.stream()
				.filter(resume -> !versionsByResume.getOrDefault(resume.id(), List.of()).isEmpty())
				.map(resume -> new ResumeSummary(resume.id(), resume.title(), resume.createdAt(), resume.origin(),
						resume.sourceAnalysisId(), resume.favorite(), resume.atsScore(), versionsByResume.get(resume.id())))
				.toList();
	}

	private static List<ResumeVersionSummary> liveSummariesNewestFirst(List<ResumeVersion> oldestFirst) {
		List<ResumeVersionSummary> summaries = new ArrayList<>();
		for (int index = 0; index < oldestFirst.size(); index++) {
			ResumeVersion version = oldestFirst.get(index);
			if (!version.isScrubbed()) {
				summaries.add(summary(version, index + 1));
			}
		}
		return summaries.reversed();
	}

	private static ResumeVersionSummary summary(ResumeVersion version, int number) {
		return new ResumeVersionSummary(version.id(), number, version.fileName(), version.mimeType(), version.sizeBytes(),
				version.pageCount(), version.createdAt());
	}

	@Override
	@Transactional(readOnly = true)
	public ParsingResult parsedVersion(UUID resumeVersionId) {
		ResumeVersion version = version(resumeVersionId);
		if (version.isScrubbed()) {
			throw new ResumeVersionNotFoundException(resumeVersionId);
		}
		return parsingPipeline.assemble(version.document(), version.signals());
	}

	@Override
	public ResumeVersionText getVersionText(UUID resumeVersionId) {
		ResumeVersion version = version(resumeVersionId);
		return new ResumeVersionText(version.rawText(), version.structuredText());
	}

	@Override
	@Transactional
	public ResumeVersionCreated storeGeneratedVersion(
			UUID sourceVersionId, byte[] content, String fileName, String mimeType, String rawText) {
		return storeGenerated(version(sourceVersionId).resumeId(), content, fileName, mimeType, rawText);
	}

	@Override
	public void assertExists(UUID resumeId, UUID resumeVersionId) {
		ownedVersion(resumeId, resumeVersionId);
	}

	@Override
	@Transactional(readOnly = true)
	public ResumeVersionOrigin origin(UUID resumeId, UUID resumeVersionId) {
		ResumeVersion version = ownedVersion(resumeId, resumeVersionId);
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));
		long number = resumeVersionRepository.countByResumeIdAndCreatedAtLessThanEqual(resumeId, version.createdAt());
		return new ResumeVersionOrigin(Math.toIntExact(number), Optional.ofNullable(resume.sourceAnalysisId()));
	}

	@Override
	@Transactional(readOnly = true)
	public String title(UUID resumeId) {
		return resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId)).title();
	}

	@Override
	@Transactional
	public void rename(UUID resumeId, String title) {
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));
		String trimmed = title.trim();
		resume.rename(trimmed.substring(0, Math.min(trimmed.length(), MAX_TITLE_LENGTH)));
		resumeRepository.save(resume);
	}

	@Override
	@Transactional
	public void setFavorite(UUID resumeId, boolean favorite) {
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));
		if (resume.origin() != ResumeOrigin.BASE) {
			throw new FavoriteResumeMustBeBaseException(resumeId);
		}
		if (favorite) {
			resumeRepository.findByOriginAndFavoriteTrue(ResumeOrigin.BASE).forEach(current -> {
				current.markFavorite(false);
				resumeRepository.save(current);
			});
		}
		resume.markFavorite(favorite);
		resumeRepository.save(resume);
	}

	@Override
	@Transactional
	public void delete(UUID resumeId) {
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));
		List<ResumeVersion> versions = resumeVersionRepository.findByResumeId(resumeId);
		for (ResumeVersion version : versions) {
			if (!version.isScrubbed()) {
				version.scrub();
			}
		}
		resumeVersionRepository.saveAll(versions);
		resume.scrub();
		resumeRepository.save(resume);
	}

	@Override
	@Transactional
	public void deleteVersion(UUID resumeId, UUID resumeVersionId) {
		Resume resume = resumeRepository.findById(resumeId)
				.orElseThrow(() -> new ResumeVersionNotFoundException(resumeVersionId));
		List<ResumeVersion> versions = resumeVersionRepository.findByResumeId(resumeId);
		ResumeVersion version = versions.stream()
				.filter(candidate -> candidate.id().equals(resumeVersionId) && !candidate.isScrubbed())
				.findFirst()
				.orElseThrow(() -> new ResumeVersionNotFoundException(resumeVersionId));
		version.scrub();
		resumeVersionRepository.save(version);
		if (versions.stream().allMatch(ResumeVersion::isScrubbed)) {
			resume.scrub();
			resumeRepository.save(resume);
		}
	}

	private ResumeVersion version(UUID resumeVersionId) {
		return resumeVersionRepository.findById(resumeVersionId)
				.orElseThrow(() -> new ResumeVersionNotFoundException(resumeVersionId));
	}

	private ResumeVersion ownedVersion(UUID resumeId, UUID resumeVersionId) {
		return resumeVersionRepository.findById(resumeVersionId)
				.filter(candidate -> candidate.resumeId().equals(resumeId) && resumeRepository.existsById(resumeId))
				.orElseThrow(() -> new ResumeVersionNotFoundException(resumeVersionId));
	}
}
