package modules.db.helpers

import model.WustSchema._
import modules.db.Database.db
import modules.db._
import common.Constants

object UserHelper {
  def getPostNotificationRecipients(postUuid: String, depth: Int = Constants.componentDepth): Seq[User] = {
    implicit val ctx = new QueryContext
    val userDef = FactoryNodeDef(User)
    val postDef = FactoryUuidNodeDef(Post, postUuid)

    val query = s"""
    match ${postDef.toPattern}

    match (${ postDef.name })-[connects:`${ Connects.startRelationType }`|`${ Connects.endRelationType }` *0..${ depth  * 2 }]->(connectable:`${ Connectable.label }`)
    match ${userDef.toPattern}-[:`${ Follows.relationType }`]->(connectable: `${ Post.label}`)
    return distinct ${userDef.name}
    """


    val discourse = Discourse(db.queryGraph(query, ctx.params))
    discourse.users
  }

  def getPostNotificationsForUser(userUuid: String, depth: Int = Constants.componentDepth): Seq[Post] = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDef(User, userUuid)
    val postDef = FactoryNodeDef(Post)

    val followsDef = RelationDef(userDef, Follows, postDef)
    val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), Tags, postDef)
    val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
    val connectsDef = FactoryNodeDef(Connects)
    val connDef = RelationDef(postDef, ConnectsStart, connectsDef)
    val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ followsDef.toPattern }

    match p=(${ postDef.name })<-[:`${ Connects.startRelationType }`|`${ Connects.endRelationType }` *0..${ depth  * 2 }]-(connectable:`${ Connectable.label }`)
    where ALL (conn in nodes(p)[1..] where not((V0)-[:`${ Follows.relationType }`]->(conn)))

    optional match (${userDef.name})-[viewed:`${Viewed.relationType}`]->(connectable: `${Post.label}`)
    with distinct ${postDef.name}, max(viewed.timestamp) as lastViewed, max(connectable.timestamp) as newest
    where lastViewed < newest

    optional match ${tagsDef.toPattern(true, false)}
    optional match ${tagClassifiesDef.toPattern(true, false)}
    optional match ${connDef.toPattern(false, true)}, ${classifiesDef.toPattern(true, false)}
    return *
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))
    discourse.posts
  }
}
