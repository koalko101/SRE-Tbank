#!/usr/bin/env bash

kubectl port-forward -n monitoring svc/vm-single-victoria-metrics-single-server 8428:8428