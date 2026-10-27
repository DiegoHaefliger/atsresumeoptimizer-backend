package com.diegohaefliger.atsresumeoptimizer.careerprofile;

import java.util.Optional;

public interface CareerProfileService {

	Optional<CareerProfile> current();

	CareerProfile save(CareerProfile profile);
}
