package com.softwaremill.realworld.tags

import com.softwaremill.realworld.articles.ArticlesRepository
import com.softwaremill.realworld.articles.model.ArticleCreateData
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.users.{UserRegisterData, UsersRepository}
import zio.ZIO

object TagDbTestSupport:

  private val exampleUser1 = UserRegisterData(
    email = "jake@example.com",
    username = "jake",
    password = "secret password"
  )

  private val exampleUser2 = UserRegisterData(
    email = "john@example.com",
    username = "john",
    password = "secret password"
  )

  private val exampleArticle1 = ArticleCreateData(
    title = "How to train your dragon",
    description = "Ever wonder how?",
    body = "It takes a Jacobian",
    tagList = None
  )

  private val exampleArticle1Slug = "how-to-train-your-dragon"

  val exampleArticle2 = ArticleCreateData(
    title = "How to train your dragon 2",
    description = "So toothless",
    body = "Its a dragon",
    tagList = None
  )

  private val exampleArticle2Slug = "how-to-train-your-dragon-2"

  private def prepareTags(articleRepo: ArticlesRepository, article1Id: Int, article2Id: Int) = {
    for {
      _ <- articleRepo.addTag("dragons", article1Id)
      _ <- articleRepo.addTag("training", article1Id)
      _ <- articleRepo.addTag("dragons", article2Id)
      _ <- articleRepo.addTag("goats", article2Id)
      _ <- articleRepo.addTag("training", article2Id)
    } yield ()
  }

  def prepareBasicTagsData = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      user2 <- userRepo.findByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle1, user1.userId)
      _ <- articleRepo.add(exampleArticle2, user2.userId)
      article1 <- articleRepo.findArticleBySlug(exampleArticle1Slug).someOrFail(s"Article $exampleArticle1Slug doesn't exist")
      article2 <- articleRepo.findArticleBySlug(exampleArticle2Slug).someOrFail(s"Article $exampleArticle2Slug doesn't exist")
      _ <- prepareTags(articleRepo, article1.articleId, article2.articleId)
    } yield ()
  }
