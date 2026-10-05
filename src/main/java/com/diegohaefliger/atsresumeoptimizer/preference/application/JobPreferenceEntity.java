package com.diegohaefliger.atsresumeoptimizer.preference.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_preference")
class JobPreferenceEntity {

	@Id
	private UUID id;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "work_models")
	private List<String> workModels;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "contract_types")
	private List<String> contractTypes;

	@Column(name = "min_salary")
	private BigDecimal minSalary;

	@Column(name = "desired_salary")
	private BigDecimal desiredSalary;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "benefits")
	private List<String> benefits;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "seniorities")
	private List<String> seniorities;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "locations")
	private List<String> locations;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "preferred_companies")
	private List<String> preferredCompanies;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "avoided_companies")
	private List<String> avoidedCompanies;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected JobPreferenceEntity() {
	}

	JobPreferenceEntity(UUID id) {
		this.id = id;
	}

	public List<String> getWorkModels() {
		return workModels;
	}

	public void setWorkModels(List<String> workModels) {
		this.workModels = workModels;
	}

	public List<String> getContractTypes() {
		return contractTypes;
	}

	public void setContractTypes(List<String> contractTypes) {
		this.contractTypes = contractTypes;
	}

	public BigDecimal getMinSalary() {
		return minSalary;
	}

	public void setMinSalary(BigDecimal minSalary) {
		this.minSalary = minSalary;
	}

	public BigDecimal getDesiredSalary() {
		return desiredSalary;
	}

	public void setDesiredSalary(BigDecimal desiredSalary) {
		this.desiredSalary = desiredSalary;
	}

	public List<String> getBenefits() {
		return benefits;
	}

	public void setBenefits(List<String> benefits) {
		this.benefits = benefits;
	}

	public List<String> getSeniorities() {
		return seniorities;
	}

	public void setSeniorities(List<String> seniorities) {
		this.seniorities = seniorities;
	}

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	public List<String> getPreferredCompanies() {
		return preferredCompanies;
	}

	public void setPreferredCompanies(List<String> preferredCompanies) {
		this.preferredCompanies = preferredCompanies;
	}

	public List<String> getAvoidedCompanies() {
		return avoidedCompanies;
	}

	public void setAvoidedCompanies(List<String> avoidedCompanies) {
		this.avoidedCompanies = avoidedCompanies;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
