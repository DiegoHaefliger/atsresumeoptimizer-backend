#!/usr/bin/env bash
# Le os dependency-check-report.json gerados pelo OWASP Dependency-Check e
# escreve um resumo legivel no Job Summary do GitHub Actions, alem de emitir
# ::error:: por CVE (aparece anotado no topo do run, sem precisar abrir o log).
#
# O log cru do plugin so diz "CVE-X(8.1)" e nao diz PARA QUAL VERSAO subir —
# esse script extrai o versionEndExcluding do CPE vulneravel, que e a menor
# versao corrigida conhecida pela NVD.
#
# Uso: summarize-dependency-check.sh <cvss-threshold> [diretorio-raiz]
set -euo pipefail

THRESHOLD="${1:-7.0}"
ROOT="${2:-.}"
SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

mapfile -t REPORTS < <(find "$ROOT" -path "*/target/dependency-check-report.json" | sort)

if [ ${#REPORTS[@]} -eq 0 ]; then
  echo "Nenhum dependency-check-report.json encontrado em $ROOT — scan nao chegou a rodar." >>"$SUMMARY"
  exit 0
fi

# Uma linha por (modulo, jar, CVE) com score >= threshold.
# fix = menor versionEndExcluding entre os CPEs vulneraveis do CVE.
ROWS=$(
  for report in "${REPORTS[@]}"; do
    module=backend
    jq -r --arg module "$module" --argjson threshold "$THRESHOLD" '
      (.dependencies // [])[]
      | .fileName as $jar
      | (.vulnerabilities // [])[]
      | (.cvssv3.baseScore // .cvssv2.score // 0) as $score
      | select($score >= $threshold)
      | [
          $module,
          $jar,
          .name,
          ($score | tostring),
          (.severity // "UNKNOWN"),
          ([ (.vulnerableSoftware // [])[].software.versionEndExcluding // empty ] | unique | join(", ") | if . == "" then "consultar o CVE" else . end)
        ]
      | @tsv
    ' "$report"
  done | sort -u
)

{
  echo "## Gate de seguranca: OWASP Dependency-Check"
  echo
  if [ -z "$ROWS" ]; then
    echo "Nenhuma vulnerabilidade com CVSS >= ${THRESHOLD}. Gate passou."
    exit 0
  fi

  echo "Build barrado: CVSS >= ${THRESHOLD} em pelo menos uma dependencia."
  echo
  echo "| Modulo | Dependencia | CVE | CVSS | Severidade | Versoes corrigidas |"
  echo "|---|---|---|---|---|---|"
  printf '%s\n' "$ROWS" | while IFS=$'\t' read -r module jar cve score severity fix; do
    echo "| \`${module}\` | \`${jar}\` | [${cve}](https://nvd.nist.gov/vuln/detail/${cve}) | ${score} | ${severity} | \`${fix}\` |"
  done
  echo
  echo "**Como corrigir:** a coluna \"Versoes corrigidas\" lista uma versao por linha de release (backport). Escolha a da"
  echo "linha que voce ja usa — nao a maior da lista."
  echo
  echo "> Atencao: versao maior nem sempre e versao corrigida. No jackson-databind o fix saiu em 2.18.8 e 2.21.4:"
  echo "> subir de 2.18.2 pra 2.19.0 **aumenta** o numero e continua vulneravel, porque 2.19/2.20 ficaram sem backport."
  echo
  echo "Relatorio HTML completo: artefato \`dependency-check-report\` deste run."
} >>"$SUMMARY"

# Anotacoes: aparecem no topo do run e no diff, sem abrir o log.
printf '%s\n' "$ROWS" | while IFS=$'\t' read -r module jar cve score severity fix; do
  echo "::error title=${cve} (CVSS ${score}) em ${jar}::Modulo ${module}. Versoes corrigidas: ${fix} (escolha a da sua linha de release). Detalhes: https://nvd.nist.gov/vuln/detail/${cve}"
done
