package tasks

import scala.io.Source
import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import modules.auth.HeaderEnvironmentModule
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.api.LoginInfo

case class User(name: String, password: String)

// adds users from files:
//   $ UserAdd.main file1 file2 file3...
// user/password files should be in the form:
//   - one user per line
//   - each line consists of two words (username and password) separated by a whitespace: username password
//   - empty lines and lines starting with a '#' are ignored

object UserAdd extends DbUtil {
  def main(args: Array[String]): Unit = {
    println(s"Adding users with passwords from ${args.size} files")
    val users = args.flatMap { file =>
      println(s"Reading users from file '$file'")
      parseUserFile(file) match {
        case Left(err) =>
          println(s"ERROR reading file: $err.")
          Seq.empty
        case Right(users) => users
      }
    }

    println(s"Found ${users.size} users")
    if (users.nonEmpty) {
      dbContext { implicit db =>
        println(s"Storing users in db '${db.serverName}'")
        val fut = AuthBlob.addUsers(users)
        fut.onFailure { case e =>
          println(s"failure while adding users: ${e.getMessage}")
        }
        Await.result(fut, 5000 millis) foreach ( _ match {
          case Some(str) => println(s"success: $str")
          case None => println("nothing found")
        })
      }
    }
  }

  def parseUserFile(file: String): Either[String, Seq[User]] = {
    Try(Source.fromFile(file)) match {
      case Success(src) =>
        val lines = src.getLines.zipWithIndex.filterNot { case (l,i) =>
          l.isEmpty || l.startsWith("#")
        }

        val users = lines.map { case (line, index) =>
          val splits = line.split(" ")
          if (splits.size != 2) Left(s"$index($line)")
          else Right(User(splits(0), splits(1)))
        }.toList

        src.close()
        val errors = Some(users collect { case Left(err) => err }).filter(_.nonEmpty)
        errors.map(e => "Invalid lines: " + e.mkString(",")) toLeft {
          (users collect { case Right(user) => user })
        }
      case Failure(ex) => Left("Cannot read file: " + ex.getMessage)
    }
  }
}

object AuthBlob extends HeaderEnvironmentModule {
  import renesca._
  import renesca.parameter._
  import renesca.parameter.implicits._
  import model.{WustSchema => ws}

  def createUser(user: User)(implicit db: TaskQueryHandler): Future[Option[String]] = {
    val loginInfo = LoginInfo(CredentialsProvider.ID, user.name)
    val authInfo = passwordHasher.hash(user.password)
    val query = s"""match (group ${ ws.UserGroup.labels.map(l => s":`$l`").mkString} { name: "everyone" }) with group merge (u ${ ws.User.labels.map(l => s":`$l`").mkString} {name: {userProps}.name, uuid: {userProps}.uuid})-[hasLogin:`${ws.HasLogin.relationType}`]->(l ${ ws.LoginInfo.labels.map(l => s":`$l`").mkString } {providerID: {loginInfoProps}.providerID, providerKey: {loginInfoProps}.providerKey}) merge (u)-[memberof: `${ ws.MemberOf.relationType}`]->(group) merge (l)-[hasPassword:`${ ws.HasPassword.relationType }`]->(p ${ ws.PasswordInfo.labels.map(l => s":`$l`").mkString }) on match set p = {passwordInfoProps} on create set p = {passwordInfoProps} return *"""
    println(query)

    val params = Map(
      "loginInfoProps" -> Map(
        "providerID" -> loginInfo.providerID,
        "providerKey" -> loginInfo.providerKey
      ),
      "passwordInfoProps" -> Map(
        "hasher" -> authInfo.hasher,
        "password" -> authInfo.password,
        "salt" -> authInfo.salt.getOrElse("")
      ),
      "userProps" -> Map(
        "name" -> user.name,
        "uuid" -> model.Helpers.uuidBase64
      )
    )

    Try(db.queryGraphs(Query(query, params)).headOption.map(ws.Auth(_)).getOrElse(ws.Auth.empty)) match {
      case Success(graph) => Future.successful(graph.users.headOption.map(_.name))
      case Failure(err) => Future.failed(err)
    }
  }

  def addUsers(users: Seq[User])(implicit db: TaskQueryHandler): Future[Seq[Option[String]]] = {
    val userFutures = users.map(createUser)
    Future.sequence(userFutures)
  }
}
