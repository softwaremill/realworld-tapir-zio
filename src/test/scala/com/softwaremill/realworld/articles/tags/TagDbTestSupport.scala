package com.softwaremill.realworld.articles.tags

import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.*
import zio.ZIO

object TagDbTestSupport:

  def prepareBasicTagsData =
    for {
      articleRepo <- ZIO.service[ArticlesRepository]
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- userRepo.findUserIdByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      userId2 <- userRepo.findUserIdByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- articleRepo.addArticleTransaction(exampleArticle1, userId1)
      _ <- articleRepo.addArticleTransaction(exampleArticle2, userId2)
    } yield ()
