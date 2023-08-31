package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.{Comment, CommentsRepository}
import com.softwaremill.realworld.articles.core.api.ArticleCreateData
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions.AlreadyInUse
import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.users.UsersRepository
import zio.test.*
import zio.test.Assertion.*
import zio.{Cause, ZIO}

import java.sql.SQLException
import java.time.Instant

object ArticleRepositoryTestSupport:

  def callListArticles(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, List[Article]] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.list(filters, pagination, viewerIdOpt)
    } yield result

  def callListArticlesByFollowedUsers(
      pagination: Pagination,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, List[Article]] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.listArticlesByFollowedUsers(pagination, viewerId)
    } yield result

  def callFindBySlug(slug: ArticleSlug, viewerId: Int): ZIO[ArticlesRepository, SQLException, Option[Article]] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findBySlug(slug, viewerId)
    } yield result

  def callFindArticleIdBySlug(slug: ArticleSlug): ZIO[ArticlesRepository, Throwable, Option[Int]] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findArticleIdBySlug(slug)
    } yield result

  def findArticleAndAuthorIdsBySlug(slug: ArticleSlug): ZIO[ArticlesRepository, SQLException, Option[(Int, Int)]] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.findArticleAndAuthorIdsBySlug(slug)
    } yield result

  def callCreateArticle(articleCreateData: ArticleCreateData, userId: Int): ZIO[ArticlesRepository, Throwable, Unit] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.addArticle(articleCreateData, userId)
    } yield result

  def callUpdateArticle(articleUpdateData: Article, articleId: Int): ZIO[ArticlesRepository, Throwable, Unit] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.updateById(articleUpdateData, articleId)
    } yield result

  def callDeleteArticle(articleId: Int): ZIO[ArticlesRepository, Throwable, Long] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      result <- repo.deleteArticle(articleId)
    } yield result

  //
  def callMakeFavorite(articleId: Int, viewerId: Int): ZIO[ArticlesRepository, Throwable, Unit] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      _ <- repo.makeFavorite(articleId, viewerId)
    } yield ()

  //
  def callRemoveFavorite(articleId: Int, viewerId: Int): ZIO[ArticlesRepository, Throwable, Unit] =
    for {
      repo <- ZIO.service[ArticlesRepository]
      _ <- repo.removeFavorite(articleId, viewerId)
    } yield ()

  def callFindUserIdByEmail(email: String): ZIO[UsersRepository, Exception, Option[Int]] =
    for {
      repo <- ZIO.service[UsersRepository]
      userIdOpt <- repo.findUserIdByEmail(email)
    } yield userIdOpt

  def callFindComments(articleId: Int, viewerIdOpt: Option[Int]): ZIO[CommentsRepository, Throwable, List[Comment]] =
    for {
      repo <- ZIO.service[CommentsRepository]
      result <- repo.findComments(articleId, viewerIdOpt)
    } yield result

  def callFindTags(): ZIO[TagsRepository, Throwable, List[String]] =
    for {
      repo <- ZIO.service[TagsRepository]
      result <- repo.listTags
    } yield result

  def checkIfArticleListIsEmpty(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticles(filters, pagination, viewerIdOpt))(isEmpty)

  def checkIfArticleFeedListIsEmpty(
      pagination: Pagination,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticlesByFollowedUsers(pagination, viewerId))(isEmpty)

  def checkArticleIdNotFoundBySlug(
      slug: ArticleSlug
  ): ZIO[ArticlesRepository, Throwable, TestResult] =
    assertZIO(callFindArticleIdBySlug(slug))(isNone)

  def checkArticleAndAuthorIdNotFoundBySlug(
      slug: ArticleSlug
  ): ZIO[ArticlesRepository, Throwable, TestResult] =
    assertZIO(findArticleAndAuthorIdsBySlug(slug))(isNone)

  def checkArticleNotFound(
      slug: ArticleSlug,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callFindBySlug(slug, viewerId))(isNone)

  def checkIfArticleAlreadyExistsInCreate(
      articleCreateData: ArticleCreateData,
      userEmail: String
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    assertZIO((for {
      userId <- callFindUserIdByEmail(userEmail).someOrFail(s"User $userEmail doesn't exist")
      _ <- callCreateArticle(articleCreateData, userId)
    } yield ()).exit)(
      failsCause(
        containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
      )
    )

  def checkIfArticleAlreadyExistsInUpdate(
      existingSlug: ArticleSlug,
      updatedSlug: ArticleSlug,
      updatedTitle: String,
      updatedDescription: String,
      updatedBody: String,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    assertZIO((for {
      articleId <- callFindArticleIdBySlug(existingSlug).someOrFail(s"Article $existingSlug doesn't exist")
      articleToUpdate = Article(
        slug = updatedSlug,
        title = updatedTitle,
        description = updatedDescription,
        body = updatedBody,
        tagList = Nil,
        createdAt = null,
        updatedAt = Instant.now(),
        favorited = false,
        favoritesCount = 0,
        author = null
      )
      _ <- callUpdateArticle(articleToUpdate, articleId)
      article <- callFindBySlug(articleToUpdate.slug, viewerId)
    } yield article).exit)(
      failsCause(
        containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
      )
    )

  def checkIfRollbackWorksCorrectlyInAddArticle(
      slug: ArticleSlug,
      articleCreateData: ArticleCreateData,
      userEmail: String,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    for {
      userId <- callFindUserIdByEmail(userEmail).someOrFail(s"User $userEmail doesn't exist")
      articleOrError <- callCreateArticle(articleCreateData, userId).either
      article <- callFindBySlug(slug, viewerId)
    } yield {
      zio.test.assert(articleOrError)(
        isLeft(
          hasField(
            "getMessage",
            _.getMessage,
            equalTo("[SQLITE_CONSTRAINT_NOTNULL] A NOT NULL constraint failed (NOT NULL constraint failed: tags_articles.tag)")
          )
        )
      ) &&
      zio.test.assert(article)(isNone)
    }

  def listArticlesWithSmallPagination(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticles(filters, pagination, viewerIdOpt))(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
    )

  def listArticlesWithBigPagination(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticles(filters, pagination, viewerIdOpt))(
      hasSize(equalTo(3)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        ) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        ) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-3")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 3"))
            && hasField("description", _.description, equalTo("The tagless one"))
            && hasField("body", _.body, equalTo("Its not a dragon"))
            && hasField("tagList", _.tagList, equalTo(List()))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("john")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
    )

  def listFeedAvailableArticlesWithSmallPagination(
      pagination: Pagination,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticlesByFollowedUsers(pagination, viewerId))(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isTrue)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isTrue
              )
            )
        )
    )

  def listFeedAvailableArticlesWithBigPagination(
      pagination: Pagination,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticlesByFollowedUsers(pagination, viewerId))(
      hasSize(equalTo(3)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isTrue)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isTrue
              )
            )
        ) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isTrue)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isTrue
              )
            )
        ) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-5")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 5"))
            && hasField("description", _.description, equalTo("The tagfull one"))
            && hasField("body", _.body, equalTo("Its a blue dragon"))
            && hasField("tagList", _.tagList, equalTo(List()))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("bill")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isTrue
              )
            )
        )
    )

  def listArticlesWithTagFilter(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticles(filters, pagination, viewerIdOpt))(
      hasSize(equalTo(2)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        ) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
    )

  def listArticlesWithFavoritedTagFilter(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticles(filters, pagination, viewerIdOpt))(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon"))
            && hasField("description", _.description, equalTo("Ever wonder how?"))
            && hasField("body", _.body, equalTo("It takes a Jacobian"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(2))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
    )

  def listArticlesWithAuthorFilter(
      filters: ArticlesFilters,
      pagination: Pagination,
      viewerIdOpt: Option[Int]
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callListArticles(filters, pagination, viewerIdOpt))(
      hasSize(equalTo(1)) &&
        exists(
          (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-3")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 3"))
            && hasField("description", _.description, equalTo("The tagless one"))
            && hasField("body", _.body, equalTo("Its not a dragon"))
            && hasField("tagList", _.tagList, equalTo(List()))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("john")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
    )

  def checkArticleIdFoundBySlug(
      slug: ArticleSlug
  ): ZIO[ArticlesRepository, Throwable, TestResult] =
    assertZIO(callFindArticleIdBySlug(slug))(isSome(equalTo(1)))

  def checkArticleAndAuthorIdFoundBySlug(
      slug: ArticleSlug
  ): ZIO[ArticlesRepository, Throwable, TestResult] =
    for {
      articleAndAuthorOpt <- findArticleAndAuthorIdsBySlug(slug)
    } yield {
      zio.test.assert(articleAndAuthorOpt.map(_._1))(isSome(equalTo(1))) &&
      zio.test.assert(articleAndAuthorOpt.map(_._2))(isSome(equalTo(1)))
    }

  def findArticleBySlug(
      slug: ArticleSlug,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callFindBySlug(slug, viewerId))(
      isSome(
        (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
          && hasField("title", _.title, equalTo("How to train your dragon"))
          && hasField("description", _.description, equalTo("Ever wonder how?"))
          && hasField("body", _.body, equalTo("It takes a Jacobian"))
          && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
          && hasField("favorited", _.favorited, isTrue)
          && hasField("favoritesCount", _.favoritesCount, equalTo(2))
          && hasField(
            "author",
            _.author,
            (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
              "following",
              _.following,
              isTrue
            )
          )
      )
    )

  def findBySlugAsSeenBy(
      slug: ArticleSlug,
      viewerId: Int
  ): ZIO[ArticlesRepository, SQLException, TestResult] =
    assertZIO(callFindBySlug(slug, viewerId))(
      isSome(
        (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
          && hasField("title", _.title, equalTo("How to train your dragon 2"))
          && hasField("description", _.description, equalTo("So toothless"))
          && hasField("body", _.body, equalTo("Its a dragon"))
          && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
          && hasField("favorited", _.favorited, isTrue)
          && hasField("favoritesCount", _.favoritesCount, equalTo(1))
          && hasField(
            "author",
            _.author,
            (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
              "following",
              _.following,
              isTrue
            )
          )
      )
    )

  def createAndCheckArticle(
      slug: ArticleSlug,
      articleCreateData: ArticleCreateData,
      userEmail: String,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    for {
      userId <- callFindUserIdByEmail(userEmail).someOrFail(s"User $userEmail doesn't exist")
      _ <- callCreateArticle(articleCreateData, userId)
      article <- callFindBySlug(slug, viewerId)
    } yield zio.test.assert(article) {
      isSome(
        (hasField("slug", _.slug.value, equalTo("new-article-under-test")): Assertion[Article])
          && hasField("title", _.title, equalTo("New-article-under-test"))
          && hasField("description", _.description, equalTo("What a nice day!"))
          && hasField("body", _.body, equalTo("Writing scala code is quite challenging pleasure"))
          && hasField("tagList", _.tagList, equalTo(Nil))
          && hasField("favorited", _.favorited, isFalse)
          && hasField("favoritesCount", _.favoritesCount, equalTo(0))
          && hasField(
            "author",
            _.author,
            (hasField("username", _.username.value, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
              "following",
              _.following,
              isFalse
            )
          )
      )
    }

  def updateAndCheckArticle(
      existingSlug: ArticleSlug,
      updatedSlug: ArticleSlug,
      updatedTitle: String,
      updatedDescription: String,
      updatedBody: String,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    for {
      articleId <- callFindArticleIdBySlug(existingSlug).someOrFail(s"Article $existingSlug doesn't exist")
      articleUpdateData = Article(
        slug = updatedSlug,
        title = updatedTitle,
        description = updatedDescription,
        body = updatedBody,
        tagList = Nil,
        createdAt = Instant.now().minusSeconds(60),
        updatedAt = Instant.now(),
        favorited = false,
        favoritesCount = 0,
        author = null
      )
      _ <- callUpdateArticle(articleUpdateData, articleId)
      article <- callFindBySlug(articleUpdateData.slug, viewerId)
    } yield zio.test.assert(article) {
      isSome(
        (hasField("slug", _.slug.value, equalTo("updated-article-under-test")): Assertion[Article])
          && hasField("title", _.title, equalTo("Updated article under test"))
          && hasField("description", _.description, equalTo("What a nice updated day!"))
          && hasField("body", _.body, equalTo("Updating scala code is quite challenging pleasure"))
      )
    }

  def deleteArticle(
      slug: ArticleSlug,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository & CommentsRepository & TagsRepository, Object, TestResult] =
    for {
      articleId <- callFindArticleIdBySlug(slug).someOrFail(s"Article $slug doesn't exist")
      commentsBefore <- callFindComments(articleId, Some(viewerId))
      tagsBefore <- callFindTags()
      articleBefore <- callFindBySlug(slug, viewerId)

      _ <- callDeleteArticle(articleId)

      commentsAfter <- callFindComments(articleId, Some(viewerId))
      tagsAfter <- callFindTags()
      articleAfter <- callFindBySlug(slug, viewerId)
    } yield {
      zio.test.assert(commentsBefore)(hasSize(equalTo(3))) &&
      zio.test.assert(tagsBefore)(hasSize(equalTo(2))) &&
      zio.test.assert(articleBefore)(isSome) &&
      zio.test.assert(commentsAfter)(hasSize(equalTo(0))) &&
      zio.test.assert(tagsAfter)(hasSize(equalTo(0))) &&
      zio.test.assert(articleAfter)(isNone)
    }

  def checkMarkFavorite(
      slug: ArticleSlug,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    for {
      articleId <- callFindArticleIdBySlug(slug).someOrFail(s"Article $slug doesn't exist")
      _ <- callMakeFavorite(articleId, viewerId)
      articleOpt <- callFindBySlug(slug, viewerId)
    } yield zio.test.assert(articleOpt) {
      isSome(
        (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
          && hasField("favorited", _.favorited, isTrue)
      )
    }

  def checkRemoveFavorite(
      slug: ArticleSlug,
      viewerId: Int
  ): ZIO[ArticlesRepository & UsersRepository, Object, TestResult] =
    for {
      articleId <- callFindArticleIdBySlug(slug).someOrFail(s"Article $slug doesn't exist")
      _ <- callRemoveFavorite(articleId, viewerId)
      articleOpt <- callFindBySlug(slug, viewerId)
    } yield zio.test.assert(articleOpt) {
      isSome(
        (hasField("slug", _.slug.value, equalTo("how-to-train-your-dragon")): Assertion[Article])
          && hasField("favorited", _.favorited, isFalse)
      )
    }
