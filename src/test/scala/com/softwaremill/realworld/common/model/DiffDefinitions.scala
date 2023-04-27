package com.softwaremill.realworld.common.model

import com.softwaremill.diffx.Diff
import com.softwaremill.realworld.articles.model.{Article, ArticleAuthor, ArticleData}
import com.softwaremill.realworld.users.api.UserResponse
import com.softwaremill.realworld.users.{User, UserWithPassword}

object UserDiff:
  given userDataDiff: Diff[User] = Diff.derived[User].ignore(_.token)
  given userDiff: Diff[UserResponse] = Diff.derived[UserResponse]

object UserWithPasswordDiff:
  given userDataDiff: Diff[User] = Diff.derived[User]
  given UserWithPasswordDiff: Diff[UserWithPassword] = Diff.derived[UserWithPassword].ignore(_.hashedPassword)

object ArticleDiff:
  given articleDiff: Diff[Article] = Diff.derived[Article]
  given articleDataDiff: Diff[ArticleData] = Diff.derived[ArticleData].ignore(_.createdAt).ignore(_.updatedAt)
  given articleAuthorDiff: Diff[ArticleAuthor] = Diff.derived[ArticleAuthor]

object ArticleDiffWithSameCreateAt:
  given articleDiff: Diff[Article] = Diff.derived[Article]
  given articleDataDiff: Diff[ArticleData] = Diff.derived[ArticleData].ignore(_.createdAt).ignore(_.updatedAt)
  given articleAuthorDiff: Diff[ArticleAuthor] = Diff.derived[ArticleAuthor]
