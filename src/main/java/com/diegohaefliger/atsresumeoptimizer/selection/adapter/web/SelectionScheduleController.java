package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/selection-processes/{processId}/schedules")
class SelectionScheduleController {

	private final SelectionScheduleService service;
	private final SelectionScheduleWebMapper mapper;

	SelectionScheduleController(SelectionScheduleService service, SelectionScheduleWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<SelectionScheduleResponse> list(@PathVariable UUID processId) {
		return service.list(processId).stream().map(mapper::toResponse).toList();
	}

	@PostMapping
	ResponseEntity<SelectionScheduleResponse> schedule(
			@PathVariable UUID processId, @Valid @RequestBody SelectionScheduleRequest request) {
		SelectionScheduleResponse created = mapper.toResponse(service.schedule(processId, mapper.toData(request)));
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/{scheduleId}/reschedule")
	SelectionScheduleResponse reschedule(@PathVariable UUID processId, @PathVariable UUID scheduleId,
			@Valid @RequestBody SelectionScheduleRequest request) {
		return mapper.toResponse(service.reschedule(processId, scheduleId, mapper.toData(request)));
	}

	@PostMapping("/{scheduleId}/complete")
	SelectionScheduleResponse complete(@PathVariable UUID processId, @PathVariable UUID scheduleId) {
		return mapper.toResponse(service.complete(processId, scheduleId));
	}

	@PostMapping("/{scheduleId}/cancel")
	SelectionScheduleResponse cancel(@PathVariable UUID processId, @PathVariable UUID scheduleId) {
		return mapper.toResponse(service.cancel(processId, scheduleId));
	}
}
