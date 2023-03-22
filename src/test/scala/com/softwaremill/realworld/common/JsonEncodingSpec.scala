package com.softwaremill.realworld.common
import com.softwaremill.realworld.articles.ArticlesEndpointsSpec.{suite, test}
import com.softwaremill.realworld.users.{User, UserData}
import zio.json.*
import zio.test.*
import zio.test.Assertion.{equalTo, isEmpty, isNegative}

object JsonEncodingSpec extends ZIOSpecDefault {

  override def spec = suite("JSON encoding for data objects") {
    suite("User related objects") {
      test("Fields with None value are present in rendered json as null values") {
        val user = User(UserData(email = "email@domain.com", token = None, username = "username", bio = None, image = None))
        assert(user.toJson)(
          equalTo("""{"user":{"email":"email@domain.com","token":null,"username":"username","bio":null,"image":null}}""")
        )
      }
    }
  }
}
