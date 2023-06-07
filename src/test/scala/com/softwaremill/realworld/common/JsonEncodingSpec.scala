package com.softwaremill.realworld.common

import com.softwaremill.realworld.articles.core.api.ArticleResponse
import com.softwaremill.realworld.articles.core.{Article, ArticleAuthor, ArticleSlug}
import com.softwaremill.realworld.users.api.UserResponse
import com.softwaremill.realworld.users.domain.{Email, Username}
import com.softwaremill.realworld.users.{User, api}
import com.softwaremill.realworld.{articles, users}
import zio.json.*
import zio.test.*
import zio.test.Assertion.equalTo

import java.time.Instant

object JsonEncodingSpec extends ZIOSpecDefault {

  override def spec = suite("JSON encoding for data objects") {
    suite("user related objects")(
      test("user fields with None value are present in rendered json as null values") {
        val user: UserResponse = UserResponse(
          User(
            email = Email("email@domain.com"),
            token = None,
            username = Username("username"),
            bio = None,
            image = None
          )
        )
        assert(user.toJson)(
          equalTo("""{"user":{"email":"email@domain.com","token":null,"username":"username","bio":null,"image":null}}""")
        )
      },
      test("article fields with None value are present in rendered json as null values") {
        val article: ArticleResponse = ArticleResponse(
          Article(
            ArticleSlug("how-to-train-your-dragon-2"),
            "How to train your dragon 2",
            "So toothless",
            "Its a dragon",
            List("dragons", "goats", "training"),
            Instant.ofEpochMilli(1455765776637L),
            Instant.ofEpochMilli(1455767315824L),
            false,
            1,
            ArticleAuthor("jake", None, Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
          )
        )
        assert(article.toJson)(
          equalTo(
            """{"article":{"slug":"how-to-train-your-dragon-2","title":"How to train your dragon 2","description":"So toothless","body":"Its a dragon","tagList":["dragons","goats","training"],"createdAt":"2016-02-18T03:22:56.637Z","updatedAt":"2016-02-18T03:48:35.824Z","favorited":false,"favoritesCount":1,"author":{"username":"jake","bio":null,"image":"https://i.stack.imgur.com/xHWG8.jpg","following":false}}}"""
          )
        )
      }
    )
  }
}
