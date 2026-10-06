package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SelectionProcessService {

	List<SelectionProcess> list(Optional<SelectionStage> stage);

	SelectionProcess get(UUID id);

	SelectionProcess create(SelectionProcessData data, SelectionStage initialStage);

	SelectionProcess update(UUID id, SelectionProcessData data);

	SelectionProcess moveTo(UUID id, SelectionStage stage, String note);

	void removeMovement(UUID id, UUID movementId);

	void delete(UUID id);
}
