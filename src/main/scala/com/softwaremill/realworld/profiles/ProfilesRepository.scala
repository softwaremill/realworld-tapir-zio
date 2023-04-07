package com.softwaremill.realworld.profiles

import io.getquill.*
import io.getquill.jdbczio.*
import zio.ZLayer

import javax.sql.DataSource

class ProfilesRepository(quill: Quill.Sqlite[SnakeCase]) {
  import quill.*

  def follow(followedId: Int, followerId: Int) = run {
    query[Followers].insert(_.userId -> lift(followedId), _.followerId -> lift(followerId)).onConflictIgnore
  }

  def unfollow(followedId: Int, followerId: Int) = run {
    query[Followers].filter(f => (f.userId == lift(followedId)) && (f.followerId == lift(followerId))).delete
  }

  def isFollowing(followedId: Int, followerId: Int) = run {
    query[Followers].filter(_.userId == lift(followedId)).filter(_.followerId == lift(followerId)).map(_ => 1).nonEmpty
  }

}

object ProfilesRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, ProfilesRepository] =
    ZLayer.fromFunction(new ProfilesRepository(_))
