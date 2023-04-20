package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.ArticleDbTestSupport.*
import com.softwaremill.realworld.articles.ArticleRepositoryTestSupport.*
import com.softwaremill.realworld.articles.ArticlesEndpoints.{*, given}
import com.softwaremill.realworld.articles.model.{ArticleAuthor, ArticleCreateData, ArticleData, ArticleRow}
import com.softwaremill.realworld.common.Exceptions.AlreadyInUse
import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.profiles.ProfilesRepository
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.TestUtils.*
import org.sqlite.{SQLiteErrorCode, SQLiteException}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.*
import zio.test.Assertion.*
import zio.{Cause, RIO, Random, ZIO, ZLayer}

import java.time.Instant
import javax.sql.DataSource

object ArticlesRepositorySpec extends ZIOSpecDefault:

  def spec = suite("check list features")(
    suite("list articles")(
      suite("with auth data only")(
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
    suite("add & update tags")(
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
    )
  ).provide(
    ArticlesRepository.live,
    UsersRepository.live,
    ProfilesRepository.live,
    testDbLayerWithEmptyDb
  )

//      suite("create and update article")(
//        test("create article") {
//          for {
//            repo <- ZIO.service[ArticlesRepository]
//            slug = "new-article-under-test"
//            _ <- repo.add(
//              ArticleCreateData(
//                title = "New-article-under-test",
//                description = "What a nice day!",
//                body = "Writing scala code is quite challenging pleasure",
//                tagList = None
//              ),
//              10
//            )
//            result <- repo.findBySlug(slug).map(_.get)
//          } yield assertTrue {
//            // TODO there must be better way to implement this...
//            import com.softwaremill.realworld.common.model.ArticleDiff.{*, given}
//            compare(
//              result,
//              ArticleData(
//                slug,
//                "New-article-under-test",
//                "What a nice day!",
//                "Writing scala code is quite challenging pleasure",
//                Nil,
//                null,
//                null,
//                false,
//                0,
//                ArticleAuthor(
//                  username = "jake",
//                  bio = Some("I work at statefarm"),
//                  image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
//                  following = false
//                )
//              )
//            ).isIdentical
//          }
//        },
//        test("create non unique article - check article already exists") {
//          assertZIO((for {
//            repo <- ZIO.service[ArticlesRepository]
//            v <- repo.add(
//              ArticleCreateData(
//                title = "How-to-train-your-dragon",
//                description = "What a nice day!",
//                body = "Writing scala code is quite challenging pleasure",
//                tagList = None
//              ),
//              10
//            )
//          } yield v).exit)(
//            failsCause(
//              containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
//            )
//          )
//        },
//        test("update article") {
//          for {
//            repo <- ZIO.service[ArticlesRepository]
//            updatedSlug = "updated-article-under-test"
//            _ <- repo.add(
//              ArticleCreateData(
//                title = "New article under test",
//                description = "What a nice day!",
//                body = "Writing scala code is quite challenging pleasure",
//                tagList = None
//              ),
//              10
//            )
//            articleIdOpt <- repo.findArticleIdBySlug("new-article-under-test")
//            articleId <- ZIO.succeed(articleIdOpt.get)
//            _ <- repo.updateById(
//              ArticleData(
//                updatedSlug,
//                "Updated article under test",
//                "What a nice updated day!",
//                "Updating scala code is quite challenging pleasure",
//                Nil,
//                null, // TODO I think more specialized class should be used for article creation
//                Instant.now(),
//                false,
//                0,
//                null
//              ),
//              articleId
//            )
//            result <- repo.findBySlug(updatedSlug).map(_.get)
//          } yield assertTrue {
//            // TODO there must be better way to implement this...
//            import com.softwaremill.realworld.common.model.ArticleDiffWithSameCreateAt.{*, given}
//            compare(
//              result,
//              ArticleData(
//                updatedSlug,
//                "Updated article under test",
//                "What a nice updated day!",
//                "Updating scala code is quite challenging pleasure",
//                Nil,
//                null,
//                null,
//                false,
//                0,
//                ArticleAuthor(
//                  username = "jake",
//                  bio = Some("I work at statefarm"),
//                  image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
//                  following = false
//                )
//              )
//            ).isIdentical
//          } && zio.test.assert(result.updatedAt)(isGreaterThan(result.createdAt))
//        },
//        test("update article - check article already exist") {
//          assertZIO((for {
//            repo <- ZIO.service[ArticlesRepository]
//            _ <- repo.add(
//              ArticleCreateData(
//                title = "Slug to update",
//                description = "What a nice day!",
//                body = "Writing scala code is quite challenging pleasure",
//                tagList = None
//              ),
//              10
//            )
//            _ <- repo.add(
//              ArticleCreateData(
//                title = "Existing slug",
//                description = "It occupies article slug",
//                body = "Which will be used for updating another article during next step",
//                tagList = None
//              ),
//              10
//            )
//            articleIdOpt <- repo.findArticleIdBySlug("slug-to-update")
//            articleId <- ZIO.succeed(articleIdOpt.get)
//            _ <- repo.updateById(
//              ArticleData(
//                "existing-slug",
//                "Existing slug",
//                "Updated article under test",
//                "Updating scala code is quite challenging pleasure",
//                Nil,
//                null, // TODO I think more specialized class should be used for article creation
//                Instant.now(),
//                false,
//                0,
//                null
//              ),
//              articleId
//            )
//            v <- repo.findBySlug("existing-slug").map(_.get)
//          } yield v).exit)(
//            failsCause(
//              containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
//            )
//          )
//        }
//      ).provide(
//        ArticlesRepository.live,
//        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
//      )
//    )
//  )
