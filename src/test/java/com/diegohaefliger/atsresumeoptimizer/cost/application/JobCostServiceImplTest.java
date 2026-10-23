package com.diegohaefliger.atsresumeoptimizer.cost.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsageSource;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelPriceLookup;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.CostJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.JobCost;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.StageCost;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobCostServiceImplTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final BigDecimal RATE = new BigDecimal("5.00");

	private final ModelPriceLookup priceLookup = mock(ModelPriceLookup.class);
	private final JobStructuringService jobStructuringService = mock(JobStructuringService.class);

	@BeforeEach
	void jobExists() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(mock(JobOffer.class)));
		when(priceLookup.hasPrice("gpt-4o-mini")).thenReturn(true);
	}

	@Test
	void sumsEachStageAcrossSourcesAndTheTotalAcrossStages() {
		JobCostServiceImpl service = service(
				source(AiStage.JOB_ANALYSIS, usage("gpt-4o-mini", 100, 50, "0.0010", 1, 0)),
				source(AiStage.JOB_ANALYSIS, usage("gpt-4o-mini", 40, 20, "0.0005", 1, 0)),
				source(AiStage.RESUME_ADAPTATION, usage("gpt-4o-mini", 300, 200, "0.0100", 2, 0)),
				source(AiStage.INTERVIEW_PREPARATION, usage("gpt-4o-mini", 10, 10, "0.0002", 1, 0)));

		JobCost cost = service.cost(JOB_ID);

		assertThat(cost.stages()).extracting(StageCost::stage).containsExactly(AiStage.values());
		StageCost analysis = cost.stages().get(0);
		assertThat(analysis.tokensIn()).isEqualTo(140);
		assertThat(analysis.tokensOut()).isEqualTo(70);
		assertThat(analysis.costUsd()).isEqualByComparingTo("0.0015");
		assertThat(analysis.costBrl()).isEqualByComparingTo("0.0075");
		assertThat(cost.totalCostUsd()).isEqualByComparingTo("0.0117");
		assertThat(cost.totalCostBrl()).isEqualByComparingTo("0.0585");
		assertThat(cost.usdBrlRate()).isEqualByComparingTo(RATE);
		assertThat(cost.models()).containsExactly("gpt-4o-mini");
		assertThat(cost.estimated()).isFalse();
	}

	@Test
	void aStageWithoutUsageIsNotExecutedAndCostsNothing() {
		JobCost cost = service(source(AiStage.JOB_ANALYSIS, usage("gpt-4o-mini", 10, 5, "0.0001", 1, 0))).cost(JOB_ID);

		StageCost adaptation = cost.stages().get(1);
		assertThat(adaptation.executed()).isFalse();
		assertThat(adaptation.costUsd()).isEqualByComparingTo("0");
		assertThat(adaptation.models()).isEmpty();
		assertThat(cost.stages().get(0).executed()).isTrue();
	}

	@Test
	void aJobWithNoStageAtAllCostsZeroInsteadOfFailing() {
		JobCost cost = service().cost(JOB_ID);

		assertThat(cost.totalCostUsd()).isEqualByComparingTo("0");
		assertThat(cost.totalCostBrl()).isEqualByComparingTo("0");
		assertThat(cost.stages()).noneMatch(StageCost::executed);
		assertThat(cost.models()).isEmpty();
	}

	@Test
	void cachedCallsCostNothingAndAreFlagged() {
		JobCost cost = service(source(AiStage.JOB_ANALYSIS, usage("gpt-4o-mini", 0, 0, "0.0300", 2, 2))).cost(JOB_ID);

		StageCost analysis = cost.stages().get(0);
		assertThat(analysis.costUsd()).isEqualByComparingTo("0");
		assertThat(analysis.cached()).isTrue();
		assertThat(cost.cached()).isTrue();
		assertThat(cost.totalCostBrl()).isEqualByComparingTo("0");
	}

	@Test
	void aModelWithoutPriceMarksTheStageAndTheTotalAsEstimated() {
		JobCost cost = service(
				source(AiStage.JOB_ANALYSIS, usage("gpt-4o-mini", 10, 5, "0.0001", 1, 0)),
				source(AiStage.RESUME_ADAPTATION, usage("modelo-novo", 10, 5, "0.0002", 1, 0))).cost(JOB_ID);

		assertThat(cost.stages().get(0).estimated()).isFalse();
		assertThat(cost.stages().get(1).estimated()).isTrue();
		assertThat(cost.estimated()).isTrue();
		assertThat(cost.models()).containsExactly("gpt-4o-mini", "modelo-novo");
	}

	@Test
	void keepsSubCentValuesInsteadOfRoundingThemToZero() {
		JobCost cost = service(source(AiStage.JOB_ANALYSIS, usage("gpt-4o-mini", 10, 5, "0.0004", 1, 0))).cost(JOB_ID);

		assertThat(cost.totalCostBrl()).isEqualByComparingTo("0.0020");
	}

	@Test
	void failsWhenTheJobDoesNotExist() {
		UUID unknown = UUID.randomUUID();
		when(jobStructuringService.offer(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().cost(unknown)).isInstanceOf(CostJobNotFoundException.class);
	}

	private JobCostServiceImpl service(AiUsageSource... sources) {
		return new JobCostServiceImpl(List.of(sources), priceLookup, jobStructuringService, new CostProperties(RATE));
	}

	private static ModelUsage usage(String model, long tokensIn, long tokensOut, String costUsd, long calls,
			long cachedCalls) {
		return new ModelUsage(model, tokensIn, tokensOut, new BigDecimal(costUsd), calls, cachedCalls);
	}

	private static AiUsageSource source(AiStage stage, ModelUsage... usages) {
		AiUsageSource source = mock(AiUsageSource.class);
		when(source.stage()).thenReturn(stage);
		when(source.usageByModel(JOB_ID)).thenReturn(List.of(usages));
		return source;
	}
}
