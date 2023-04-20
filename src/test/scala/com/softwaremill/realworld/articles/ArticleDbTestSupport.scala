package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.model.ArticleCreateData
import com.softwaremill.realworld.profiles.ProfilesRepository
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.users.{UserRegisterData, UsersRepository}
import zio.ZIO

object ArticleDbTestSupport {

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

  private val exampleUser3 = UserRegisterData(
    email = "bill@example.com",
    username = "bill",
    password = "secret password"
  )

  private val exampleUser4 = UserRegisterData(
    email = "michael@example.com",
    username = "michael",
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

  private val exampleArticle3 = ArticleCreateData(
    title = "How to train your dragon 3",
    description = "The tagless one",
    body = "Its not a dragon",
    tagList = None
  )

  private val exampleArticle4 = ArticleCreateData(
    title = "How to train your dragon 4",
    description = "So toothfull",
    body = "Its not a red dragon",
    tagList = None
  )

  private val exampleArticle5 = ArticleCreateData(
    title = "How to train your dragon 5",
    description = "The tagfull one",
    body = "Its a blue dragon",
    tagList = None
  )

  private val exampleArticle6 = ArticleCreateData(
    title = "How to train your dragon 6",
    description = "Not wonder how",
    body = "Its not a test dragon",
    tagList = None
  )

  private def prepareTags(articleRepo: ArticlesRepository, article1Id: Int, article2Id: Int) = {
    for {
      _ <- articleRepo.addTag("dragons", article1Id)
      _ <- articleRepo.addTag("training", article1Id)
      _ <- articleRepo.addTag("dragons", article2Id)
      _ <- articleRepo.addTag("goats", article2Id)
      _ <- articleRepo.addTag("training", article2Id)
    } yield ()
  }

  private def prepareFavorites(articleRepo: ArticlesRepository, article1Id: Int, article2Id: Int, user1Id: Int, user2Id: Int) = {
    for {
      _ <- articleRepo.makeFavorite(article1Id, user1Id)
      _ <- articleRepo.makeFavorite(article1Id, user2Id)
      _ <- articleRepo.makeFavorite(article2Id, user2Id)
    } yield ()
  }

  def prepareDataForListingArticles = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      profileRepo <- ZIO.service[ProfilesRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      user2 <- userRepo.findByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- profileRepo.follow(user1.userId, user2.userId)
      _ <- articleRepo.add(exampleArticle1, user1.userId)
      _ <- articleRepo.add(exampleArticle2, user1.userId)
      _ <- articleRepo.add(exampleArticle3, user2.userId)
      article1 <- articleRepo.findArticleBySlug(exampleArticle1Slug).someOrFail(s"Article $exampleArticle1Slug doesn't exist")
      article2 <- articleRepo.findArticleBySlug(exampleArticle2Slug).someOrFail(s"Article $exampleArticle2Slug doesn't exist")
      _ <- prepareTags(articleRepo, article1.articleId, article2.articleId)
      _ <- prepareFavorites(articleRepo, article1.articleId, article2.articleId, user1.userId, user2.userId)
    } yield ()
  }

  def prepareDataForFeedingArticles = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      profileRepo <- ZIO.service[ProfilesRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      _ <- userRepo.add(exampleUser3)
      _ <- userRepo.add(exampleUser4)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      user2 <- userRepo.findByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      user3 <- userRepo.findByEmail(exampleUser3.email).someOrFail(s"User with email ${exampleUser3.email} doesn't exist.")
      user4 <- userRepo.findByEmail(exampleUser4.email).someOrFail(s"User with email ${exampleUser4.email} doesn't exist.")
      _ <- profileRepo.follow(user1.userId, user2.userId)
      _ <- profileRepo.follow(user3.userId, user2.userId)
      _ <- articleRepo.add(exampleArticle1, user1.userId)
      _ <- articleRepo.add(exampleArticle2, user1.userId)
      _ <- articleRepo.add(exampleArticle3, user2.userId)
      _ <- articleRepo.add(exampleArticle4, user2.userId)
      _ <- articleRepo.add(exampleArticle5, user3.userId)
      _ <- articleRepo.add(exampleArticle6, user4.userId)
      article1 <- articleRepo.findArticleBySlug(exampleArticle1Slug).someOrFail(s"Article $exampleArticle1Slug doesn't exist")
      article2 <- articleRepo.findArticleBySlug(exampleArticle2Slug).someOrFail(s"Article $exampleArticle2Slug doesn't exist")
      _ <- prepareTags(articleRepo, article1.articleId, article2.articleId)
      _ <- prepareFavorites(articleRepo, article1.articleId, article2.articleId, user1.userId, user2.userId)
    } yield ()
  }

  def prepareDataForGettingArticle = {
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
      _ <- prepareFavorites(articleRepo, article1.articleId, article2.articleId, user1.userId, user2.userId)
    } yield ()
  }

  def prepareDataForArticleCreation = {
    for {
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
    } yield ()
  }

  def prepareDataForCreatingNameConflict = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle2, user1.userId)
    } yield ()
  }

  def prepareDataForArticleDeletion = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle1, user1.userId)
      _ <- articleRepo.add(exampleArticle2, user1.userId)
      _ <- articleRepo.add(exampleArticle3, user1.userId)
    } yield ()
  }

  def prepareDataForArticleUpdating = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle1, user1.userId)
    } yield ()
  }

  def prepareDataForUpdatingNameConflict = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle1, user1.userId)
      _ <- articleRepo.add(exampleArticle2, user1.userId)
    } yield ()
  }
}
