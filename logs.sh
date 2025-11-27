#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
tail -f $DIR/payara5/glassfish/domains/domain1/logs/server.log
