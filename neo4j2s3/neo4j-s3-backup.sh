#!/bin/sh -e

backup_file="$(date +"%Y-%m-%d-%H-%M-%S-backup.cql")"
/var/lib/neo4j/bin/neo4j-shell -host neo4j -c dump > $backup_file

aws s3 cp "$backup_file" "s3://wust-fliegenpilz/backup/$backup_file"

rm "$backup_file"
