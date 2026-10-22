package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileContent;
import java.util.UUID;

public interface ChatFileService {

	/** Só imagens têm o original guardado; documento guarda só o texto extraído. */
	ChatFileContent image(UUID fileId);
}
