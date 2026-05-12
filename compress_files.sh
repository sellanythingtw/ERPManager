#!/bin/bash

cd "$(dirname "$0")" || exit 1

timestamp=$(date +"%y%m%d%H%M")
folder_name="$(basename "$PWD")"
zipfile="${folder_name}_source_${timestamp}.zip"

zip -r -X "$zipfile" "pom.xml" "src" \
  -x "*/.DS_Store" -x "__MACOSX/*" -x "*/._*" -x "*/.Spotlight-V100/*" -x "*/.Trashes/*" "*/logs/*"