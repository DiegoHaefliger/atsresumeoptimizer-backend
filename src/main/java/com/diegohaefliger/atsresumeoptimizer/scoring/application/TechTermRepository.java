package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import org.springframework.data.jpa.repository.JpaRepository;

interface TechTermRepository extends JpaRepository<TechTerm, String> {
}
