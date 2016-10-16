FROM neo4j:2.3.6

RUN apt-get update && apt-get install cron python -y

RUN wget "s3.amazonaws.com/aws-cli/awscli-bundle.zip" -O "awscli-bundle.zip" && \
    unzip awscli-bundle.zip && \
    ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws && \
    rm awscli-bundle.zip && \
    rm -rf awscli-bundle

COPY extension /neo4j-extension

ENV EXTENSION_SCRIPT=/neo4j-extension/init.sh

RUN chmod +x /neo4j-extension/neo4j-s3-backup.sh

RUN echo "0 5 * * * /neo4j-extension/neo4j-s3-backup.sh >> /var/log/cron.log 2>&1" > /cronbackup

RUN crontab /cronbackup
