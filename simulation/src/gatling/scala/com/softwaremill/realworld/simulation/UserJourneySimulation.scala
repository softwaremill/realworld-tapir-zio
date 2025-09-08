package com.softwaremill.realworld.simulation

import io.gatling.core.Predef.*
import io.gatling.http.Predef.*
import io.gatling.http.protocol.HttpProtocolBuilder
import net.datafaker.Faker

class UserJourneySimulation extends Simulation {

  private val faker = new Faker()

  private val userFeeder: Iterator[Map[String, Any]] = Iterator.continually {
    Map(
      "user" -> Map(
        "email" -> faker.internet().emailAddress(),
        "password" -> faker.internet().password(),
        "username" -> faker.internet().username()
      ),
      "update-user" -> Map(
        "email" -> faker.internet().emailAddress()
      )
    )
  }

  private val articlesFeeder: Iterator[Map[String, Any]] = Iterator.continually {
    Map(
      "article" -> Map(
        "title" -> faker.text().text(),
        "description" -> faker.yoda().quote(),
        "body" -> faker.text().text(),
        "tagList" -> List
          .fill(faker.number().numberBetween(1, 5))(s"\"${faker.text().text(20, 30)}\"")
          .mkString("[", ",", "]")
      )
    )
  }

  private val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://localhost:8080/api")

  private val registerUser = http("Register User")
    .post("/users")
    .body(StringBody("""
        |{
        |  "user": {
        |    "email":"#{user.email}",
        |    "password":"#{user.password}",
        |    "username":"#{user.username}"
        |  }
        |}
        |""".stripMargin))
    .asJson
    .check(status.is(200))

  private val loginUser = http("Login User")
    .post("/users/login")
    .body(StringBody("""
        |{
        |  "user":{
        |    "email":"#{user.email}",
        |    "password":"#{user.password}"
        |  }
        |}
        |""".stripMargin))
    .asJson
    .check(status.is(200))
    .check(jsonPath("$.user.token").saveAs("authToken"))

  private val currentUser = http("Current User")
    .get("/user")
    .header("Authorization", "Bearer #{authToken}")
    .check(status.is(200))

  private val updateUser = http("Update User")
    .put("/user")
    .header("Authorization", "Bearer #{authToken}")
    .body(StringBody("""
        |{
        |  "user":{
        |    "email":"#{update-user.email}"
        |  }
        |}
        |""".stripMargin))
    .check(status.is(200))

  private val loginUserWithNewEmail = http("Login User With New Email")
    .post("/users/login")
    .body(StringBody(
      """
        |{
        |  "user":{
        |    "email":"#{update-user.email}",
        |    "password":"#{user.password}"
        |  }
        |}
        |""".stripMargin))
    .asJson
    .check(status.is(200))
    .check(jsonPath("$.user.token").saveAs("authToken"))

  private val getAllArticles = http(s"Get All Articles")
    .get("/articles")
    .check(status.is(200))

  private val getArticlesByAuthor = http(s"Get All Articles by Author")
    .get("/articles")
    .queryParam("author", "non_existing_author": Any)
    .check(status.is(200))

  private val getArticlesFavoritedByUser = http(s"Articles Favorited by Username")
    .get("/articles")
    .queryParam("favorited", "non_existing_user": Any)
    .check(status.is(200))

  private val getArticlesByTag = http(s"Articles by Tag")
    .get("/articles")
    .queryParam("tag", "non_existing_tag": Any)
    .check(status.is(200))

  private val createArticle = repeat(faker.number().numberBetween(2, 10))(
    feed(articlesFeeder)
      .exec(
        http("Create Article")
          .post("/articles")
          .header("Authorization", "Bearer #{authToken}")
          .body(StringBody("""
              |{
              |  "article":{
              |    "title":"#{article.title}",
              |    "description":"#{article.description}",
              |    "body":"#{article.body}",
              |    "tagList":#{article.tagList}
              |  }
              |}
              |""".stripMargin))
      )
  )

  private val getFeed = http(s"Get Feed")
    .get("/articles/feed")
    .header("Authorization", "Bearer #{authToken}")
    .check(status.is(200))

  private val getAllArticlesFromUser = http(s"Get All Articles From User")
    .get("/articles")
    .header("Authorization", "Bearer #{authToken}")
    .check(status.is(200))

  private val endToEndScenario = scenario("End To End Scenario")
    .feed(userFeeder)
    .exec(registerUser)
    .exec(loginUser)
    .exec(currentUser)
    .exec(updateUser)
    .exec(loginUserWithNewEmail)
    .exec(getAllArticles)
    .exec(getArticlesByAuthor)
    .exec(getArticlesFavoritedByUser)
    .exec(getArticlesByTag)
    .exec(createArticle)
    .exec(getFeed)
    .exec(getAllArticlesFromUser)

  private val _ = setUp(endToEndScenario.inject(atOnceUsers(200)))
    .protocols(httpProtocol)

}
