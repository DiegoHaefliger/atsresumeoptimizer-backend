package com.diegohaefliger.atsresumeoptimizer.resume.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeUploadLimits;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionInfo;
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

	private final ResumeStorage storage;
	private final ResumeRepository resumeRepository;
	private final ResumeVersionRepository resumeVersionRepository;
	private final ResumeAnalysisPipeline parsingPipeline;

	ResumeServiceImpl(
			ResumeStorage storage,
			ResumeRepository resumeRepository,
			ResumeVersionRepository resumeVersionRepository,
			ResumeAnalysisPipeline parsingPipeline) {
		this.storage = storage;
		this.resumeRepository = resumeRepository;
		this.resumeVersionRepository = resumeVersionRepository;
		this.parsingPipeline = parsingPipeline;
	}

	@Override
	@Transactional
	public ResumeVersionCreated storeUpload(
			String title,
			byte[] content,
			String fileName,
			String mimeType,
			String rawText,
			String structuredText,
			Integer pageCount) {
		String sha256 = Sha256.hex(content);

		Resume resume = resumeRepository.save(Resume.base(UuidCreator.getTimeOrderedEpoch(), title));
		return storeFirstVersion(resume, content, fileName, mimeType, sha256, pageCount, rawText, structuredText);
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
		return storeFirstVersion(resume, content, fileName, mimeType, Sha256.hex(content), null, rawText, rawText);
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

	private ResumeVersionCreated storeFirstVersion(Resume resume, byte[] content, String fileName, String mimeType,
			String sha256, Integer pageCount, String rawText, String structuredText) {
		UUID versionId = UuidCreator.getTimeOrderedEpoch();
		String storageKey = storageKey(resume.id(), versionId, fileName);
		storage.upload(storageKey, content, mimeType);

		ResumeVersion version = resumeVersionRepository.save(new ResumeVersion(
				versionId, resume.id(), storageKey, fileName, mimeType, content.length,
				sha256, pageCount, rawText, structuredText));

		return new ResumeVersionCreated(resume.id(), version.id(), storageKey, version.sizeBytes(), sha256, pageCount);
	}

	@Override
	@Transactional
	public ResumeVersionCreated upload(byte[] content, String fileName, String mimeType, String title) {
		ResumeUploadLimits.validateSize(content.length);
		ParsingResult parsingResult = parsingPipeline.analyze(content);
		ResumeUploadLimits.validatePageCount(parsingResult.document().pageCount());
		return storeUpload(resolveTitle(title, fileName), content, fileName, mimeType, parsingResult.document().rawText(),
				parsingResult.document().structuredText(), parsingResult.document().pageCount());
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
		return summarize(resumeRepository.findTop50ByTitleNotOrderByCreatedAtDesc(Resume.SCRUBBED_PLACEHOLDER)).stream()
				.sorted(Comparator.comparing(ResumeSummary::favorite).reversed())
				.toList();
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
						resume.sourceAnalysisId(), resume.favorite(), versionsByResume.get(resume.id())))
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

	private static String storageKey(UUID resumeId, UUID versionId, String fileName) {
		return "%s/%s/%s".formatted(resumeId, versionId, fileName);
	}

	private static ResumeVersionSummary summary(ResumeVersion version, int number) {
		return new ResumeVersionSummary(version.id(), number, version.fileName(), version.mimeType(), version.sizeBytes(),
				version.pageCount(), version.createdAt());
	}

	@Override
	public byte[] downloadContent(UUID resumeVersionId) {
		return storage.download(version(resumeVersionId).storageKey());
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
		ResumeVersion sourceVersion = version(sourceVersionId);

		String sha256 = Sha256.hex(content);
		UUID versionId = UuidCreator.getTimeOrderedEpoch();
		String storageKey = storageKey(sourceVersion.resumeId(), versionId, fileName);
		storage.upload(storageKey, content, mimeType);

		ResumeVersion version = resumeVersionRepository.save(new ResumeVersion(
				versionId, sourceVersion.resumeId(), storageKey, fileName, mimeType,
				content.length, sha256, null, rawText, rawText));

		return new ResumeVersionCreated(sourceVersion.resumeId(), version.id(), storageKey, version.sizeBytes(), sha256, null);
	}

	@Override
	public ResumeVersionInfo getVersionInfo(UUID resumeId, UUID resumeVersionId) {
		ResumeVersion version = ownedVersion(resumeId, resumeVersionId);
		byte[] content = storage.download(version.storageKey());
		return new ResumeVersionInfo(content, version.fileName(), version.mimeType());
	}

	@Override
	public void assertExists(UUID resumeId, UUID resumeVersionId) {
		ownedVersion(resumeId, resumeVersionId);
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
				storage.delete(version.storageKey());
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
		storage.delete(version.storageKey());
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
