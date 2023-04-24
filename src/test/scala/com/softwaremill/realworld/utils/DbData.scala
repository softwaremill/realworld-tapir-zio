package com.softwaremill.realworld.utils
import com.softwaremill.realworld.articles.model.ArticleCreateData
import com.softwaremill.realworld.users.UserRegisterData

object DbData {

  val exampleUser1 = UserRegisterData(
    email = "jake@example.com",
    username = "jake",
    password = "secret password"
  )

  val exampleUser2 = UserRegisterData(
    email = "john@example.com",
    username = "john",
    password = "secret password"
  )

  val exampleUser3 = UserRegisterData(
    email = "bill@example.com",
    username = "bill",
    password = "secret password"
  )

  val exampleUser4 = UserRegisterData(
    email = "michael@example.com",
    username = "michael",
    password = "secret password"
  )

  val exampleArticle1 = ArticleCreateData(
    title = "How to train your dragon",
    description = "Ever wonder how?",
    body = "It takes a Jacobian",
    tagList = None
  )

  val exampleArticle1Slug = "how-to-train-your-dragon"

  val exampleArticle2 = ArticleCreateData(
    title = "How to train your dragon 2",
    description = "So toothless",
    body = "Its a dragon",
    tagList = None
  )

  val exampleArticle2Slug = "how-to-train-your-dragon-2"

  val exampleArticle3 = ArticleCreateData(
    title = "How to train your dragon 3",
    description = "The tagless one",
    body = "Its not a dragon",
    tagList = None
  )

  val exampleArticle3Slug = "how-to-train-your-dragon-3"

  val exampleArticle4 = ArticleCreateData(
    title = "How to train your dragon 4",
    description = "So toothfull",
    body = "Its not a red dragon",
    tagList = None
  )

  val exampleArticle4Slug = "how-to-train-your-dragon-4"

  val exampleArticle5 = ArticleCreateData(
    title = "How to train your dragon 5",
    description = "The tagfull one",
    body = "Its a blue dragon",
    tagList = None
  )

  val exampleArticle5Slug = "how-to-train-your-dragon-5"

  val exampleArticle6 = ArticleCreateData(
    title = "How to train your dragon 6",
    description = "Not wonder how",
    body = "Its not a test dragon",
    tagList = None
  )

  val exampleArticle6Slug = "how-to-train-your-dragon-6"
}
