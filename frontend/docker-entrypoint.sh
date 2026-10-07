#!/bin/sh
set -eu

items_per_page="${ITEMS_PER_PAGE:-4}"
case "$items_per_page" in
  ''|*[!0-9]*|0)
    echo "ITEMS_PER_PAGE must be a positive integer" >&2
    exit 1
    ;;
esac

if [ "$items_per_page" -gt 24 ]; then
  items_per_page=24
fi

printf 'window.APP_CONFIG = { itemsPerPage: %s };\n' "$items_per_page" \
  > /usr/share/nginx/html/config.js

exec nginx -g 'daemon off;'
