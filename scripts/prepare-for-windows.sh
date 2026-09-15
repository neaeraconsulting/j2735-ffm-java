#!/bin/bash
set -euo pipefail

# Prepare asn1c-generated C sources for Windows compilation.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENERATED_DIR="${1:-$REPO_ROOT/generated-files/2024}"

if [[ ! -d "$GENERATED_DIR" ]]; then
  echo "Generated C sources not found at '$GENERATED_DIR'." >&2
  echo "Run a Linux Docker build first to populate generated-files/2024." >&2
  exit 1
fi

echo "Preparing generated C sources for Windows build in $GENERATED_DIR"

files=(
  GeneralizedTime.h GeneralizedTime.c GeneralizedTime_ber.c
  GeneralizedTime_print.c GeneralizedTime_rfill.c GeneralizedTime_xer.c
  Period.c Period.h
  AggregatedSingleTariffClassSession.c AggregatedSingleTariffClassSession.h
  DetectedChargeObject.c DetectedChargeObject.h
  converter-example.c
)

for file in "${files[@]}"; do
  path="$GENERATED_DIR/$file"
  if [[ -f "$path" ]]; then
    rm -f "$path"
    echo "  removed $file"
  fi
done

pdu_path="$GENERATED_DIR/pdu_collection.c"
if [[ ! -f "$pdu_path" ]]; then
  echo "Missing pdu_collection.c at $pdu_path" >&2
  exit 1
fi

sed -i \
  -e 's/^extern struct asn_TYPE_descriptor_s asn_DEF_Period;/\/\/ extern struct asn_TYPE_descriptor_s asn_DEF_Period;/' \
  -e 's/^extern struct asn_TYPE_descriptor_s asn_DEF_AggregatedSingleTariffClassSession;/\/\/ extern struct asn_TYPE_descriptor_s asn_DEF_AggregatedSingleTariffClassSession;/' \
  -e 's/^extern struct asn_TYPE_descriptor_s asn_DEF_DetectedChargeObject;/\/\/ extern struct asn_TYPE_descriptor_s asn_DEF_DetectedChargeObject;/' \
  -e 's/^&asn_DEF_Period,/\/\/ \&asn_DEF_Period,/' \
  -e 's/^&asn_DEF_AggregatedSingleTariffClassSession,/\/\/ \&asn_DEF_AggregatedSingleTariffClassSession,/' \
  -e 's/^&asn_DEF_DetectedChargeObject,/\/\/ \&asn_DEF_DetectedChargeObject,/' \
  "$pdu_path"

echo "Windows C source preparation complete."
