package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.*
import zio.ZIO

object CommentDbTestSupport:

  def prepareDataForCommentsNotFound = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      userId1 <- userRepo.findUserIdByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle2, userId1)
    } yield ()
  }

  def prepareDataForCommentsList = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      commentRepo <- ZIO.service[CommentsRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      _ <- userRepo.add(exampleUser4)
      userId1 <- userRepo.findUserIdByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      userId2 <- userRepo.findUserIdByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      userId4 <- userRepo.findUserIdByEmail(exampleUser4.email).someOrFail(s"User with email ${exampleUser4.email} doesn't exist.")
      _ <- userRepo.follow(userId1, userId2)
      _ <- articleRepo.add(exampleArticle3, userId2)
      articleId3 <- articleRepo.findArticleIdBySlug(exampleArticle3Slug).someOrFail(s"Article $exampleArticle3Slug doesn't exist")
      _ <- commentRepo.addComment(articleId3, userId1, "Thank you so much!")
      _ <- commentRepo.addComment(articleId3, userId4, "Great article!")
    } yield ()
  }

  def prepareDataForCommentsRemoving = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      commentRepo <- ZIO.service[CommentsRepository]
      _ <- userRepo.add(exampleUser2)
      _ <- userRepo.add(exampleUser3)
      _ <- userRepo.add(exampleUser4)
      userId2 <- userRepo.findUserIdByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      userId3 <- userRepo.findUserIdByEmail(exampleUser3.email).someOrFail(s"User with email ${exampleUser3.email} doesn't exist.")
      userId4 <- userRepo.findUserIdByEmail(exampleUser4.email).someOrFail(s"User with email ${exampleUser4.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle3, userId4)
      _ <- articleRepo.add(exampleArticle4, userId2)
      articleId3 <- articleRepo.findArticleIdBySlug(exampleArticle3Slug).someOrFail(s"Article $exampleArticle3Slug doesn't exist")
      articleId4 <- articleRepo.findArticleIdBySlug(exampleArticle4Slug).someOrFail(s"Article $exampleArticle4Slug doesn't exist")
      _ <- commentRepo.addComment(articleId3, userId3, "Thank you so much!")
      _ <- commentRepo.addComment(articleId4, userId3, "Amazing article!")
      _ <- commentRepo.addComment(articleId4, userId4, "Not bad.")
    } yield ()
  }

  def prepareDataForCommentsCreation = {
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser4)
      userId4 <- userRepo.findUserIdByEmail(exampleUser4.email).someOrFail(s"User with email ${exampleUser4.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle6, userId4)
    } yield ()
  }
