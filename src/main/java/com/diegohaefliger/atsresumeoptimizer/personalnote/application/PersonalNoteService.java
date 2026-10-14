package com.diegohaefliger.atsresumeoptimizer.personalnote.application;

import com.diegohaefliger.atsresumeoptimizer.personalnote.domain.PersonalNote;

public interface PersonalNoteService {

	PersonalNote get();

	PersonalNote save(String content);
}
