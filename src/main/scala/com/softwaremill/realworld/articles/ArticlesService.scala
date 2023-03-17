package com.softwaremill.realworld.articles

import com.softwaremill.realworld.common.Exceptions.NotFound
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.profiles.ProfileRow
import com.softwaremill.realworld.users.UsersRepository
import zio.{Console, IO, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class ArticlesService(articlesRepository: ArticlesRepository, usersRepository: UsersRepository):

  def list(filters: Map[ArticlesFilters, String], pagination: Pagination): IO[SQLException, List[ArticleData]] = articlesRepository
    .list(filters, pagination)

  def find(slug: String): IO[Exception, ArticleData] = articlesRepository
    .findBySlug(slug)
    .flatMap {
      case Some(a) => ZIO.succeed(a)
      case None    => ZIO.fail(Exceptions.NotFound(s"Article with slug $slug doesn't exist."))
    }

  def create(createData: ArticleCreateData, userEmail: String): IO[Exception, ArticleData] = {
    val title = createData.title.trim
    val slug = title.replace(" ", "-")
    val now = Instant.now()

    def checkArticleDoesNotExist(): IO[Exception, Unit] =
      for {
        maybeArticle <- articlesRepository.findBySlug(slug)
        _ <- ZIO.fail(Exceptions.AlreadyInUse(s"Article with slug $slug already exists!")).when(maybeArticle.isDefined)
      } yield ()

    for {
      _ <- checkArticleDoesNotExist()
      maybeUser <- usersRepository.findUserRowByEmail(userEmail)
      user <- ZIO.fromOption(maybeUser).mapError(_ => NotFound("User doesn't exist, re-login may be needed!"))
      newArticle = ArticleData(
        slug = slug,
        title = title,
        description = createData.description,
        body = createData.body,
        tagList = createData.tagList,
        createdAt = now,
        updatedAt = now,
        favorited = false,
        favoritesCount = 0,
        author = ArticleAuthor(
          username = user.username,
          bio = Option(user.bio),
          image = Option(user.image),
          following = false
        ) // TODO update when follows are implemented
      )
      _ <- articlesRepository.add(newArticle, user.userId)
      _ <- ZIO.foreach(createData.tagList)(tag => articlesRepository.addTag(tag, slug))
    } yield newArticle
  }

  def update(articleUpdateData: ArticleUpdateData, slug: String, email: String): IO[Exception, ArticleData] = {
    for {
      maybeOldArticle <- articlesRepository.findBySlugAndEmail(slug, email)
      oldArticle <- ZIO.fromOption(maybeOldArticle).mapError(_ => NotFound(s"Article with slug $slug doesn't exist."))
      updatedSlug = articleUpdateData.title
        .map(_.toLowerCase)
        .map(_.trim)
        .map(title => title.replace(" ", "-"))
        .getOrElse(oldArticle.slug)
      updatedArticle <- articlesRepository.updateBySlug(
        articleUpdateData.copy(
          title = articleUpdateData.title.map(_.trim),
          slug = Option(updatedSlug)
        ),
        slug
      )
    } yield updatedArticleData(oldArticle, updatedArticle)
  }

  private def updatedArticleData(articleData: ArticleData, updatedData: ArticleUpdateData): ArticleData =
    articleData.copy(
      slug = updatedData.slug.orNull,
      title = updatedData.title.orNull,
      description = updatedData.description.orNull,
      body = updatedData.body.orNull
    )

object ArticlesService:
  val live: ZLayer[ArticlesRepository with UsersRepository, Nothing, ArticlesService] = ZLayer.fromFunction(ArticlesService(_, _))
