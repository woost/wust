package modules.db

import model.WustSchema._
import model._
import renesca._
import renesca.schema._
import renesca.graph.RelationType
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._
import common.ConfigString._
import play.api.Play.current
import renesca.graph.Label

object Database {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
    credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"), "db.neo4j.pass".configOrElse("neo4j")))
  )

  private def nodeWithUuid[NODE <: UuidNode](discourse: Discourse, uuid: String) = discourse.uuidNodes.find(_.uuid == uuid).get.asInstanceOf[NODE]

  private def relationMatcherQuery[RELATION <: SchemaAbstractRelation[_, _]](relFactory: SchemaAbstractRelationFactory[_, RELATION, _]) = {
    relFactory match {
      case r: SchemaRelationFactory[_, RELATION, _]            => s"[relation :`${ r.relationType }`]"
      case r: SchemaHyperRelationFactory[_, _, RELATION, _, _] => s"[startRelation :`${ r.startRelationType }`]->(middle :`${ r.label }`)-[endRelation :`${ r.endRelationType }`]"
    }
  }

  private def relationMatcherQueryWithUuid[RELATION <: SchemaAbstractRelation[_, _]](startUuid: String, relFactory: SchemaAbstractRelationFactory[_, RELATION, _], endUuid: String) = s"(start :`${ relFactory.startNodeFactory.label }` {uuid: {startUuid}})-${ relationMatcherQuery(relFactory) }->(end :`${ relFactory.endNodeFactory.label }` {uuid: {endUuid}})"

  def wholeDiscourseGraph: Discourse = Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))

  def labelDiscourseGraph(label: Label): Discourse = Discourse(db.queryGraph(s"match (n :`$label`) return n"))

  def nodeDiscourseGraph[START <: UuidNode, END <: UuidNode](startFactory: SchemaNodeFactory[START], startUuid: String, endFactory: SchemaNodeFactory[END], endUuid: String): Discourse = {
    val query = s"match (start :`${ startFactory.label }` {uuid: {startUuid}}), (end: `${ endFactory.label }` {uuid: {endUuid}}) return start,end limit 2"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def nodeDiscourseGraph[NODE <: UuidNode](factory: SchemaNodeFactory[NODE], uuids: String*): Discourse = {
    if(uuids.isEmpty)
      return labelDiscourseGraph(factory.label)

    val query = s"match (n :`${ factory.label }`) where n.uuid in {uuids} return n limit ${ uuids.size }"
    val params = Map("uuids" -> uuids)

    Discourse(db.queryGraph(Query(query, params)))
  }


  def discourseNodes[NODE <: UuidNode](factory: SchemaNodeFactory[NODE], uuids: String*) = {
    val discourse = nodeDiscourseGraph(factory, uuids: _*)
    if(uuids.isEmpty)
      (discourse, discourse.uuidNodes.map(_.asInstanceOf[NODE]))
    else
      (discourse, uuids.map { uuid => nodeWithUuid[NODE](discourse, uuid) })
  }

  def discourseNodes[START <: UuidNode, END <: UuidNode](startFactory: SchemaNodeFactory[START], startUuid: String, endFactory: SchemaNodeFactory[END], endUuid: String) = {
    val discourse = nodeDiscourseGraph(startFactory, startUuid, endFactory, endUuid)
    (discourse, (nodeWithUuid[START](discourse, startUuid), nodeWithUuid[END](discourse, endUuid)))
  }

  def relationDiscourseGraph(startUuid: String, relFactory: SchemaAbstractRelationFactoryNode[UuidNode], endUuid: String): Discourse = {
    val query = s"match ${ relationMatcherQueryWithUuid(startUuid, relFactory, endUuid) } return *"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def relationDiscourseGraphWithUuid[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END], END <: UuidNode, NODE <: UuidNode](startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String, nestedFactory: SchemaNodeFactory[NODE], nestedUuid: String): Discourse = {
    val query = s"match ${ relationMatcherQueryWithUuid(startUuid, relFactory, endUuid) }, (nested :`${ nestedFactory.label }` {uuid: {nestedUuid}}) return *"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid, "nestedUuid" -> nestedUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  //TODO: does not work for nested hyperrelations => collision of
  //identifiers in the query should have SchemaRelationFactory as type for
  //nestedRelFactory => affects Start/EndConnectSchema.
  def startHyperRelationDiscourseGraph[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[RELATION, NESTEDEND], NESTEDEND <: UuidNode](startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[RELATION, NESTEDRELATION, NESTEDEND], nestedEndUuid: String) = {
    val query = s"match ${ relationMatcherQueryWithUuid(startUuid, relFactory, endUuid) }, (middle)-${ relationMatcherQuery(nestedRelFactory) }->(nested :`${ nestedRelFactory.endNodeFactory.label }` {uuid: {nestedEndUuid}}) return middle, relation, nested"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid, "nestedEndUuid" -> nestedEndUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def endHyperRelationDiscourseGraph[NESTEDSTART <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[NESTEDSTART, RELATION], START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode](nestedStartUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[NESTEDSTART, NESTEDRELATION, RELATION], startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String) = {
    val query = s"match ${ relationMatcherQueryWithUuid(startUuid, relFactory, endUuid) }, (nested :`${ nestedRelFactory.startNodeFactory.label }` {uuid: {nestedStartUuid}})-${ relationMatcherQuery(nestedRelFactory) }->(middle) return middle, relation, nested"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid, "nestedStartUuid" -> nestedStartUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, _]](startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, _]): Discourse = {
    val query = s"match (start :`${ relFactory.startNodeFactory.label }` {uuid: {startUuid}})-${ relationMatcherQuery(relFactory) }->(end :`${ relFactory.endNodeFactory.label }`) return end"
    val params = Map("startUuid" -> startUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[RELATION <: SchemaAbstractRelation[_, END], END <: UuidNode](relFactory: SchemaAbstractRelationFactory[_, RELATION, END], endUuid: String): Discourse = {
    val query = s"match (start :`${ relFactory.startNodeFactory.label }`)-${ relationMatcherQuery(relFactory) }->(end :`${ relFactory.endNodeFactory.label }` {uuid: {endUuid}}) return start"
    val params = Map("endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  //TODO: should have signature with SchemaHyperRelation instead of SchemaAbstractRelation (see RequestSchema TODO)
  def hyperConnectedNodesDiscourseGraph[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[RELATION, _]](startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[RELATION, NESTEDRELATION, _]): Discourse = {
    val query = s"match ${ relationMatcherQueryWithUuid(startUuid, relFactory, endUuid) }, (middle)-${ relationMatcherQuery(nestedRelFactory) }->(nestedEnd :`${ nestedRelFactory.endNodeFactory.label }`) return nestedEnd"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def hyperConnectedNodesDiscourseGraph[NESTEDRELATION <: SchemaAbstractRelation[_, RELATION], START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode](nestedRelFactory: SchemaAbstractRelationFactory[_, NESTEDRELATION, RELATION], startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String): Discourse = {
    val query = s"match ${ relationMatcherQueryWithUuid(startUuid, relFactory, endUuid) }, (nestedStart :`${ nestedRelFactory.startNodeFactory.label }`)-${ relationMatcherQuery(nestedRelFactory) }->(middle) return nestedStart"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectNodes[START <: SchemaNode, RELATION <: SchemaAbstractRelation[START, END], END <: SchemaNode](discourse: Discourse, start: START, factory: ContentRelationFactory[START, RELATION, END], end: END): (START, END) = {
    discourse.add(factory.local(start, end))
    db.persistChanges(discourse.graph)
    (start, end)
  }

  def connectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END], END <: UuidNode](startUuid: String, factory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String): (START, END) = {
    val (discourse, (start, end)) = discourseNodes(factory.startNodeFactory, startUuid, factory.endNodeFactory, endUuid)
    connectNodes(discourse, start, factory, end)
  }

  def startHyperConnectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[RELATION, NESTEDEND], NESTEDEND <: UuidNode](startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[RELATION, NESTEDRELATION, NESTEDEND], nestedEndUuid: String) = {
    val discourse = relationDiscourseGraphWithUuid(startUuid, relFactory, endUuid, nestedRelFactory.endNodeFactory, nestedEndUuid)
    val nested = nodeWithUuid[NESTEDEND](discourse, nestedEndUuid)
    val middle = discourse.contentNodeHyperRelations.head.asInstanceOf[RELATION]
    connectNodes(discourse, middle, nestedRelFactory, nested)
  }

  def endHyperConnectNodes[NESTEDSTART <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[NESTEDSTART, RELATION], START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode](nestedStartUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[NESTEDSTART, NESTEDRELATION, RELATION], startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String) = {
    val discourse = relationDiscourseGraphWithUuid(startUuid, relFactory, endUuid, nestedRelFactory.startNodeFactory, nestedStartUuid)
    val nested = nodeWithUuid[NESTEDSTART](discourse, nestedStartUuid)
    val middle = discourse.contentNodeHyperRelations.head.asInstanceOf[RELATION]
    connectNodes(discourse, nested, nestedRelFactory, middle)
  }

  def disconnectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END], END <: UuidNode](startUuid: String, factory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String) {
    val discourse = relationDiscourseGraph(startUuid, factory, endUuid)
    discourse.graph.nodes --= discourse.contentNodeHyperRelations.map(_.node) //TODO: wrap boilerplate
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
  }

  //TODO: really needs assurance that only schemarelations are handled on the
  //rhs of a nesting
  def hyperDisconnectNodes(discourse: Discourse) = {
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
  }

  def startHyperDisconnectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[RELATION, NESTEDEND], NESTEDEND <: UuidNode](startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[RELATION, NESTEDRELATION, NESTEDEND], nestedEndUuid: String) = {
    val discourse = startHyperRelationDiscourseGraph(startUuid, relFactory, endUuid, nestedRelFactory, nestedEndUuid)
    hyperDisconnectNodes(discourse)
  }

  def endHyperDisconnectNodes[NESTEDSTART <: UuidNode, NESTEDRELATION <: SchemaAbstractRelation[NESTEDSTART, RELATION], START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaNode, END <: UuidNode](nestedStartUuid: String, nestedRelFactory: SchemaAbstractRelationFactory[NESTEDSTART, NESTEDRELATION, RELATION], startUuid: String, relFactory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String) = {
    val discourse = endHyperRelationDiscourseGraph(nestedStartUuid, nestedRelFactory, startUuid, relFactory, endUuid)
    hyperDisconnectNodes(discourse)
  }
}
