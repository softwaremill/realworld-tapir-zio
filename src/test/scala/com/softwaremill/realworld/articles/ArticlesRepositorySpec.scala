package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.ArticlesEndpoints.{*, given}
import com.softwaremill.realworld.articles.model.{ArticleAuthor, ArticleCreateData, ArticleData, ArticleRow}
import com.softwaremill.realworld.common.Exceptions.AlreadyInUse
import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
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

  def spec = suite("Check list features")(
    suite("list articles")(
      suite("with auth data only")(
        test("no filters") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(ArticlesFilters.empty, Pagination(20, 0))
          } yield zio.test.assert(v)(isEmpty)
        },
        test("with filters") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(
              ArticlesFilters(Some("dragon"), Some("John"), Some("Ron")),
              Pagination(20, 0)
            )
          } yield zio.test.assert(v)(isEmpty)
        }
      ).provide(
        ArticlesRepository.live,
        testDbLayerWithEmptyDb
      ),
      suite("with populated db")(
        test("with small offset and small limit") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(ArticlesFilters.empty, Pagination(1, 1))
          } yield zio.test.assert(v)(
            hasSize(equalTo(1))
              && contains(
                ArticleData(
                  "how-to-train-your-dragon-2",
                  "How to train your dragon 2",
                  "So toothless",
                  "Its a dragon",
                  List("dragons", "goats", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  1,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
          )
        },
        test("no filters") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(ArticlesFilters.empty, Pagination(20, 0))
          } yield zio.test.assert(v)(
            hasSize(equalTo(3))
              && contains(
                ArticleData(
                  "how-to-train-your-dragon",
                  "How to train your dragon",
                  "Ever wonder how?",
                  "It takes a Jacobian",
                  List("dragons", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  2,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
              && contains(
                ArticleData(
                  "how-to-train-your-dragon-2",
                  "How to train your dragon 2",
                  "So toothless",
                  "Its a dragon",
                  List("dragons", "goats", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  1,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
              && contains(
                ArticleData(
                  "how-to-train-your-dragon-3",
                  "How to train your dragon 3",
                  "The tagless one",
                  "Its not a dragon",
                  List(),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  0,
                  ArticleAuthor(
                    "john",
                    Some("I no longer work at statefarm"),
                    Some("https://i.stack.imgur.com/xHWG8.jpg"),
                    following = false
                  )
                )
              )
          )
        },
        test("with tag filter") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(
              ArticlesFilters.withTag("dragons"),
              Pagination(20, 0)
            )
          } yield zio.test.assert(v)(
            hasSize(equalTo(2))
              && contains(
                ArticleData(
                  "how-to-train-your-dragon",
                  "How to train your dragon",
                  "Ever wonder how?",
                  "It takes a Jacobian",
                  List("dragons", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  2,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
              && contains(
                ArticleData(
                  "how-to-train-your-dragon-2",
                  "How to train your dragon 2",
                  "So toothless",
                  "Its a dragon",
                  List("dragons", "goats", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  1,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
          )
        },
        test("with favorited filter") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(
              ArticlesFilters.withFavorited("jake"),
              Pagination(20, 0)
            )
          } yield zio.test.assert(v)(
            hasSize(equalTo(1))
              && contains(
                ArticleData(
                  "how-to-train-your-dragon",
                  "How to train your dragon",
                  "Ever wonder how?",
                  "It takes a Jacobian",
                  List("dragons", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  2,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
          )
        },
        test("with author filter") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.list(
              ArticlesFilters.withAuthor("john"),
              Pagination(20, 0)
            )
          } yield zio.test.assert(v)(
            hasSize(equalTo(1))
              && contains(
                ArticleData(
                  "how-to-train-your-dragon-3",
                  "How to train your dragon 3",
                  "The tagless one",
                  "Its not a dragon",
                  List(),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  0,
                  ArticleAuthor(
                    "john",
                    Some("I no longer work at statefarm"),
                    Some("https://i.stack.imgur.com/xHWG8.jpg"),
                    following = false
                  )
                )
              )
          )
        }
      ).provide(
        ArticlesRepository.live,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      ),
      suite("find article")(
        test("find by slug") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.findBySlug("how-to-train-your-dragon")
          } yield zio.test.assert(v)(
            equalTo(
              Option(
                ArticleData(
                  "how-to-train-your-dragon",
                  "How to train your dragon",
                  "Ever wonder how?",
                  "It takes a Jacobian",
                  List("dragons", "training"),
                  Instant.ofEpochMilli(1455765776637L),
                  Instant.ofEpochMilli(1455767315824L),
                  false,
                  2,
                  ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
            )
          )
        },
        test("find article - check article not found") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.findBySlug("non-existing-article-slug")
          } yield zio.test.assert(v)(isNone)
        },
        test("find article by slug as seen by user that marked it as favorite") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.findBySlugAsSeenBy("how-to-train-your-dragon-2", viewerEmail = "jake@example.com")
          } yield zio.test.assert(v)(
            equalTo(
              Option(
                ArticleData(
                  slug = "how-to-train-your-dragon-2",
                  title = "How to train your dragon 2",
                  description = "So toothless",
                  body = "Its a dragon",
                  tagList = List("dragons", "goats", "training"),
                  createdAt = Instant.ofEpochMilli(1455765776637L),
                  updatedAt = Instant.ofEpochMilli(1455767315824L),
                  favorited = false,
                  favoritesCount = 1,
                  author =
                    ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
            )
          )
        },
        test("find article by slug as seen by user that marked it as favorite") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.findBySlugAsSeenBy("how-to-train-your-dragon-2", viewerEmail = "john@example.com")
          } yield zio.test.assert(v)(
            equalTo(
              Option(
                ArticleData(
                  slug = "how-to-train-your-dragon-2",
                  title = "How to train your dragon 2",
                  description = "So toothless",
                  body = "Its a dragon",
                  tagList = List("dragons", "goats", "training"),
                  createdAt = Instant.ofEpochMilli(1455765776637L),
                  updatedAt = Instant.ofEpochMilli(1455767315824L),
                  favorited = true,
                  favoritesCount = 1,
                  author =
                    ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                )
              )
            )
          )
        }
      ).provide(
        ArticlesRepository.live,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      ),
      suite("add & update tags")(
        test("add tag") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            newTag = "new_tag"
            _ <- repo.addTag(newTag, 1)
            v <- repo.findBySlug("how-to-train-your-dragon").map(_.get.tagList)
          } yield zio.test.assert(v)(contains(newTag))
        },
        test("add tag - check other article is untouched") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            newTag = "new_tag"
            _ <- repo.addTag(newTag, 1)
            v <- repo.findBySlug("how-to-train-your-dragon-2").map(_.get.tagList)
          } yield zio.test.assert(v)(hasNoneOf(newTag))
        }
      ).provide(
        ArticlesRepository.live,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      ),
      suite("create and update article")(
        test("create article") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            slug = "new-article-under-test"
            _ <- repo.add(
              ArticleCreateData(
                title = "New-article-under-test",
                description = "What a nice day!",
                body = "Writing scala code is quite challenging pleasure",
                tagList = None
              ),
              10
            )
            result <- repo.findBySlug(slug).map(_.get)
          } yield assertTrue {
            // TODO there must be better way to implement this...
            import com.softwaremill.realworld.common.model.ArticleDiff.{*, given}
            compare(
              result,
              ArticleData(
                slug,
                "New-article-under-test",
                "What a nice day!",
                "Writing scala code is quite challenging pleasure",
                Nil,
                null,
                null,
                false,
                0,
                ArticleAuthor(
                  username = "jake",
                  bio = Some("I work at statefarm"),
                  image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
                  following = false
                )
              )
            ).isIdentical
          }
        },
        test("create non unique article - check article already exists") {
          assertZIO((for {
            repo <- ZIO.service[ArticlesRepository]
            v <- repo.add(
              ArticleCreateData(
                title = "How-to-train-your-dragon",
                description = "What a nice day!",
                body = "Writing scala code is quite challenging pleasure",
                tagList = None
              ),
              10
            )
          } yield v).exit)(
            failsCause(
              containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
            )
          )
        },
        test("update article") {
          for {
            repo <- ZIO.service[ArticlesRepository]
            updatedSlug = "updated-article-under-test"
            _ <- repo.add(
              ArticleCreateData(
                title = "New article under test",
                description = "What a nice day!",
                body = "Writing scala code is quite challenging pleasure",
                tagList = None
              ),
              10
            )
            articleId <- repo.findArticleIdBySlug("new-article-under-test")
            _ <- repo.updateById(
              ArticleData(
                updatedSlug,
                "Updated article under test",
                "What a nice updated day!",
                "Updating scala code is quite challenging pleasure",
                Nil,
                null, // TODO I think more specialized class should be used for article creation
                Instant.now(),
                false,
                0,
                null
              ),
              articleId
            )
            result <- repo.findBySlug(updatedSlug).map(_.get)
          } yield assertTrue {
            // TODO there must be better way to implement this...
            import com.softwaremill.realworld.common.model.ArticleDiffWithSameCreateAt.{*, given}
            compare(
              result,
              ArticleData(
                updatedSlug,
                "Updated article under test",
                "What a nice updated day!",
                "Updating scala code is quite challenging pleasure",
                Nil,
                null,
                null,
                false,
                0,
                ArticleAuthor(
                  username = "jake",
                  bio = Some("I work at statefarm"),
                  image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
                  following = false
                )
              )
            ).isIdentical
          } && zio.test.assert(result.updatedAt)(isGreaterThan(result.createdAt))
        },
        test("update article - check article already exist") {
          assertZIO((for {
            repo <- ZIO.service[ArticlesRepository]
            _ <- repo.add(
              ArticleCreateData(
                title = "Slug to update",
                description = "What a nice day!",
                body = "Writing scala code is quite challenging pleasure",
                tagList = None
              ),
              10
            )
            _ <- repo.add(
              ArticleCreateData(
                title = "Existing slug",
                description = "It occupies article slug",
                body = "Which will be used for updating another article during next step",
                tagList = None
              ),
              10
            )
            articleId <- repo.findArticleIdBySlug("slug-to-update")
            _ <- repo.updateById(
              ArticleData(
                "existing-slug",
                "Existing slug",
                "Updated article under test",
                "Updating scala code is quite challenging pleasure",
                Nil,
                null, // TODO I think more specialized class should be used for article creation
                Instant.now(),
                false,
                0,
                null
              ),
              articleId
            )
            v <- repo.findBySlug("existing-slug").map(_.get)
          } yield v).exit)(
            failsCause(
              containsCause(Cause.fail(AlreadyInUse(message = "Article name already exists")))
            )
          )
        }
      ).provide(
        ArticlesRepository.live,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      )
    )
  )
