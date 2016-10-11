#!/bin/bash

backup_file="/backup.cql"
neo4j-shell -c dump > $backup_file
