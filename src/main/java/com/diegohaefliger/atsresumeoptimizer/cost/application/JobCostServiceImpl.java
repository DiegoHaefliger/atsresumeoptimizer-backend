package com.diegohaefliger.atsresumeoptimizer.cost.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsageSource;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelPriceLookup;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.CostJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.JobCost;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.StageCost;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class JobCostServiceImpl implements JobCostService {

	private static final int COST_SCALE = 4;

	private final List<AiUsageSource> sources;
	private final ModelPriceLookup priceLookup;
	private final JobStructuringService jobStructuringService;
	private final CostProperties properties;

	JobCostServiceImpl(List<AiUsageSource> sources, ModelPriceLookup priceLookup,
			JobStructuringService jobStructuringService, CostProperties properties) {
		this.sources = sources;
		this.priceLookup = priceLookup;
		this.jobStructuringService = jobStructuringService;
		this.properties = properties;
	}

	@Override
	public JobCost cost(UUID jobPostingId) {
		if (jobStructuringService.offer(jobPostingId).isEmpty()) {
			throw new CostJobNotFoundException(jobPostingId);
		}
		List<StageCost> stages = Arrays.stream(AiStage.values()).map(stage -> stageCost(stage, jobPostingId)).toList();
		BigDecimal totalUsd = stages.stream().map(StageCost::costUsd).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new JobCost(totalUsd, toBrl(totalUsd), properties.usdBrlRate(),
				stages.stream().flatMap(stage -> stage.models().stream()).distinct().toList(),
				stages.stream().anyMatch(StageCost::estimated), stages.stream().anyMatch(StageCost::cached), stages);
	}

	private StageCost stageCost(AiStage stage, UUID jobPostingId) {
		List<ModelUsage> usages = sources.stream()
				.filter(source -> source.stage() == stage)
				.flatMap(source -> source.usageByModel(jobPostingId).stream())
				.toList();
		long tokensIn = usages.stream().mapToLong(ModelUsage::tokensIn).sum();
		long tokensOut = usages.stream().mapToLong(ModelUsage::tokensOut).sum();
		BigDecimal costUsd = usages.stream().map(this::billedCostUsd).reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(COST_SCALE, RoundingMode.HALF_UP);
		List<String> models = usages.stream().map(ModelUsage::model).filter(Objects::nonNull).distinct().sorted().toList();
		boolean estimated = usages.stream().anyMatch(usage -> usage.model() != null
				&& billedCostUsd(usage).signum() > 0 && !priceLookup.hasPrice(usage.model()));
		boolean cached = usages.stream().anyMatch(usage -> usage.cachedCalls() > 0);
		return new StageCost(stage, !usages.isEmpty(), tokensIn, tokensOut, costUsd, toBrl(costUsd), models, estimated, cached);
	}

	private BigDecimal billedCostUsd(ModelUsage usage) {
		return usage.cachedCalls() >= usage.calls() ? BigDecimal.ZERO : usage.costUsd();
	}

	private BigDecimal toBrl(BigDecimal usd) {
		return usd.multiply(properties.usdBrlRate()).setScale(COST_SCALE, RoundingMode.HALF_UP);
	}
}
