FROM alpine

RUN wget "s3.amazonaws.com/aws-cli/awscli-bundle.zip" -O "awscli-bundle.zip" && \
    unzip awscli-bundle.zip && \
    apk add --update groff less python && \
    rm /var/cache/apk/* && \
    ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws && \
    rm awscli-bundle.zip && \
    rm -rf awscli-bundle

COPY neo4j-s3-backup.sh /bin

RUN chmod +x /bin/neo4j-s3-backup.sh

RUN echo "* * * * * /bin/neo4j-s3-backup.sh" > /cronbackup && crontab /cronbackup

CMD crond -f
