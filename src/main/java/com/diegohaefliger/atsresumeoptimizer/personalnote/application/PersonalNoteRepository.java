package com.diegohaefliger.atsresumeoptimizer.personalnote.application;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PersonalNoteRepository extends JpaRepository<PersonalNoteEntity, UUID> {
}
