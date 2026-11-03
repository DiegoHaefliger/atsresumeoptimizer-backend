package com.diegohaefliger.atsresumeoptimizer.feedback.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.feedback.application.FeedbackService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/feedback")
class FeedbackController {

	private final FeedbackService service;
	private final FeedbackWebMapper mapper;

	FeedbackController(FeedbackService service, FeedbackWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	FeedbackResponse submit(@Valid @ModelAttribute FeedbackRequest request,
			@RequestParam(name = "files", required = false) List<MultipartFile> files) {
		List<MultipartFile> uploaded = files == null ? List.of() : files;
		return mapper.toResponse(service.submit(mapper.toDomain(request), uploaded.stream().map(mapper::toAttachment).toList()));
	}
}
