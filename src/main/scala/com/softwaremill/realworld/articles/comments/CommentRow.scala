package com.softwaremill.realworld.articles.comments

import java.time.Instant

case class CommentRow(commentId: Int, articleId: Int, createdAt: Instant, updatedAt: Instant, authorId: Int, body: String)
