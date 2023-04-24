package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.ArticlesRepository
import com.softwaremill.realworld.profiles.ProfilesRepository
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.*
import zio.ZIO

object CommentDbTestSupport:

  def prepareDataForComments = {
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
      article3 <- articleRepo.findArticleBySlug(exampleArticle3Slug).someOrFail(s"Article $exampleArticle3Slug doesn't exist")
      article4 <- articleRepo.findArticleBySlug(exampleArticle4Slug).someOrFail(s"Article $exampleArticle4Slug doesn't exist")
      _ <- articleRepo.addComment(article3.articleId, user1.userId, "Thank you so much!")
      _ <- articleRepo.addComment(article3.articleId, user4.userId, "Great article!")
      _ <- articleRepo.addComment(article4.articleId, user3.userId, "Amazing article!")
      _ <- articleRepo.addComment(article4.articleId, user4.userId, "Not bad.")
    } yield ()
  }
