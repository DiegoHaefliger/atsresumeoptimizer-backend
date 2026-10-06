package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "selection_process")
class SelectionProcessEntity {

	@Id
	private UUID id;

	private String company;

	@Column(name = "job_title")
	private String jobTitle;

	@Column(name = "job_url")
	private String jobUrl;

	@Column(name = "process_url")
	private String processUrl;

	@Enumerated(EnumType.STRING)
	private SelectionStage stage;

	@Column(name = "applied_on")
	private LocalDate appliedOn;

	@Column(name = "next_step_on")
	private LocalDate nextStepOn;

	@Column(name = "contact_name")
	private String contactName;

	@Column(name = "contact_email")
	private String contactEmail;

	private BigDecimal salary;

	private String notes;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected SelectionProcessEntity() {
	}

	SelectionProcessEntity(UUID id, SelectionStage stage, Instant now) {
		this.id = id;
		this.stage = stage;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public String getJobUrl() {
		return jobUrl;
	}

	public void setJobUrl(String jobUrl) {
		this.jobUrl = jobUrl;
	}

	public String getProcessUrl() {
		return processUrl;
	}

	public void setProcessUrl(String processUrl) {
		this.processUrl = processUrl;
	}

	public SelectionStage getStage() {
		return stage;
	}

	public void setStage(SelectionStage stage) {
		this.stage = stage;
	}

	public LocalDate getAppliedOn() {
		return appliedOn;
	}

	public void setAppliedOn(LocalDate appliedOn) {
		this.appliedOn = appliedOn;
	}

	public LocalDate getNextStepOn() {
		return nextStepOn;
	}

	public void setNextStepOn(LocalDate nextStepOn) {
		this.nextStepOn = nextStepOn;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public BigDecimal getSalary() {
		return salary;
	}

	public void setSalary(BigDecimal salary) {
		this.salary = salary;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
