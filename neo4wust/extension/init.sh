script_dir="/neo4j-extension/init"
imported_label="__CQL_FILE_IMPORTED"

echo "initializing neo4j..."

cql_files="$(find $script_dir -maxdepth 1 -name *.cql | xargs -I{} basename {} | sort -n)"

if [ -z "$cql_files" ]; then
    echo "no cql files found. nothing to import."
else
    while read -r cql_file; do
        echo -n "found cql file '$cql_file': "
        existing="$(bin/neo4j-shell -path /data/databases/graph.db -c "match (n :$imported_label { filename: \"$cql_file\" }) return n.filename;")"
        if echo "$existing" | grep -E "^0 row$" > /dev/null; then
            echo "importing into neo4j."
            bin/neo4j-shell -path /data/databases/graph.db -file "$script_dir/$cql_file"
            bin/neo4j-shell -path /data/databases/graph.db -c "create (:$imported_label { filename: \"$cql_file\" });"
        elif echo "$existing" | grep -E "^1 row$" > /dev/null; then
            echo "already imported, is ignored."
        else
            echo "this file was imported more than once - please check this!"
            echo "initializing exited."
            exit 1
        fi
    done <<< "$cql_files"
fi

echo "initializing done."
