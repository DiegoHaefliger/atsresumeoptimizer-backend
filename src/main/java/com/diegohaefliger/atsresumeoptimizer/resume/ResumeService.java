package com.diegohaefliger.atsresumeoptimizer.resume;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeService {

	ResumeVersionCreated storeUpload(
			String title,
			byte[] content,
			String fileName,
			String mimeType,
			String rawText,
			String structuredText,
			Integer pageCount);

	/** Valida tamanho e páginas e extrai o texto antes de {@link #storeUpload}. */
	default ResumeVersionCreated upload(byte[] content, String fileName, String mimeType) {
		return upload(content, fileName, mimeType, fileName);
	}

	ResumeVersionCreated upload(byte[] content, String fileName, String mimeType, String title);

	/** O número da versão conta as já apagadas, pra "versão 2" não virar "versão 1" depois de excluir a primeira. */
	List<ResumeSummary> listResumes();

	/** Só currículos adaptados cuja análise está na lista, do mais novo para o mais antigo. */
	List<ResumeSummary> listAdaptedFromAnalyses(Collection<UUID> analysisIds);

	/** Nota ATS do currículo gerado, calculada ao fim da adaptação. */
	void updateAtsScore(UUID resumeId, Integer atsScore);

	List<ResumeVersionSummary> listVersions(UUID resumeId);

	byte[] downloadContent(UUID resumeVersionId);

	ResumeVersionText getVersionText(UUID resumeVersionId);

	ResumeVersionCreated storeGeneratedVersion(
			UUID sourceVersionId, byte[] content, String fileName, String mimeType, String rawText);

	/** Cria um currículo à parte, ligado à análise, pra não misturar o resultado da reescrita com o currículo base. */
	ResumeVersionCreated storeAdapted(UUID sourceVersionId, UUID analysisId, String titleSuffix, byte[] content,
			String fileName, String mimeType, String rawText);

	Optional<UUID> latestAdaptedVersion(UUID analysisId);

	/** Versão de outro currículo responde 404 igual a inexistente, pra não vazar existência. */
	ResumeVersionInfo getVersionInfo(UUID resumeId, UUID resumeVersionId);

	void assertExists(UUID resumeId, UUID resumeVersionId);

	String title(UUID resumeId);

	void rename(UUID resumeId, String title);

	/** Só um base fica favorito por vez; marcar outro desmarca o anterior. */
	void setFavorite(UUID resumeId, boolean favorite);

	/** LGPD: apaga arquivo e texto mas mantém as linhas, pra análises antigas continuarem válidas sem dado pessoal. */
	void delete(UUID resumeId);

	void deleteVersion(UUID resumeId, UUID resumeVersionId);
}
