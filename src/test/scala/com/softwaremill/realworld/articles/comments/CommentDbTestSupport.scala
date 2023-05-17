package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.*
import com.softwaremill.realworld.utils.TestUtils.{findArticleIdBySlug, findUserIdByEmail}
import zio.ZIO

object CommentDbTestSupport:

  def prepareDataForCommentsNotFound =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      _ <- articleRepo.addArticle(exampleArticle2, userId1)
    } yield ()

  def prepareDataForCommentsList =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      commentRepo <- ZIO.service[CommentsRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      _ <- userRepo.add(exampleUser4)
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      userId4 <- findUserIdByEmail(userRepo, exampleUser4.email)
      _ <- userRepo.follow(userId1, userId2)
      _ <- articleRepo.addArticle(exampleArticle3, userId2)
      articleId3 <- findArticleIdBySlug(articleRepo, exampleArticle3Slug)
      _ <- commentRepo.addComment(articleId3, userId1, "Thank you so much!")
      _ <- commentRepo.addComment(articleId3, userId4, "Great article!")
    } yield ()

  def prepareDataForCommentsRemoving =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      commentRepo <- ZIO.service[CommentsRepository]
      _ <- userRepo.add(exampleUser2)
      _ <- userRepo.add(exampleUser3)
      _ <- userRepo.add(exampleUser4)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      userId3 <- findUserIdByEmail(userRepo, exampleUser3.email)
      userId4 <- findUserIdByEmail(userRepo, exampleUser4.email)
      _ <- articleRepo.addArticle(exampleArticle3, userId4)
      _ <- articleRepo.addArticle(exampleArticle4, userId2)
      articleId3 <- findArticleIdBySlug(articleRepo, exampleArticle3Slug)
      articleId4 <- findArticleIdBySlug(articleRepo, exampleArticle4Slug)
      _ <- commentRepo.addComment(articleId3, userId3, "Thank you so much!")
      _ <- commentRepo.addComment(articleId4, userId3, "Amazing article!")
      _ <- commentRepo.addComment(articleId4, userId4, "Not bad.")
    } yield ()

  def prepareDataForCommentsCreation =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser4)
      userId4 <- findUserIdByEmail(userRepo, exampleUser4.email)
      _ <- articleRepo.addArticle(exampleArticle6, userId4)
    } yield ()
