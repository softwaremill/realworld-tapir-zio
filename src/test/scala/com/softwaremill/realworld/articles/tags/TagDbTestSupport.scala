package com.softwaremill.realworld.articles.tags

import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.*
import zio.ZIO

object TagDbTestSupport:

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
      userId1 <- userRepo.findUserIdByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      userId2 <- userRepo.findUserIdByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- articleRepo.add(exampleArticle1, userId1)
      _ <- articleRepo.add(exampleArticle2, userId2)
      articleId1 <- articleRepo.findArticleIdBySlug(exampleArticle1Slug).someOrFail(s"Article $exampleArticle1Slug doesn't exist")
      articleId2 <- articleRepo.findArticleIdBySlug(exampleArticle2Slug).someOrFail(s"Article $exampleArticle2Slug doesn't exist")
      _ <- prepareTags(articleRepo, articleId1, articleId2)
    } yield ()
  }
