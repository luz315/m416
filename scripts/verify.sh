#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

verify_harness() {
  local failed=0 file name target directory
  local required=(README.md AGENTS.md docs/project.md docs/architecture.md
    docs/development.md docs/verification.md docs/domain/overview.md docs/model-policy.md docs/java-style.md
    .codex/config.toml .codex/agents/light-worker.toml .codex/agents/deep-worker.toml
    docs/decisions/README.md docs/tasks/TEMPLATE.md scripts/verify.sh)
  local skills=(build-feature plan-feature implement-feature review-code verify-feature)
  for name in "${skills[@]}"; do
    required+=(".agents/skills/$name/SKILL.md")
  done
  for file in "${required[@]}"; do
    if [[ ! -f "$file" ]] || ! grep -q '[^[:space:]]' "$file"; then
      echo "실패: 필수 파일이 없거나 비어 있음: $file" >&2
      failed=1
    fi
  done
  for name in "${skills[@]}"; do
    file=".agents/skills/$name/SKILL.md"
    [[ -f "$file" ]] || continue
    # 현재 Skill에서 사용하는 한 줄 name/description 형식만 검사한다.
    if ! awk -v expected="$name" '
      NR == 1 { if ($0 != "---") exit 1; next }
      $0 == "---" { closed=1; exit }
      /^name: / { if ($0 == "name: " expected) valid_name=1 }
      /^description: / { if ($0 ~ /^description: [^[:space:]]/) description=1 }
      END { if (!closed || !valid_name || !description) exit 1 }
    ' "$file"; then
      echo "실패: Skill 이름 또는 설명 형식 오류: $name" >&2
      failed=1
    fi
  done
  while IFS= read -r file; do
    directory="${file%/*}"
    [[ "$directory" != "$file" ]] || directory=.
    while IFS= read -r target; do
      case "$target" in
        \#*|/*) continue ;;
      esac
      [[ "$target" =~ ^[a-zA-Z][a-zA-Z0-9+.-]*: ]] && continue
      target="${target%%#*}"
      target="${target%%\?*}"
      if [[ -n "$target" && ! -e "$directory/$target" ]]; then
        echo "실패: 잘못된 상대 링크: $file -> $target" >&2
        failed=1
      fi
    done < <(awk '
      /^[[:space:]]*```/ { fence=!fence; next }
      fence { next }
      {
        line=$0
        gsub(/`[^`]*`/, "", line)
        while (match(line, /\[[^]]+\]\([^[:space:]()]+\)/)) {
          link=substr(line,RSTART,RLENGTH)
          sub(/^[^]]*\]\(/,"",link); sub(/\)$/, "", link)
          print link
          line=substr(line,RSTART+RLENGTH)
        }
      }
    ' "$file")
  done < <(printf '%s\n' README.md AGENTS.md; find docs .agents/skills -type f -name '*.md' 2>/dev/null)
  for file in scripts/verify.sh scripts/verify-app.sh; do
    [[ -f "$file" ]] || continue
    if ! bash -n "$file"; then failed=1; fi
  done
  [[ "$failed" -eq 0 ]] || return 1
  echo '통과: 하네스 파일, Skill 기본 정보, 단순 상대 링크, 셸 문법'
}

verify_app() {
  if [[ ! -f scripts/verify-app.sh ]]; then
    echo '미설정: Java 빌드 도구를 결정한 뒤 scripts/verify-app.sh에 검사를 연결하세요.' >&2
    return 2
  fi
  if bash scripts/verify-app.sh; then
    echo '통과: 연결된 애플리케이션 검사'
  else
    echo '실패: 애플리케이션 검사' >&2
    return 1
  fi
}

if [[ "$#" -gt 1 ]]; then
  echo '사용법: bash scripts/verify.sh [harness|app|all]' >&2
  exit 2
fi
case "${1:-all}" in
  harness) verify_harness ;;
  app) verify_app ;;
  all) verify_harness; verify_app ;;
  *) echo '사용법: bash scripts/verify.sh [harness|app|all]' >&2; exit 2 ;;
esac
