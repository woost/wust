# Wust

## Build dependencies
- JDK 8
- simple build tool (sbt)
- Neo4j 2.2.2

## Build instructions
- ```./installbuilddeps```
    this will install npm and rvm and needed dev-packages
- ```./buildproduction```
    this will run tests and build a binary containing the server with wust.

## Running
- start the neo4j server
- set the environment variables for connecting to the database:
    ```NEO4J_URL```, ```NEO4J_USER```, ```NEO4J_PASS```
    to activate the tutorial: ```UI_SURVEY_ENABLED=true```
- ```./initseed``` and select ```[6] tasks.SeedDatabase```
    this seeds the database
- ```target/universal/stage/bin/wust```
    starts the production web server on port ```9000```
