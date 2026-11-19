package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidLinkedInSnapshotException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInImage;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotSummary;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LinkedInSnapshotServiceImpl implements LinkedInSnapshotService {

	private final LinkedInSnapshotRepository repository;
	private final LinkedInSnapshotEntityMapper mapper;

	LinkedInSnapshotServiceImpl(LinkedInSnapshotRepository repository, LinkedInSnapshotEntityMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public LinkedInCapture capture(NewLinkedInSnapshot snapshot) {
		if (snapshot.profile().lacksContent()) {
			throw new InvalidLinkedInSnapshotException(
					"Nenhum conteúdo do perfil foi lido. Abra o seu perfil no LinkedIn e tente de novo.");
		}
		Map<ProfileSection, String> incoming = LinkedInContentFingerprint.of(snapshot.profile(), bytes(snapshot.photo()),
				bytes(snapshot.banner()));
		Optional<LinkedInSnapshotEntity> sameSource = repository.findFirstBySourceOrderByCapturedAtDesc(snapshot.source().name());
		if (sameSource.isPresent() && LinkedInContentFingerprint.changed(fingerprintOf(sameSource.get()), incoming).isEmpty()) {
			sameSource.get().setCapturedAt(Instant.now());
			return new LinkedInCapture(mapper.toDomain(repository.save(sameSource.get())), true, List.of());
		}
		Optional<LinkedInSnapshotEntity> reference = sameSource.or(repository::findFirstByOrderByCapturedAtDesc);
		List<ProfileSection> changed = reference.isPresent()
				? LinkedInContentFingerprint.changed(fingerprintOf(reference.get()), incoming)
				: incoming.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(Map.Entry::getKey).toList();
		return new LinkedInCapture(store(snapshot), false, changed);
	}

	private Map<ProfileSection, String> fingerprintOf(LinkedInSnapshotEntity entity) {
		return LinkedInContentFingerprint.of(mapper.toDomain(entity));
	}

	private LinkedInSnapshot store(NewLinkedInSnapshot snapshot) {
		LinkedInSnapshotEntity entity = new LinkedInSnapshotEntity(UuidCreator.getTimeOrderedEpoch());
		entity.setCapturedAt(Instant.now());
		entity.setSource(snapshot.source().name());
		entity.setContent(snapshot.profile());
		LinkedInImage photo = snapshot.photo();
		if (photo != null) {
			entity.setPhoto(photo.data());
			entity.setPhotoMimeType(photo.mimeType());
		}
		LinkedInImage banner = snapshot.banner();
		if (banner != null) {
			entity.setBanner(banner.data());
			entity.setBannerMimeType(banner.mimeType());
		}
		return mapper.toDomain(repository.save(entity));
	}

	private static byte[] bytes(LinkedInImage image) {
		return image == null ? null : image.data();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LinkedInSnapshot> latest() {
		return repository.findFirstByOrderByCapturedAtDesc().map(mapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public LinkedInSnapshot get(UUID id) {
		return repository.findById(id).map(mapper::toDomain).orElseThrow(LinkedInSnapshotNotFoundException::new);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LinkedInSnapshotSummary> history() {
		return repository.findAllByOrderByCapturedAtDesc().stream().map(mapper::toSummary).toList();
	}
}
