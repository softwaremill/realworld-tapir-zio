package com.softwaremill.realworld.utils

import com.softwaremill.realworld.articles.core.ArticleSlug
import com.softwaremill.realworld.articles.core.api.ArticleCreateData
import com.softwaremill.realworld.users.api.UserRegisterData

object DbData:

  val exampleUser1 = UserRegisterData(
    email = "jake@example.com",
    username = "jake",
    password =
      "$argon2id$v=19$m=12,t=20,p=2$LGVt7F82NPRc6pTfwwvOQBgMrPMcW/JVamGdmKvc8fich+qrr9lF6J/TQiBGnjavunldHYhA8B01yrajDzu/Og$aImjH6G1kWWBMI0Ysn+vyNaOpVDvEBg7BU7tp7cKjIo"
  )

  val exampleUser2 = UserRegisterData(
    email = "john@example.com",
    username = "john",
    password =
      "$argon2id$v=19$m=12,t=20,p=2$LGVt7F82NPRc6pTfwwvOQBgMrPMcW/JVamGdmKvc8fich+qrr9lF6J/TQiBGnjavunldHYhA8B01yrajDzu/Og$aImjH6G1kWWBMI0Ysn+vyNaOpVDvEBg7BU7tp7cKjIo"
  )

  val exampleUser3 = UserRegisterData(
    email = "bill@example.com",
    username = "bill",
    password =
      "$argon2id$v=19$m=12,t=20,p=2$LGVt7F82NPRc6pTfwwvOQBgMrPMcW/JVamGdmKvc8fich+qrr9lF6J/TQiBGnjavunldHYhA8B01yrajDzu/Og$aImjH6G1kWWBMI0Ysn+vyNaOpVDvEBg7BU7tp7cKjIo"
  )

  val exampleUser4 = UserRegisterData(
    email = "michael@example.com",
    username = "michael",
    password =
      "$argon2id$v=19$m=12,t=20,p=2$LGVt7F82NPRc6pTfwwvOQBgMrPMcW/JVamGdmKvc8fich+qrr9lF6J/TQiBGnjavunldHYhA8B01yrajDzu/Og$aImjH6G1kWWBMI0Ysn+vyNaOpVDvEBg7BU7tp7cKjIo"
  )

  val exampleArticle1 = ArticleCreateData(
    title = "How to train your dragon",
    description = "Ever wonder how?",
    body = "It takes a Jacobian",
    tagList = Some(List("dragons", "training"))
  )

  val exampleArticle1Slug = ArticleSlug("how-to-train-your-dragon")

  val exampleArticle2 = ArticleCreateData(
    title = "How to train your dragon 2",
    description = "So toothless",
    body = "Its a dragon",
    tagList = Some(List("dragons", "goats", "training"))
  )

  val exampleArticle2Slug = ArticleSlug("how-to-train-your-dragon-2")

  val exampleArticle3 = ArticleCreateData(
    title = "How to train your dragon 3",
    description = "The tagless one",
    body = "Its not a dragon",
    tagList = None
  )

  val exampleArticle3Slug = ArticleSlug("how-to-train-your-dragon-3")

  val exampleArticle4 = ArticleCreateData(
    title = "How to train your dragon 4",
    description = "So toothfull",
    body = "Its not a red dragon",
    tagList = None
  )

  val exampleArticle4Slug = ArticleSlug("how-to-train-your-dragon-4")

  val exampleArticle5 = ArticleCreateData(
    title = "How to train your dragon 5",
    description = "The tagfull one",
    body = "Its a blue dragon",
    tagList = None
  )

  val exampleArticle5Slug = ArticleSlug("how-to-train-your-dragon-5")

  val exampleArticle6 = ArticleCreateData(
    title = "How to train your dragon 6",
    description = "Not wonder how",
    body = "Its not a test dragon",
    tagList = None
  )

  val exampleArticle6Slug = ArticleSlug("how-to-train-your-dragon-6")
