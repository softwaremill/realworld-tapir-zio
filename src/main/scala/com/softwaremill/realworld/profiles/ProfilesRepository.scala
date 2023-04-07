package com.softwaremill.realworld.profiles

import io.getquill.*
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.jdbczio.*
import zio.{Tag, ZIO, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource

trait ProfilesRepository {
  def follow(followedId: Int, followerId: Int): ZIO[Any, SQLException, Long]
  def unfollow(followedId: Int, followerId: Int): ZIO[Any, SQLException, Long]
  def isFollowing(followedId: Int, followerId: Int): ZIO[Any, SQLException, Boolean]
}

object ProfilesRepository {
  def live[I <: SqlIdiom: Tag, N <: NamingStrategy: Tag]: ZLayer[Quill[I, N], Nothing, ProfilesRepository] =
    ZLayer.fromFunction(ProfilesRepositoryLive[I, N](_))

  private class ProfilesRepositoryLive[I <: SqlIdiom, N <: NamingStrategy](quill: Quill[I, N]) extends ProfilesRepository {

    import quill.*

    def follow(followedId: Int, followerId: Int): ZIO[Any, SQLException, Long] = run {
      query[Followers].insert(_.userId -> lift(followedId), _.followerId -> lift(followerId)).onConflictIgnore
    }

    def unfollow(followedId: Int, followerId: Int): ZIO[Any, SQLException, Long] = run {
      query[Followers].filter(f => (f.userId == lift(followedId)) && (f.followerId == lift(followerId))).delete
    }

    def isFollowing(followedId: Int, followerId: Int): ZIO[Any, SQLException, Boolean] = run {
      query[Followers].filter(_.userId == lift(followedId)).filter(_.followerId == lift(followerId)).map(_ => 1).nonEmpty
    }
  }
}
