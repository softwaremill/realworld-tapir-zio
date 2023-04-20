package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.model.{ArticleAuthor, ArticleData}
import com.softwaremill.realworld.common.Pagination
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.*
import zio.test.Assertion.*
import zio.{Cause, RIO, Random, ZIO, ZLayer}

import java.sql.SQLException

object ArticleRepositoryTestSupport {

  def callListArticles(filters: ArticlesFilters, pagination: Pagination): ZIO[ArticlesRepository, SQLException, List[ArticleData]] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.list(filters, pagination)
    } yield result
  }

  def callFindBySlug(slug: String): ZIO[ArticlesRepository, SQLException, Option[ArticleData]] = {
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findBySlug(slug)
    } yield result
  }

  def callFindBySlugAsSeenBy(slug: String, viewerEmail: String): ZIO[ArticlesRepository, SQLException, Option[ArticleData]] = {
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

  def checkIfArticleListIsEmpty(filters: ArticlesFilters, pagination: Pagination): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield zio.test.assert(result)(isEmpty)
  }

  def listArticlesWithSmallPagination(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield assertTrue {

      result.size == 1 &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      )
    }
  }

  def listArticlesWithBigPagination(filters: ArticlesFilters, pagination: Pagination): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield assertTrue {

      result.size == 3 &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon" &&
          article.title == "How to train your dragon" &&
          article.description == "Ever wonder how?" &&
          article.body == "It takes a Jacobian" &&
          article.tagList == List("dragons", "training") &&
          !article.favorited &&
          article.favoritesCount == 2 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon-3" &&
          article.title == "How to train your dragon 3" &&
          article.description == "The tagless one" &&
          article.body == "Its not a dragon" &&
          article.tagList == List() &&
          !article.favorited &&
          article.favoritesCount == 0 &&
          article.author.username == "john" &&
          !article.author.following
      )
    }
  }

  def listArticlesWithTagFilter(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield assertTrue {

      result.size == 2 &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon" &&
          article.title == "How to train your dragon" &&
          article.description == "Ever wonder how?" &&
          article.body == "It takes a Jacobian" &&
          article.tagList == List("dragons", "training") &&
          !article.favorited &&
          article.favoritesCount == 2 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      )
    }
  }

  def listArticlesWithFavoritedTagFilter(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield assertTrue {

      result.size == 1 &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon" &&
          article.title == "How to train your dragon" &&
          article.description == "Ever wonder how?" &&
          article.body == "It takes a Jacobian" &&
          article.tagList == List("dragons", "training") &&
          !article.favorited &&
          article.favoritesCount == 2 &&
          article.author.username == "jake" &&
          !article.author.following
      )
    }
  }

  def listArticlesWithAuthorFilter(
      filters: ArticlesFilters,
      pagination: Pagination
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {
    for {
      result <- callListArticles(filters, pagination)
    } yield assertTrue {

      result.size == 1 &&
      result.exists(article =>
        article.slug == "how-to-train-your-dragon-3" &&
          article.title == "How to train your dragon 3" &&
          article.description == "The tagless one" &&
          article.body == "Its not a dragon" &&
          article.tagList == List() &&
          !article.favorited &&
          article.favoritesCount == 0 &&
          article.author.username == "john" &&
          !article.author.following
      )
    }
  }

  def findArticleBySlug(
      slug: String
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {

    assertZIO(
      callFindBySlug(slug)
    )(
      isSome(
        (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[ArticleData])
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

  def checkArticleNotFound(
      slug: String
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {

    assertZIO(callFindBySlug(slug))(isNone)
  }

  def findBySlugAsSeenBy(
      slug: String,
      viewerEmail: String
  ): ZIO[ArticlesRepository, SQLException, TestResult] = {

    assertZIO(
      callFindBySlugAsSeenBy(slug, viewerEmail)
    )(
      isSome(
        (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[ArticleData])
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
  ) = {

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
  ) = {

    for {
      articleToChangeId <- callFindArticleIdBySlug(articleSlugToChange).someOrFail(s"Article $articleSlugToChange doesn't exist")
      _ <- callAddTag(newTag, articleToChangeId)
      articleWithoutChange <- callFindBySlug(articleSlugWithoutChange)
        .someOrFail(s"Article $articleSlugWithoutChange doesn't exist")
        .map(_.tagList)
    } yield zio.test.assert(articleWithoutChange)(hasNoneOf(newTag))
  }
}
