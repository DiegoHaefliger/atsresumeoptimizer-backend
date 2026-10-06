package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/selection-processes")
class SelectionProcessController {

	private static final SelectionStage DEFAULT_INITIAL_STAGE = SelectionStage.INTERESTED;

	private final SelectionProcessService service;
	private final SelectionProcessWebMapper mapper;

	SelectionProcessController(SelectionProcessService service, SelectionProcessWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<SelectionProcessResponse> list(@RequestParam Optional<SelectionStage> stage) {
		return service.list(stage).stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/{id}")
	SelectionProcessResponse get(@PathVariable UUID id) {
		return mapper.toResponse(service.get(id));
	}

	@PostMapping
	ResponseEntity<SelectionProcessResponse> create(@Valid @RequestBody SelectionProcessRequest request) {
		SelectionStage initialStage = request.stage() == null ? DEFAULT_INITIAL_STAGE : request.stage();
		SelectionProcessResponse created =
				mapper.toResponse(service.create(mapper.toData(request), initialStage));
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	SelectionProcessResponse update(@PathVariable UUID id, @Valid @RequestBody SelectionProcessRequest request) {
		return mapper.toResponse(service.update(id, mapper.toData(request)));
	}

	@PostMapping("/{id}/stage")
	SelectionProcessResponse moveStage(@PathVariable UUID id, @Valid @RequestBody MoveStageRequest request) {
		return mapper.toResponse(service.moveTo(id, request.stage(), request.note()));
	}

	@DeleteMapping("/{id}/history/{movementId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeMovement(@PathVariable UUID id, @PathVariable UUID movementId) {
		service.removeMovement(id, movementId);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		service.delete(id);
	}
}
