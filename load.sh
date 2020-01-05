#!/usr/bin/env bash
# shellcheck disable=SC2039

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
readonly project_directory="$(readlink -f "${script_directory}")"

declare -a files
mapfile -t files < <(find "${project_directory}/src" -type f)


for file in "${files[@]}"; do
  echo "loading '${file}'"
  ruby "${project_directory}/Assets/Arcadia/Editor/repl-client.rb" <<< "$(cat "${file}")"
done
