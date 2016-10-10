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
        Await.result(AuthBlob.addUsers(users), 5000 millis) foreach (_ match {
          case None => println(s"ERROR adding user in db")
          case Some(str) => println(s"success: $str")
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
    val query = s"merge (u:`${ ws.User.label }` {name: {userProps}.name})-[hasLogin:`${ws.HasLogin.relationType}`]->(l:`${ ws.LoginInfo.label }` {providerID: {loginInfoProps}.providerID, providerKey: {loginInfoProps}.providerKey}) merge (l)-[hasPassword:`${ ws.HasPassword.relationType }`]->(p:`${ ws.PasswordInfo.label }`) on match set p = {passwordInfoProps} on create set p = {passwordInfoProps} return *"
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
        "name" -> user.name
      )
    )

    val graph = ws.Auth(db.queryGraph(Query(query, params)))
    Future.successful(graph.users.headOption.map(_.name))
  }

  def addUsers(users: Seq[User])(implicit db: TaskQueryHandler): Future[Seq[Option[String]]] = {
    val userFutures = users.map(createUser)
    Future.sequence(userFutures)
  }
}
