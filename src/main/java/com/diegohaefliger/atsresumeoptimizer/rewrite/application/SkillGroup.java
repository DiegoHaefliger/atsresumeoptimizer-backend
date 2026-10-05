package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.List;

record SkillGroup(String label, List<String> items) {

	SkillGroup {
		items = List.copyOf(items);
	}
}
