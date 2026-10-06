package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GoogleEventLinkRepository extends JpaRepository<GoogleEventLinkEntity, UUID> {
}
