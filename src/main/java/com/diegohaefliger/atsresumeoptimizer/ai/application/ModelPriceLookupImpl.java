package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ModelPriceLookup;
import org.springframework.stereotype.Component;

@Component
class ModelPriceLookupImpl implements ModelPriceLookup {

	@Override
	public boolean hasPrice(String model) {
		return ModelPricing.hasPrice(model);
	}
}
