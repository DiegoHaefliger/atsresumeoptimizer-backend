package com.diegohaefliger.atsresumeoptimizer.careerprofile.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "career_profile")
class CareerProfileEntity {

	@Id
	private UUID id;

	@Column(name = "area_of_expertise")
	private String areaOfExpertise;

	@Column(name = "current_job_title")
	private String currentJobTitle;

	@Column(name = "current_seniority")
	private String currentSeniority;

	@Column(name = "target_job_title")
	private String targetJobTitle;

	@Column(name = "target_seniority")
	private String targetSeniority;

	@Column(name = "target_timeframe")
	private String targetTimeframe;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "target_industries")
	private List<String> targetIndustries;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "target_companies")
	private List<String> targetCompanies;

	@Column(name = "location")
	private String location;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "languages")
	private List<String> languages;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "strengths")
	private List<String> strengths;

	@Column(name = "differentiators")
	private String differentiators;

	@Column(name = "tone")
	private String tone;

	@Column(name = "profile_language")
	private String profileLanguage;

	@Column(name = "notes")
	private String notes;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected CareerProfileEntity() {
	}

	CareerProfileEntity(UUID id) {
		this.id = id;
	}

	public String getAreaOfExpertise() {
		return areaOfExpertise;
	}

	public void setAreaOfExpertise(String areaOfExpertise) {
		this.areaOfExpertise = areaOfExpertise;
	}

	public String getCurrentJobTitle() {
		return currentJobTitle;
	}

	public void setCurrentJobTitle(String currentJobTitle) {
		this.currentJobTitle = currentJobTitle;
	}

	public String getCurrentSeniority() {
		return currentSeniority;
	}

	public void setCurrentSeniority(String currentSeniority) {
		this.currentSeniority = currentSeniority;
	}

	public String getTargetJobTitle() {
		return targetJobTitle;
	}

	public void setTargetJobTitle(String targetJobTitle) {
		this.targetJobTitle = targetJobTitle;
	}

	public String getTargetSeniority() {
		return targetSeniority;
	}

	public void setTargetSeniority(String targetSeniority) {
		this.targetSeniority = targetSeniority;
	}

	public String getTargetTimeframe() {
		return targetTimeframe;
	}

	public void setTargetTimeframe(String targetTimeframe) {
		this.targetTimeframe = targetTimeframe;
	}

	public List<String> getTargetIndustries() {
		return targetIndustries;
	}

	public void setTargetIndustries(List<String> targetIndustries) {
		this.targetIndustries = targetIndustries;
	}

	public List<String> getTargetCompanies() {
		return targetCompanies;
	}

	public void setTargetCompanies(List<String> targetCompanies) {
		this.targetCompanies = targetCompanies;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public List<String> getLanguages() {
		return languages;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	public List<String> getStrengths() {
		return strengths;
	}

	public void setStrengths(List<String> strengths) {
		this.strengths = strengths;
	}

	public String getDifferentiators() {
		return differentiators;
	}

	public void setDifferentiators(String differentiators) {
		this.differentiators = differentiators;
	}

	public String getTone() {
		return tone;
	}

	public void setTone(String tone) {
		this.tone = tone;
	}

	public String getProfileLanguage() {
		return profileLanguage;
	}

	public void setProfileLanguage(String profileLanguage) {
		this.profileLanguage = profileLanguage;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
