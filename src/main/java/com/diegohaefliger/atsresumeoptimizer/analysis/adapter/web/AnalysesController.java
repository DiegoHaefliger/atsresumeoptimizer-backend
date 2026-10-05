package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisAtsView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisCostReportService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisCreation;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisEventService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisFeedbackService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisReportView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.CostReport;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.RecentJobService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.RecentJobView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.ResumeUpload;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
class AnalysesController {

	private final AnalysisService analysisService;
	private final AnalysisQueryService analysisQueryService;
	private final AnalysisFeedbackService analysisFeedbackService;
	private final AnalysisCostReportService analysisCostReportService;
	private final AnalysisEventService analysisEventService;
	private final RecentJobService recentJobService;
	private final JobDetailsMapper jobDetailsMapper;

	AnalysesController(
			AnalysisService analysisService,
			AnalysisQueryService analysisQueryService,
			AnalysisFeedbackService analysisFeedbackService,
			AnalysisCostReportService analysisCostReportService,
			AnalysisEventService analysisEventService,
			RecentJobService recentJobService,
			JobDetailsMapper jobDetailsMapper) {
		this.analysisService = analysisService;
		this.analysisQueryService = analysisQueryService;
		this.analysisFeedbackService = analysisFeedbackService;
		this.analysisCostReportService = analysisCostReportService;
		this.analysisEventService = analysisEventService;
		this.recentJobService = recentJobService;
		this.jobDetailsMapper = jobDetailsMapper;
	}

	@PostMapping(value = "/analyses", consumes = "multipart/form-data")
	ResponseEntity<AnalysisCreatedResponse> create(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "jobDescription", required = false) String jobDescription,
			@RequestParam(value = "targetRole", required = false) String targetRole,
			@Valid @ModelAttribute JobDetailsParams jobDetails) throws IOException {

		var resumeUpload = new ResumeUpload(file.getOriginalFilename(), file.getContentType());
		AnalysisCreation creation = analysisService.create(
				resumeUpload, file.getBytes(), jobDescription, targetRole, jobDetailsMapper.toJobDetails(jobDetails));

		var body = new AnalysisCreatedResponse(creation.id().value(), creation.status());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
	}

	@PostMapping("/resumes/{resumeId}/analyses")
	ResponseEntity<AnalysisCreatedResponse> createFromExistingVersion(
			@PathVariable UUID resumeId, @Valid @RequestBody AnalyzeExistingVersionRequest request) {
		AnalysisCreation creation = analysisService.createFromExistingVersion(resumeId, request.resumeVersionId(),
				request.jobId(), request.jobDescription(), request.targetRole(),
				request.jobDetails() != null ? jobDetailsMapper.toJobDetails(request.jobDetails()) : JobDetails.NONE);
		var body = new AnalysisCreatedResponse(creation.id().value(), creation.status());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
	}

	@GetMapping("/analyses/{id}")
	AnalysisReportView get(@PathVariable UUID id) {
		return analysisQueryService.get(new AnalysisId(id));
	}

	@PostMapping("/analyses/{id}/feedback")
	ResponseEntity<Void> submitFeedback(@PathVariable UUID id, @Valid @RequestBody AnalysisFeedbackRequest request) {
		analysisFeedbackService.submit(new AnalysisId(id), request.rating(), request.comment());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/analyses/cost-report")
	CostReport costReport() {
		return analysisCostReportService.report();
	}

	@GetMapping("/analyses/recent-jobs")
	List<RecentJobView> recentJobs() {
		return recentJobService.recentJobs();
	}

	@DeleteMapping("/analyses/recent-jobs/{jobId}")
	ResponseEntity<Void> removeRecentJob(@PathVariable UUID jobId) {
		recentJobService.remove(jobId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping(value = "/analyses/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter events(@PathVariable UUID id) {
		return analysisEventService.subscribe(new AnalysisId(id));
	}

	@GetMapping("/analyses/{id}/ats-view")
	AnalysisAtsView atsView(@PathVariable UUID id) {
		return analysisQueryService.getAtsView(new AnalysisId(id));
	}
}
