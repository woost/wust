package modules.db

import common.ConfigString._

import model.WustSchema._
import renesca.graph.Graph
import modules.db.GraphHelper._
import modules.db.types._
import play.api.Play.current
import renesca._
import renesca.parameter._
import renesca.parameter.implicits._
import renesca.schema._

object Database {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
    credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"), "db.neo4j.pass".configOrElse("neo4j")))
  )

  private def discourseGraphWithReturn(returns: String, definitions: GraphDefinition*): Discourse = {
    if(definitions.isEmpty || returns.isEmpty)
      return Discourse.empty

    val matcher = definitions.map(_.toQuery).mkString(",")
    val query = s"match $matcher return $returns"
    val params = definitions.map(_.parameterMap).reduce(_ ++ _)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def discourseGraph(definitions: GraphDefinition*): Discourse = {
    discourseGraphWithReturn("*", definitions: _*)
  }

  def itemDiscourseGraph[NODE <: Node](definitions: GraphDefinition*): Discourse = {
    val returns = definitions.map(_.name).mkString(",")
    discourseGraphWithReturn(returns, definitions: _*)
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
    discourse.graph.nodes.clear()
    db.transaction { tx =>
      tx.persistChanges(discourse.graph)
      model.WustSchema.deleteConnectsGarbage(tx)
    }
  }

  def connectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](discourse: Discourse, start: START, factory: ContentRelationFactory[START, RELATION, END], end: END): (START, END) = {
    discourse.add(factory.mergeContentRelation(start, end))
    db.transaction(_.persistChanges(discourse.graph))
    (start, end)
  }

  def connectUuidNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: UuidRelationDefinition[START, RELATION, END] with ContentRelationDefinition[START, RELATION, END]): Option[(START, END)] = {
    import relationDefinition._
    val (discourse, (startOpt, endOpt)) = discourseNodes(startDefinition, endDefinition)
    if(startOpt.isEmpty || endOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, startOpt.get, factory, endOpt.get))
  }

  def gatherHyperNodesConnector[BASE <: Node, OTHER <: UuidNode](baseDef: HyperNodeDefinitionBase[BASE], otherDef: UuidNodeDefinition[OTHER]): (Discourse, Option[(BASE, OTHER)]) = {
    val discourse = itemDiscourseGraph(baseDef, otherDef)
    val nodeOpt = nodeWithUuid[OTHER](discourse, otherDef.uuid)
    if(nodeOpt.isEmpty)
      return (discourse, None)

    val node = nodeOpt.get
    val middleOpt = discourse.hyperRelations.filterNot(_ == node).headOption
    if(middleOpt.isEmpty)
      return (discourse, None)

    val middle = middleOpt.get.asInstanceOf[BASE]
    (discourse, Some((middle, node)))
  }

  def startConnectHyperNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: HyperAndNodeRelationDefinition[START, RELATION, END] with NodeAndUuidRelationDefinition[START, RELATION, END] with ContentRelationDefinition[START, RELATION, END]): Option[(START, END)] = {
    import relationDefinition._
    val (discourse, nodesOpt) = gatherHyperNodesConnector(startDefinition, endDefinition)
    if(nodesOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, nodesOpt.get._1, factory, nodesOpt.get._2))
  }

  def endConnectHyperNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: NodeAndHyperRelationDefinition[START, RELATION, END] with UuidAndNodeRelationDefinition[START, RELATION, END] with ContentRelationDefinition[START, RELATION, END]): Option[(START, END)] = {
    import relationDefinition._
    val (discourse, nodesOpt) = gatherHyperNodesConnector(endDefinition, startDefinition)
    if(nodesOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, nodesOpt.get._2, factory, nodesOpt.get._1))
  }

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
      discourse.graph.relations.clear()
      val failure = tx.commit.persistChanges(discourse.graph)
      failure.isEmpty
    }
  }

  val tagweight_p = 0.5
  val tagweight_u = 10
  def tagweight(up:String, down:String) = s"(($up + ${tagweight_p*tagweight_u})/($up + $down + $tagweight_u))"
  def connectedComponent(focusNode: UuidNodeDefinition[_], depth: Int = 5): Discourse = {
    // Tag weights
    // 1. Cypher does not support subqueries yet, so we have to do extra queries for the tag weights.
    // This is not very efficient, because the component is traversed in each query.
    // We also cannot union :POST and :CONNECTS to take care of both tags at the same time.
    // https://github.com/neo4j/neo4j/issues/2725
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
    // TODO: postandconnects: CONNECTABLE instead of POST or CONNECTS
    val query = s"""
      match ${ focusNode.toQuery }
      match (${ focusNode.name })-[rel:`${ Connects.startRelationType }`|`${ Connects.endRelationType }` *0..${ depth * 2 }]-(postsandconnects) where (postsandconnects:`${ Post.label }`) or (postsandconnects:`${ Connects.label }`)
      with distinct postsandconnects, rel
      optional match (tag:`${ TagLike.label }`)-[tagtocat:`${ Tags.startRelationType }`]->(cat:`${ Tags.label }`)-[cattotaggable:`${ Tags.endRelationType }`]->(postsandconnects)
      return postsandconnects,rel,tag,cat,tagtocat,cattotaggable
    """


    // query for getting the weight per post per tag:
    // from paper:
    // 2011 User-Rating based Ranking of Items from an Axiomatic Perspective
    // Dirichlet Prior Smoothing: (up + u*p) / (down + up + u)
    // p: probability for an upvote (needs to be calculated from all votes in the system, I set this to 0.5)
    // u: influence of our prior (I set this to 10)
    // IMPORTANT: we need to write the constancts as doubles to avoid integer arithmetic

    //TODO: only get the tags with votes, the rest will get default values anyways
    val tagWeightQuery = s"""
      match ${ focusNode.toQuery }
      match (${ focusNode.name })-[:`${ Connects.startRelationType }`|`${ Connects.endRelationType }` *0..${ depth * 2 }]-(postsandconnects) where (postsandconnects:`${ Post.label }`) or (postsandconnects:`${ Connects.label }`)
      with distinct postsandconnects
      match (tag:`${ TagLike.label }`)-[:`${ Tags.startRelationType }`]->(tags:`${ Tags.label }`)-[:`${ Tags.endRelationType }`]->(postsandconnects)
      optional match (tag)-[:`${ Dimensionizes.startRelationType }`]->(dim:`${ Dimensionizes.label }`)-[:`${ Dimensionizes.endRelationType }`]->(postsandconnects)
      optional match (dim)<-[nodetagvoteup:${ Votes.relationType }]-() where nodetagvoteup.weight = 1
      optional match (dim)<-[nodetagvotedown:${ Votes.relationType }]-() where nodetagvotedown.weight = -1
      with tags, count(nodetagvoteup.weight) as up, count(nodetagvotedown.weight) as down
      return tags.uuid, ${tagweight("up","down")} as weight
    """

    val params = focusNode.parameterMap
    implicit val componentRawGraph = db.queryGraph(Query(query, params))
    val component = Discourse(componentRawGraph)
    val tagweights = db.queryTable(Query(tagWeightQuery, params))

    // build hashmap weights: Map tags-hyperrelation.uuid -> weight
    val weights = tagweights.rows.map { tagweight =>
      tagweight("tags.uuid").asString -> tagweight("weight").asInstanceOf[DoublePropertyValue]
    }.toMap

    // write weights in to tags-hyperrelations
    for(tags <- component.tags) {
      tags.rawItem.properties("weight") = weights.getOrElse(tags.uuid, tagweight_p)
    }

    component
  }
}
