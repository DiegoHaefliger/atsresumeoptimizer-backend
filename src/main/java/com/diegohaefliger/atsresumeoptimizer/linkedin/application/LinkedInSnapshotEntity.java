package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "linkedin_snapshot")
class LinkedInSnapshotEntity {

	@Id
	private UUID id;

	@Column(name = "captured_at")
	private Instant capturedAt;

	private String source;

	@Convert(converter = LinkedInProfileConverter.class)
	private LinkedInProfile content;

	private byte[] photo;

	@Column(name = "photo_mime_type")
	private String photoMimeType;

	private byte[] banner;

	@Column(name = "banner_mime_type")
	private String bannerMimeType;

	protected LinkedInSnapshotEntity() {
	}

	LinkedInSnapshotEntity(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	public Instant getCapturedAt() {
		return capturedAt;
	}

	public void setCapturedAt(Instant capturedAt) {
		this.capturedAt = capturedAt;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public LinkedInProfile getContent() {
		return content;
	}

	public void setContent(LinkedInProfile content) {
		this.content = content;
	}

	public byte[] getPhoto() {
		return photo;
	}

	public void setPhoto(byte[] photo) {
		this.photo = photo;
	}

	public String getPhotoMimeType() {
		return photoMimeType;
	}

	public void setPhotoMimeType(String photoMimeType) {
		this.photoMimeType = photoMimeType;
	}

	public byte[] getBanner() {
		return banner;
	}

	public void setBanner(byte[] banner) {
		this.banner = banner;
	}

	public String getBannerMimeType() {
		return bannerMimeType;
	}

	public void setBannerMimeType(String bannerMimeType) {
		this.bannerMimeType = bannerMimeType;
	}
}
