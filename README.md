# Wust
A hypergraph-based discussion system

This is a proof of concept prototype. Please don't consider extending this project. We'd like to do a complete rewrite based on the experiences, survey results and new ideas we have now. If you are interested, please contact us.

## Build dependencies
- JDK 8
- simple build tool (sbt)
- Neo4j 2.2.2
- `gem install compass`
- npm
- bower

## Build instructions
have a look into `./buildproduction` and then run it:
- `./buildproduction`
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

## Screenshots
![Screenshot of Graph View](screenshot-graph.png)
![Screenshot of Focus View](screenshot-focus.png)

## License
wust is free software released under the [Apache License, Version 2.0][Apache]

[Apache]: http://www.apache.org/licenses/LICENSE-2.0
