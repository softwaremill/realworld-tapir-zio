package com.softwaremill.realworld.articles.core

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.core.api.ArticleCreateData
import com.softwaremill.realworld.articles.core.{Article, ArticleAuthor, ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.common.Exceptions.AlreadyInUse
import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.users.{Profile, User, UsersRepository}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.*
import zio.test.Assertion.*
import zio.{Cause, RIO, Random, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant

object ArticleRepositoryTestSupport:

  def callListArticles(filters: ArticlesFilters, pagination: Pagination): ZIO[ArticlesRepository, SQLException, List[Article]] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.list(filters, pagination)
    } yield result
  }

  def callFindBySlug(slug: String): ZIO[ArticlesRepository, SQLException, Option[Article]] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findBySlug(slug)
    } yield result
  }

  def callFindBySlugAsSeenBy(slug: String, viewerEmail: String): ZIO[ArticlesRepository, SQLException, Option[Article]] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findBySlugAsSeenBy(slug, viewerEmail)
    } yield result
  }

  def callFindArticleIdBySlug(slug: String): ZIO[ArticlesRepository, Throwable, Option[Int]] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findArticleIdBySlug(slug)
    } yield result
  }

  def callAddTag(newtTag: String, articleId: Int): ZIO[ArticlesRepository, Exception, Unit] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.addTag(newtTag, articleId)
    } yield result
  }

  def callCreateArticle(articleCreateData: ArticleCreateData, userId: Int): ZIO[ArticlesRepository, Throwable, Int] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.add(articleCreateData, userId)
    } yield result
  }

  def callFindUserIdByEmail(email: String): ZIO[UsersRepository, Exception, Option[Int]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserIdByEmail(email)
    } yield result
  }

  def callUpdateArticle(articleUpdateData: Article, articleId: Int): ZIO[ArticlesRepository, Throwable, Unit] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.updateById(articleUpdateData, articleId)
    } yield result
  }

  def checkIfArticleListIsEmpty(filters: ArticlesFilters, pagination: Pagination): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield zio.test.assert(result)(isEmpty)
  }

  def checkArticleNotFound(
      slug: String
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {

    assertZIO(callFindBySlug(slug))(isNone)
  }

  def checkIfArticleAlreadyExistsInCreate(
      articleCreateData: ArticleCreateData,
      userEmail: String
  ): ZIO[ArticlesRepository with UsersRepository, Object, TestResult] = {

    assertZIO((for {
      userId <- callFindUserIdByEmail(userEmail).someOrFail(s"User $userEmail doesn't exist")
      articleId <- callCreateArticle(articleCreateData, userId)
    } yield articleId).exit)(
      failsCause(
        containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
      )
    )
  }

  def checkIfArticleAlreadyExistsInUpdate(
      existingSlug: String,
      updatedSlug: String,
      updatedTitle: String,
      updatedDescription: String,
      updatedBody: String
  ): ZIO[ArticlesRepository with UsersRepository, Object, TestResult] = {

    assertZIO((for {
      articleId <- callFindArticleIdBySlug(existingSlug).someOrFail(s"Article $existingSlug doesn't exist")
      articleUpdateData = Article(
        slug = updatedSlug,
        title = updatedTitle,
        description = updatedDescription,
        body = updatedBody,
        tagList = Nil,
        createdAt = null, // TODO I think more specialized class should be used for article creation
        updatedAt = Instant.now(),
        favorited = false,
        favoritesCount = 0,
        author = null
      )
      _ <- callUpdateArticle(articleUpdateData, articleId)
      article <- callFindBySlug(articleUpdateData.slug)
    } yield article).exit)(
      failsCause(
        containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
      )
    )
  }

  def listArticlesWithSmallPagination(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      articlesList <- callListArticles(filters, pagination)
    } yield zio.test.assert(articlesList)(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        )
    )
  }

  def listArticlesWithBigPagination(filters: ArticlesFilters, pagination: Pagination): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      articlesList <- callListArticles(filters, pagination)
    } yield zio.test.assert(articlesList)(
      hasSize(equalTo(3)) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        ) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        ) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-3")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 3"))
            && hasField("description", _.description, equalTo("The tagless one"))
            && hasField("body", _.body, equalTo("Its not a dragon"))
            && hasField("tagList", _.tagList, equalTo(List()))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField("author", _.author, hasField("username", _.username, equalTo("john")): Assertion[ArticleAuthor])
        )
    )
  }

  def listArticlesWithTagFilter(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      articlesList <- callListArticles(filters, pagination)
    } yield zio.test.assert(articlesList)(
      hasSize(equalTo(2)) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        ) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        )
    )
  }

  def listArticlesWithFavoritedTagFilter(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      articlesList <- callListArticles(filters, pagination)
    } yield zio.test.assert(articlesList)(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        )
    )
  }

  def listArticlesWithAuthorFilter(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      articlesList <- callListArticles(filters, pagination)
    } yield zio.test.assert(articlesList)(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-3")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 3"))
            && hasField("description", _.description, equalTo("The tagless one"))
            && hasField("body", _.body, equalTo("Its not a dragon"))
            && hasField("tagList", _.tagList, equalTo(List()))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField("author", _.author, hasField("username", _.username, equalTo("john")): Assertion[ArticleAuthor])
        )
    )
  }

  def findArticleBySlug(
      slug: String
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {

    assertZIO(
      callFindBySlug(slug)
    )(
      isSome(
        (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[Article])
          && hasField("title", _.title, equalTo("How to train your dragon"))
          && hasField("description", _.description, equalTo("Ever wonder how?"))
          && hasField("body", _.body, equalTo("It takes a Jacobian"))
          && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
          && hasField("favorited", _.favorited, isFalse)
          && hasField("favoritesCount", _.favoritesCount, equalTo(2))
          && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
      )
    )
  }

  def findBySlugAsSeenBy(
      slug: String,
      viewerEmail: String
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {

    assertZIO(
      callFindBySlugAsSeenBy(slug, viewerEmail)
    )(
      isSome(
        (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
          && hasField("title", _.title, equalTo("How to train your dragon 2"))
          && hasField("description", _.description, equalTo("So toothless"))
          && hasField("body", _.body, equalTo("Its a dragon"))
          && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
          && hasField("favorited", _.favorited, isFalse)
          && hasField("favoritesCount", _.favoritesCount, equalTo(1))
          && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
      )
    )
  }

  def addAndCheckTag(
      newTag: String,
      articleSlug: String
  ): ZIO[ArticlesRepository, Object, TestResult] = {

    for {
      articleId <- callFindArticleIdBySlug(articleSlug).someOrFail(s"Article $articleSlug doesn't exist")
      _ <- callAddTag(newTag, articleId)
      updatedArticle <- callFindBySlug(articleSlug).map(_.get.tagList)
    } yield zio.test.assert(updatedArticle)(contains(newTag))
  }

  def addTagAndCheckIfOtherArticleIsUntouched(
      newTag: String,
      articleSlugToChange: String,
      articleSlugWithoutChange: String
  ): ZIO[ArticlesRepository, Object, TestResult] = {

    for {
      articleToChangeId <- callFindArticleIdBySlug(articleSlugToChange).someOrFail(s"Article $articleSlugToChange doesn't exist")
      _ <- callAddTag(newTag, articleToChangeId)
      articleWithoutChange <- callFindBySlug(articleSlugWithoutChange)
        .someOrFail(s"Article $articleSlugWithoutChange doesn't exist")
        .map(_.tagList)
    } yield zio.test.assert(articleWithoutChange)(hasNoneOf(newTag))
  }

  def createAndCheckArticle(
      slug: String,
      articleCreateData: ArticleCreateData,
      userEmail: String
  ): ZIO[ArticlesRepository with UsersRepository, Object, TestResult] = {

    for {
      userId <- callFindUserIdByEmail(userEmail).someOrFail(s"User $userEmail doesn't exist")
      _ <- callCreateArticle(articleCreateData, userId)
      article <- callFindBySlug(slug)
    } yield zio.test.assert(article) {
      isSome(
        (hasField("slug", _.slug, equalTo("new-article-under-test")): Assertion[Article])
          && hasField("title", _.title, equalTo("New-article-under-test"))
          && hasField("description", _.description, equalTo("What a nice day!"))
          && hasField("body", _.body, equalTo("Writing scala code is quite challenging pleasure"))
          && hasField("tagList", _.tagList, equalTo(Nil))
          && hasField("favorited", _.favorited, isFalse)
          && hasField("favoritesCount", _.favoritesCount, equalTo(0))
          && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
      )
    }
  }

  def updateAndCheckArticle(
      existingSlug: String,
      updatedSlug: String,
      updatedTitle: String,
      updatedDescription: String,
      updatedBody: String
  ): ZIO[ArticlesRepository with UsersRepository, Object, TestResult] = {

    for {
      articleId <- callFindArticleIdBySlug(existingSlug).someOrFail(s"Article $existingSlug doesn't exist")
      articleUpdateData = Article(
        slug = updatedSlug,
        title = updatedTitle,
        description = updatedDescription,
        body = updatedBody,
        tagList = Nil,
        createdAt = null, // TODO I think more specialized class should be used for article creation
        updatedAt = Instant.now(),
        favorited = false,
        favoritesCount = 0,
        author = null
      )
      _ <- callUpdateArticle(articleUpdateData, articleId)
      article <- callFindBySlug(articleUpdateData.slug)
    } yield zio.test.assert(article) {
      isSome(
        (hasField("slug", _.slug, equalTo("updated-article-under-test")): Assertion[Article])
          && hasField("title", _.title, equalTo("Updated article under test"))
          && hasField("description", _.description, equalTo("What a nice updated day!"))
          && hasField("body", _.body, equalTo("Updating scala code is quite challenging pleasure"))
      )
    }
  }
