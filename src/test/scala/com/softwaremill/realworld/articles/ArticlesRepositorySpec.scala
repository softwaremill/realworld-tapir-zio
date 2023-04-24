package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.ArticleDbTestSupport.*
import com.softwaremill.realworld.articles.ArticleRepositoryTestSupport.*
import com.softwaremill.realworld.articles.ArticlesEndpoints.{*, given}
import com.softwaremill.realworld.articles.model.{ArticleAuthor, ArticleCreateData, ArticleData}
import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.profiles.ProfilesRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.exampleUser1
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.*

object ArticlesRepositorySpec extends ZIOSpecDefault:

  override def spec = suite("check list features")(
    suite("list articles")(
      suite("with empty db")(
        test("no filters") {
          checkIfArticleListIsEmpty(ArticlesFilters.empty, Pagination(20, 0))
        },
        test("with filters") {
          checkIfArticleListIsEmpty(ArticlesFilters(Some("dragon"), Some("John"), Some("Ron")), Pagination(20, 0))
        }
      ),
      suite("with populated db")(
        test("with small pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithSmallPagination(ArticlesFilters.empty, Pagination(1, 1))
          } yield result
        },
        test("with big pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithBigPagination(ArticlesFilters.empty, Pagination(20, 0))
          } yield result
        },
        test("with tag filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithTagFilter(ArticlesFilters.withTag("dragons"), Pagination(20, 0))
          } yield result
        },
        test("with favorited filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithFavoritedTagFilter(ArticlesFilters.withFavorited("jake"), Pagination(20, 0))
          } yield result
        },
        test("with author filter") {
          for {
            _ <- prepareDataForListingArticles
            result <- listArticlesWithAuthorFilter(ArticlesFilters.withAuthor("john"), Pagination(20, 0))
          } yield result
        }
      )
    ),
    suite("find article")(
      test("find by slug") {
        for {
          _ <- prepareDataForListingArticles
          result <- findArticleBySlug("how-to-train-your-dragon")
        } yield result
      },
      test("find article - check article not found") {
        for {
          _ <- prepareDataForListingArticles
          result <- checkArticleNotFound("non-existing-article-slug")
        } yield result
      },
      test("find article by slug as seen by user that marked it as favorite") {
        for {
          _ <- prepareDataForListingArticles
          result <- findBySlugAsSeenBy(slug = "how-to-train-your-dragon-2", viewerEmail = "jake@example.com")
        } yield result
      }
    ),
    suite("add and update tags")(
      test("add tag") {
        for {
          _ <- prepareDataForListingArticles
          result <- addAndCheckTag(newTag = "new-tag", articleSlug = "how-to-train-your-dragon")
        } yield result
      },
      test("add tag - check other article is untouched") {
        for {
          _ <- prepareDataForListingArticles
          result <- addTagAndCheckIfOtherArticleIsUntouched(
            newTag = "new-tag",
            articleSlugToChange = "how-to-train-your-dragon",
            articleSlugWithoutChange = "how-to-train-your-dragon-2"
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
            userEmail = exampleUser1.email
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
            updatedBody = "Updating scala code is quite challenging pleasure"
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
            updatedBody = "Updating scala code is quite challenging pleasure"
          )
        } yield result
      }
    )
  ).provide(
    ArticlesRepository.live,
    UsersRepository.live,
    ProfilesRepository.live,
    testDbLayerWithEmptyDb
  )
