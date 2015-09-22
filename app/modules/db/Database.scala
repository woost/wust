package modules.db

import common.ConfigString._
import model.WustSchema._
import modules.db.GraphHelper._
import modules.db.types._
import play.api.Play.current
import renesca._
import renesca.graph.Graph
import renesca.parameter.implicits._
import renesca.schema._

object Database {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
    credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"), "db.neo4j.pass".configOrElse("neo4j")))
  )

  //TODO: more methods with optional tx

  private def discourseGraphWithReturn(returns: String, definitions: GraphDefinition*): Discourse = discourseGraphWithReturn(db, returns, definitions: _*)
  private def discourseGraphWithReturn(tx: QueryHandler, returns: String, definitions: GraphDefinition*): Discourse = {
    if(definitions.isEmpty || returns.isEmpty)
      return Discourse.empty

    val matcher = definitions.map(_.toQuery).mkString(",")
    val query = s"match $matcher return $returns"
    val params = definitions.map(_.parameterMap).reduce(_ ++ _)
    Discourse(tx.queryGraph(Query(query, params)))
  }

  def discourseGraph(definitions: GraphDefinition*): Discourse = discourseGraph(db, definitions: _*)
  def discourseGraph(tx: QueryHandler, definitions: GraphDefinition*): Discourse = {
    discourseGraphWithReturn(tx, "*", definitions: _*)
  }

  def itemDiscourseGraph(definitions: GraphDefinition*): Discourse = itemDiscourseGraph(db, definitions: _*)
  def itemDiscourseGraph[NODE <: Node](tx: QueryHandler, definitions: GraphDefinition*): Discourse = {
    val returns = definitions.map(_.name).mkString(",")
    discourseGraphWithReturn(tx, returns, definitions: _*)
  }

  def nodeDiscourseGraph[NODE <: UuidNode](factory: NodeFactory[NODE], uuids: String*): Discourse = {
    if(uuids.isEmpty)
      return discourseGraph(ConcreteFactoryNodeDefinition(factory))

    val query = s"match (n :`${ factory.label }`) where n.uuid in {uuids} return n limit ${ uuids.size }"
    val params = Map("uuids" -> uuids)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def limitedDiscourseNodes[NODE <: UuidNode](skip: Int, limit: Int, factory: NodeFactory[NODE]): (Discourse, Seq[NODE]) = {
    val nodeDef = ConcreteFactoryNodeDefinition(factory)
    val discourse = discourseGraphWithReturn(s"${ nodeDef.name } skip $skip limit $limit", nodeDef)
    (discourse, nodesWithType[NODE](discourse.nodes))
  }

  def discourseNodes[NODE <: UuidNode](factory: NodeFactory[NODE], uuids: String*): (Discourse, Seq[NODE]) = {
    val discourse = nodeDiscourseGraph(factory, uuids: _*)
    (discourse, findNodes(discourse, factory, uuids: _*))
  }

  def discourseNodes[START <: UuidNode, END <: UuidNode](startDefinition: UuidNodeDefinition[START], endDefinition: UuidNodeDefinition[END]): (Discourse, (Option[START], Option[END])) = {
    val discourse = itemDiscourseGraph(startDefinition, endDefinition)
    (discourse, findNodes(discourse, startDefinition, endDefinition))
  }

  def discourseNodes[NODE <: UuidNode](definitions: UuidNodeDefinition[NODE]*): (Discourse, Seq[NODE]) = {
    val discourse = itemDiscourseGraph(definitions: _*)
    (discourse, findNodes(discourse, definitions: _*))
  }

  def startConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.endDefinition.name }"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def startConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Seq[END] = {
    val discourse = startConnectedDiscourseGraph(relationDefinitions: _*)
    nodesWithType[END](discourse.nodes)
  }

  //TODO: need ordering for limit search, otherwise we get duplicates
  def limitedStartConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.endDefinition.name } skip $skip limit $limit"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def limitedStartConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Seq[END] = {
    val discourse = limitedStartConnectedDiscourseGraph(skip, limit, relationDefinitions: _*)
    nodesWithType[END](discourse.nodes)
  }

  def endConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.startDefinition.name }"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def endConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Seq[START] = {
    val discourse = endConnectedDiscourseGraph(relationDefinitions: _*)
    nodesWithType[START](discourse.nodes)
  }

  def limitedEndConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.startDefinition.name } skip $skip limit $limit"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def limitedEndConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Seq[START] = {
    val discourse = limitedEndConnectedDiscourseGraph(skip, limit, relationDefinitions: _*)
    nodesWithType[START](discourse.nodes)
  }

  def deleteNodes[NODE <: UuidNode](definitions: NodeDefinition[NODE]*) {
    val discourse = discourseGraph(definitions: _*)
    if(!discourse.graph.nodes.isEmpty) {
      //TODO clear does not emit changes
      //discourse.graph.nodes.clear()
      discourse.graph.nodes.foreach(discourse.graph.nodes -= _)
      db.transaction { tx =>
        tx.persistChanges(discourse.graph)
        model.WustSchema.deleteConnectsGarbage(tx)
      }
    }
  }

  def connectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](discourse: Discourse, start: START, factory: ConstructRelationFactory[START, RELATION, END], end: END): (START, END) = {
    discourse.add(factory.mergeConstructRelation(start, end))
    db.transaction(_.persistChanges(discourse.graph))
    (start, end)
  }

  //FIXME: I am an unreliable function if you have a relation to a hyperrelation i will delete that hyperrelation.
  def disconnectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: FixedRelationDefinition[START, RELATION, END]) = {
    val tx = db.newTransaction()
    val discourse = itemDiscourseGraph(relationDefinition)
    if(discourse.relations.isEmpty && discourse.hyperRelations.size == 1) {
      val label = discourse.hyperRelations.head.label
      discourse.graph.nodes -= discourse.hyperRelations.head.rawItem
      val failure = tx.persistChanges(discourse.graph)
      if(failure.isEmpty) {
        // here we disconnect a hyperrelation
        // therefore we garbage collect broken hyperrelations
        // in our case, only CONNECTS is recursive
        // TODO: challenge, this is a workaround
        if(label == Connects.label)
          model.WustSchema.deleteConnectsGarbage(tx)

        tx.commit()
        true
      } else {
        false
      }
    }
    else {
      discourse.graph.relations.foreach(discourse.graph.relations -= _)
      val failure = tx.commit.persistChanges(discourse.graph)
      failure.isEmpty
    }
  }

  // IMPORTANT: we need to write the constancts as doubles to avoid integer arithmetic
  // def tagweight(up:String, down:String) = s"(($up + ${tagweight_p*tagweight_u})/($up + $down + $tagweight_u))"
  def connectedComponent(focusNode: UuidNodeDefinition[_], identity: Option[User], depth: Int = 5): Discourse = {
    // Tag weights
    // 1. Cypher does not support subqueries yet, so we have to do extra queries for the tag weights.
    // This is not very efficient, because the component is traversed in each query.
    //
    // 2. As we are calculating a new value in the query (the tag weight), and cypher does not support something like
    // "implicit properties", we have to use a table.
    // In this case we are writing the results from the table into the discourse graph:
    // (post)<-[:TAGS]-(tag)
    //              ^
    //            weight
    //
    // https://groups.google.com/forum/#!topic/neo4j/s1GnCkzINYY


    // query undirected connected component of posts with maximum depth
    // depth * 2 because hyperrelation depth
    val query = s"""
match ${ focusNode.toQuery }-[connects:`${ Connects.startRelationType }`|`${ Connects.endRelationType }` *0..${ depth * 2 }]-(connectable:`${ Connectable.label }`)
with distinct connectable, connects

match (connectable) where (:`${Connectable.label}`)<-[:`${Connects.endRelationType}`]-(connectable:`${Connects.label}`)-[:`${Connects.startRelationType}`]-(:`${Post.label}`) OR (connectable:`${Post.label}`)
with connectable, connects

optional match (context:`${ Scope.label }`)-[contexttotags:`${ Tags.startRelationType }`]->(tags:`${ Tags.label }`)-[tagstopost:`${ Tags.endRelationType }`]->(connectable:`${ Post.label }`)
optional match (classification:`${ Classification.label }`)-[classifies:`${ Classifies.relationType }`]->(connectable:`${ Connects.label }`)
optional match (:`${ User.label }` {uuid: {useruuid}})-[selfanswervoted :`${ Votes.relationType }`]->(connectable:`${ Connects.label }`)
return connectable,connects,context,tags,contexttotags,tagstopost, classification, classifies, count(selfanswervoted) as selfanswervotecount
    """

    val useruuid = identity.map(_.uuid).getOrElse("") //TODO: do not write empty string into query
    val params = focusNode.parameterMap + ("useruuid" -> useruuid)

    val (graph, table) = db.queryGraphsAndTables(Query(query, params)).head
    val component = Discourse(graph)

    val uuidToNode = component.uuidNodes.map(n => (n.uuid, n)).toMap
    table.rows.foreach { row =>
      val selfanswervotecount = row("selfanswervotecount").asLong

      if(selfanswervotecount > 0) {
        val uuid = row("connectable").asMap("uuid").asString
        uuidToNode(uuid).rawItem.properties += ("selfanswervotecount" -> selfanswervotecount)
      }
    }

    component
  }
}
