package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.CommentsRepository
import com.softwaremill.realworld.articles.core.ArticleDbTestSupport.*
import com.softwaremill.realworld.articles.core.ArticleRepositoryTestSupport.*
import com.softwaremill.realworld.articles.core.ArticlesServerEndpoints.{*, given}
import com.softwaremill.realworld.articles.core.api.ArticleCreateData
import com.softwaremill.realworld.articles.core.{ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
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
          checkIfArticleListIsEmpty(filters = ArticlesFilters.empty, pagination = Pagination(20, 0), viewerIdOpt = None)
        },
        test("with filters") {
          checkIfArticleListIsEmpty(
            filters = ArticlesFilters(Some("dragon"), Some("John"), Some("Ron")),
            pagination = Pagination(20, 0),
            viewerIdOpt = None
          )
        }
      ),
      suite("with populated db")(
        test("with small pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithSmallPagination(filters = ArticlesFilters.empty, pagination = Pagination(1, 1), viewerIdOpt = None)
          } yield result
        },
        test("with big pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithBigPagination(filters = ArticlesFilters.empty, pagination = Pagination(20, 0), viewerIdOpt = None)
          } yield result
        },
        test("with tag filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithTagFilter(
              filters = ArticlesFilters.withTag("dragons"),
              pagination = Pagination(20, 0),
              viewerIdOpt = None
            )
          } yield result
        },
        test("with favorited filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithFavoritedTagFilter(
              filters = ArticlesFilters.withFavorited("jake"),
              pagination = Pagination(20, 0),
              viewerIdOpt = None
            )
          } yield result
        },
        test("with author filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithAuthorFilter(
              filters = ArticlesFilters.withAuthor("john"),
              pagination = Pagination(20, 0),
              viewerIdOpt = None
            )
          } yield result
        }
      )
    ),
    suite("find article id by slug")(
      test("article id not found") {
        for {
          _ <- prepareDataForFindingArticle
          result <- checkArticleIdNotFoundBySlug(ArticleSlug("not-existing-slug"))
        } yield result
      },
      test("article id found") {
        for {
          _ <- prepareDataForFindingArticle
          result <- checkArticleIdFoundBySlug(ArticleSlug("how-to-train-your-dragon"))
        } yield result
      }
    ),
    suite("find article and author id by slug")(
      test("article and author id not found") {
        for {
          _ <- prepareDataForFindingArticle
          result <- checkArticleAndAuthorIdNotFoundBySlug(ArticleSlug("not-existing-slug"))
        } yield result
      },
      test("article and author id found") {
        for {
          _ <- prepareDataForFindingArticle
          result <- checkArticleAndAuthorIdFoundBySlug(ArticleSlug("how-to-train-your-dragon"))
        } yield result
      }
    ),
    suite("find article")(
      test("find article - check article not found") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkArticleNotFound(slug = ArticleSlug("non-existing-article-slug"), viewerId = 1)
        } yield result
      },
      test("find by slug") {
        for {
          _ <- prepareDataForListingArticles
          result <- findArticleBySlug(slug = ArticleSlug("how-to-train-your-dragon"), viewerId = 2)
        } yield result
      },
      test("find article by slug as seen by user that marked it as favorite") {
        for {
          _ <- prepareDataForListingArticles
          result <- findBySlugAsSeenBy(slug = ArticleSlug("how-to-train-your-dragon-2"), viewerId = 2)
        } yield result
      }
    ),
    suite("create article")(
      test("create article with not proper tag list - check if rollback returns the previous state") {
        for {
          _ <- prepareDataForArticleCreation
          result <- checkIfRollbackWorksCorrectlyInAddArticle(
            slug = ArticleSlug("new-article-under-test"),
            articleCreateData = ArticleCreateData(
              title = "New-article-under-test",
              description = "What a nice day!",
              body = "Writing scala code is quite challenging pleasure",
              tagList = Some(List(null))
            ),
            userEmail = exampleUser1.email,
            viewerId = 1
          )
        } yield result
      },
      test("create non unique article - check article already exists") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkIfArticleAlreadyExistsInCreate(
            articleCreateData = ArticleCreateData(
              title = ArticleSlug("How-to-train-your-dragon"),
              description = "What a nice day!",
              body = "Writing scala code is quite challenging pleasure",
              tagList = None
            ),
            userEmail = exampleUser1.email
          )
        } yield result
      },
      test("positive create article") {
        for {
          _ <- prepareDataForArticleCreation
          result <- createAndCheckArticle(
            slug = ArticleSlug("new-article-under-test"),
            articleCreateData = ArticleCreateData(
              title = "New-article-under-test",
              description = "What a nice day!",
              body = "Writing scala code is quite challenging pleasure",
              tagList = None
            ),
            userEmail = exampleUser1.email,
            viewerId = 1
          )
        } yield result
      }
    ),
    suite("update article")(
      test("update article - check article already exist") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkIfArticleAlreadyExistsInUpdate(
            existingSlug = ArticleSlug("how-to-train-your-dragon"),
            updatedSlug = ArticleSlug("how-to-train-your-dragon-2"),
            updatedTitle = "How to train your dragon 2",
            updatedDescription = "What a nice updated day!",
            updatedBody = "Updating scala code is quite challenging pleasure",
            viewerId = 1
          )
        } yield result
      },
      test("positive updating article") {
        for {
          _ <- prepareDataForListingArticles
          result <- updateAndCheckArticle(
            existingSlug = ArticleSlug("how-to-train-your-dragon"),
            updatedSlug = ArticleSlug("updated-article-under-test"),
            updatedTitle = "Updated article under test",
            updatedDescription = "What a nice updated day!",
            updatedBody = "Updating scala code is quite challenging pleasure",
            viewerId = 1
          )
        } yield result
      }
    ),
    suite("delete article")(
      test("positive deleting article") {
        for {
          _ <- prepareDataForArticleDeletion
          result <- deleteArticle(
            slug = ArticleSlug("how-to-train-your-dragon"),
            viewerId = 1
          )
        } yield result
      }
    ),
    suite("check favorite")(
      test("check mark favorite") {
        for {
          _ <- prepareDataForMarkingFavorites
          result <- checkMarkFavorite(
            slug = ArticleSlug("how-to-train-your-dragon"),
            viewerId = 2
          )
        } yield result
      },
      test("check remove favorite") {
        for {
          _ <- prepareDataForRemovingFavorites
          result <- checkRemoveFavorite(
            slug = ArticleSlug("how-to-train-your-dragon"),
            viewerId = 2
          )
        } yield result
      }
    )
  ).provide(
    ArticlesRepository.live,
    UsersRepository.live,
    CommentsRepository.live,
    TagsRepository.live,
    testDbLayerWithEmptyDb
  )
