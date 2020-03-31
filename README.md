## This is an unmaintained research project. Please learn from it and steal ideas.

# Wust
A hypergraph-based discussion system

You can find an in-depth description of the goals and ideas in our master theses:
- [Quality Assurance in a Structured Collaborative Discussion System, Felix Dietze (2015)](https://github.com/fdietze/notes/raw/master/felix_dietze_master_thesis_2015.pdf)
- [Modeling Open Discourse in a Structured Collaborative Discussion System, Johannes Karoff (2015)](https://github.com/fdietze/notes/raw/master/johannes_karoff_master_thesis_2015.pdf)

Other related projects can be found here: [Related Projects](https://github.com/FactGraph/FactGraph/wiki/Related-Projects)

If you have any questions, or just want to talk to us, use felix.dietze@rwth-aachen.de. We're happy to talk to you.


## Screenshots
![Screenshot of Graph View](screenshot-graph.png)
![Screenshot of Focus View](screenshot-focus.png)

## Build dependencies
- JDK 8
- simple build tool (sbt)
- Neo4j >= 2.2.2
- gem
- npm
- bower

## Build instructions
have a look into `./buildproduction` and then run it:
- `./buildproduction`
  this will run tests and build a binary containing the server with wust.

## Running
- start the neo4j server
- set the environment variables for connecting to the database:
    - `NEO4J_URL`, `NEO4J_USER`, `NEO4J_PASS`
    - to activate the tutorial: `UI_TUTORIAL_ENABLED=true`
- `./initseed` and select `[6] tasks.SeedDatabase`
    - this seeds the database
- `target/universal/stage/bin/wust`
    - starts the production web server on port `9000`

## Hacking
- `gem install compass`
- `npm install`
- `bower install`
- `npm install -g blumenkohl.js`
- start a neo4j server on `localhost:7474` with user/pw `neo4j/neo4j` or adjust using env variables `NEO4J_URL`, `NEO4J_USER`, `NEO4J_PASS`
    - don't forget to seed the database: `./initseed`
- `sbt run` and `./blumenkohl`
- open browser at http://localhost:3000
- changing source files will recompile and reload automatically

## License
wust is free software released under the [Apache License, Version 2.0][Apache]

[Apache]: http://www.apache.org/licenses/LICENSE-2.0
