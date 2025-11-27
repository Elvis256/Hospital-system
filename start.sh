#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
$DIR/payara5/bin/asadmin start-domain domain1
echo "HMISD started: http://localhost:9090/rh-3.0.0"
