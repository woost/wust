FROM anapsix/alpine-java

COPY ./target/universal/stage /app/stage
COPY ./public /app/public

WORKDIR /app
ENTRYPOINT stage/bin/wust
