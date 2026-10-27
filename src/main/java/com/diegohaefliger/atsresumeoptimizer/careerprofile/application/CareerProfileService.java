package com.diegohaefliger.atsresumeoptimizer.careerprofile.application;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.domain.CareerProfile;
import java.util.Optional;

public interface CareerProfileService {

	Optional<CareerProfile> current();

	CareerProfile save(CareerProfile profile);
}
