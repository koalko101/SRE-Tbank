#!/usr/bin/env bash

set -Eeuo pipefail

kubectl port-forward --address 127.0.0.1 -n logging svc/victoria-logs 9428:9428