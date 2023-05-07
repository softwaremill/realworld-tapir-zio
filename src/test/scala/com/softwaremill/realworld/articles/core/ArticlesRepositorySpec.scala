package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.core.ArticleDbTestSupport.*
import com.softwaremill.realworld.articles.core.ArticleRepositoryTestSupport.*
import com.softwaremill.realworld.articles.core.ArticlesServerEndpoints.{*, given}
import com.softwaremill.realworld.articles.core.api.ArticleCreateData
import com.softwaremill.realworld.articles.core.{ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.{exampleUser1, exampleUser2}
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.*

object ArticlesRepositorySpec extends ZIOSpecDefault:

  override def spec = suite("article repository tests")(
    suite("list articles")(
      suite("with empty db")(
        test("with no filters") {
          checkIfArticleListIsEmpty(filters = ArticlesFilters.empty, pagination = Pagination(20, 0), viewerEmailOpt = None)
        },
        test("with filters") {
          checkIfArticleListIsEmpty(
            filters = ArticlesFilters(Some("dragon"), Some("John"), Some("Ron")),
            pagination = Pagination(20, 0),
            viewerEmailOpt = None
          )
        }
      ),
      suite("with populated db")(
        test("with small pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithSmallPagination(filters = ArticlesFilters.empty, pagination = Pagination(1, 1), viewerEmailOpt = None)
          } yield result
        },
        test("with big pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithBigPagination(filters = ArticlesFilters.empty, pagination = Pagination(20, 0), viewerEmailOpt = None)
          } yield result
        },
        test("with tag filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithTagFilter(
              filters = ArticlesFilters.withTag("dragons"),
              pagination = Pagination(20, 0),
              viewerEmailOpt = None
            )
          } yield result
        },
        test("with favorited filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithFavoritedTagFilter(
              filters = ArticlesFilters.withFavorited("jake"),
              pagination = Pagination(20, 0),
              viewerEmailOpt = None
            )
          } yield result
        },
        test("with author filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithAuthorFilter(
              filters = ArticlesFilters.withAuthor("john"),
              pagination = Pagination(20, 0),
              viewerEmailOpt = None
            )
          } yield result
        }
      )
    ),
    suite("find article")(
      test("find by slug") {
        for {
          _ <- prepareDataForListingArticles
          result <- findArticleBySlug(slug = "how-to-train-your-dragon", viewerEmail = exampleUser1.email)
        } yield result
      },
      test("find article - check article not found") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkArticleNotFound(slug = "non-existing-article-slug", viewerEmail = exampleUser1.email)
        } yield result
      },
      test("find article by slug as seen by user that marked it as favorite") {
        for {
          _ <- prepareDataForListingArticles
          result <- findBySlugAsSeenBy(slug = "how-to-train-your-dragon-2", viewerEmail = exampleUser2.email)
        } yield result
      }
    ),
    suite("add and update tags")(
      test("add tag") {
        for {
          _ <- prepareDataForListingArticles
          result <- addAndCheckTag(newTag = "new-tag", articleSlug = "how-to-train-your-dragon", viewerEmail = exampleUser1.email)
        } yield result
      },
      test("add tag - check other article is untouched") {
        for {
          _ <- prepareDataForListingArticles
          result <- addTagAndCheckIfOtherArticleIsUntouched(
            newTag = "new-tag",
            articleSlugToChange = "how-to-train-your-dragon",
            articleSlugWithoutChange = "how-to-train-your-dragon-2",
            viewerEmail = exampleUser1.email
          )
        } yield result
      }
    ),
    suite("create and update article")(
      test("create article") {
        for {
          _ <- prepareDataForListingArticles
          result <- createAndCheckArticle(
            slug = "new-article-under-test",
            articleCreateData = ArticleCreateData(
              title = "New-article-under-test",
              description = "What a nice day!",
              body = "Writing scala code is quite challenging pleasure",
              tagList = None
            ),
            userEmail = exampleUser1.email,
            viewerEmail = exampleUser1.email
          )
        } yield result
      },
      test("create non unique article - check article already exists") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkIfArticleAlreadyExistsInCreate(
            articleCreateData = ArticleCreateData(
              title = "How-to-train-your-dragon",
              description = "What a nice day!",
              body = "Writing scala code is quite challenging pleasure",
              tagList = None
            ),
            userEmail = exampleUser1.email
          )
        } yield result
      },
      test("update article") {
        for {
          _ <- prepareDataForListingArticles
          result <- updateAndCheckArticle(
            existingSlug = "how-to-train-your-dragon",
            updatedSlug = "updated-article-under-test",
            updatedTitle = "Updated article under test",
            updatedDescription = "What a nice updated day!",
            updatedBody = "Updating scala code is quite challenging pleasure",
            viewerEmail = exampleUser1.email
          )
        } yield result
      },
      test("update article - check article already exist") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkIfArticleAlreadyExistsInUpdate(
            existingSlug = "how-to-train-your-dragon",
            updatedSlug = "how-to-train-your-dragon-2",
            updatedTitle = "How to train your dragon 2",
            updatedDescription = "What a nice updated day!",
            updatedBody = "Updating scala code is quite challenging pleasure",
            viewerEmail = exampleUser1.email
          )
        } yield result
      }
    )
  ).provide(
    ArticlesRepository.live,
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
