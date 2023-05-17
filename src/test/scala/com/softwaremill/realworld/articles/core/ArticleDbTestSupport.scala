package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.CommentsRepository
import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.*
import com.softwaremill.realworld.utils.TestUtils.{findArticleIdBySlug, findUserIdByEmail}
import zio.ZIO

object ArticleDbTestSupport:

  def prepareDataForListingArticles =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      _ <- userRepo.follow(userId1, userId2)
      _ <- userRepo.follow(userId2, userId1)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
      _ <- articleRepo.addArticle(exampleArticle2, userId1)
      _ <- articleRepo.addArticle(exampleArticle3, userId2)
      articleId1 <- findArticleIdBySlug(articleRepo, exampleArticle1Slug)
      articleId2 <- findArticleIdBySlug(articleRepo, exampleArticle2Slug)
      _ <- prepareFavorites(articleRepo, articleId1, articleId2, userId1, userId2)
    } yield ()

  def prepareDataForFeedingArticles =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      _ <- userRepo.add(exampleUser3)
      _ <- userRepo.add(exampleUser4)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      userId3 <- findUserIdByEmail(userRepo, exampleUser3.email)
      userId4 <- findUserIdByEmail(userRepo, exampleUser4.email)
      _ <- userRepo.follow(userId1, userId2)
      _ <- userRepo.follow(userId3, userId2)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
      _ <- articleRepo.addArticle(exampleArticle2, userId1)
      _ <- articleRepo.addArticle(exampleArticle3, userId2)
      _ <- articleRepo.addArticle(exampleArticle4, userId2)
      _ <- articleRepo.addArticle(exampleArticle5, userId3)
      _ <- articleRepo.addArticle(exampleArticle6, userId4)
      articleId1 <- findArticleIdBySlug(articleRepo, exampleArticle1Slug)
      articleId2 <- findArticleIdBySlug(articleRepo, exampleArticle2Slug)
      _ <- prepareFavorites(articleRepo, articleId1, articleId2, userId1, userId2)
    } yield ()

  def prepareDataForGettingArticle =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
      _ <- articleRepo.addArticle(exampleArticle2, userId2)
      articleId1 <- findArticleIdBySlug(articleRepo, exampleArticle1Slug)
      articleId2 <- findArticleIdBySlug(articleRepo, exampleArticle2Slug)
      _ <- prepareFavorites(articleRepo, articleId1, articleId2, userId1, userId2)
    } yield ()

  def prepareDataForArticleCreation =
    ZIO.service[UsersRepository].flatMap(_.add(exampleUser1))

  def prepareDataForListingEmptyList =
    ZIO.service[UsersRepository].flatMap(_.add(exampleUser1))

  def prepareDataForCreatingNameConflict =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      _ <- articleRepo.addArticle(exampleArticle2, userId1)
    } yield ()

  def prepareDataForArticleDeletion =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      commentRepo <- ZIO.service[CommentsRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
      _ <- articleRepo.addArticle(exampleArticle3, userId2)
      articleId1 <- findArticleIdBySlug(articleRepo, exampleArticle1Slug)
      _ <- commentRepo.addComment(articleId1, userId2, "Thank you so much!")
      _ <- commentRepo.addComment(articleId1, userId2, "Amazing article!")
      _ <- commentRepo.addComment(articleId1, userId2, "Not bad.")
    } yield ()

  def prepareDataForArticleUpdating =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
    } yield ()

  def prepareDataForFindingArticle =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
    } yield ()

  def prepareDataForUpdatingNameConflict =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
      _ <- articleRepo.addArticle(exampleArticle2, userId1)
    } yield ()

  def prepareDataForMarkingFavorites =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
    } yield ()

  def prepareDataForRemovingFavorites =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      _ <- articleRepo.addArticle(exampleArticle1, userId1)
      articleId1 <- findArticleIdBySlug(articleRepo, exampleArticle1Slug)
      _ <- articleRepo.makeFavorite(articleId1, userId2)
    } yield ()

  private def prepareFavorites(articleRepo: ArticlesRepository, article1Id: Int, article2Id: Int, user1Id: Int, user2Id: Int) =
    for {
      _ <- articleRepo.makeFavorite(article1Id, user1Id)
      _ <- articleRepo.makeFavorite(article1Id, user2Id)
      _ <- articleRepo.makeFavorite(article2Id, user2Id)
    } yield ()
