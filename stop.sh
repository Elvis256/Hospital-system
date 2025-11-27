#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
$DIR/payara5/bin/asadmin stop-domain domain1
echo "HMISD stopped"
