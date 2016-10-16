#!/bin/bash -e

source /envscript

echo "initiating neo4j backup to s3"

backup_file="$(date +"%Y-%m-%d-%H-%M-%S-backup.cql")"
/var/lib/neo4j/bin/neo4j-shell -host localhost -c dump  > $backup_file
echo "dumped backup to '$backup_file'"

# TODO: use NEO4J_BACKUP_S3_BUCKET var, but not there?
s3_addr="s3://$NEO4J_BACKUP_S3_BUCKET/backup/$backup_file"
/usr/local/bin/aws s3 cp "$backup_file" "$s3_addr"
echo "uploaded to backup s3 bucket '$s3_addr'"

rm "$backup_file"
