package com.softwaremill.realworld.common

import sttp.model.HeaderNames
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.{Codec, CodecFormat, DecodeResult, EndpointInput, Mapping, Schema, header}

/** This authorization mechanism grants the ability to employ either the "Bearer" header or the "Token" parameter at the commencement of the
  * token. It offers the flexibility to choose between these two methods, enabling users to seamlessly incorporate the access token as per
  * their specific requirements or API specifications.
  */
object CustomAuthInputs {

  type StringListCodec[T] = Codec[List[String], T, CodecFormat.TextPlain]

  def authBearerOrTokenHeaders[T: StringListCodec]: EndpointInput.Auth[T, EndpointInput.AuthType.Http] = {
    val codec = implicitly[StringListCodec[T]]

    def filterBearerAndTokenHeaders[T: StringListCodec](headers: List[String]) =
      headers.filter(x => x.startsWith("Token") || x.startsWith("Bearer"))

    val authCodec = Codec
      .id[List[String], CodecFormat.TextPlain](codec.format, Schema.binary)
      .map(headers => filterBearerAndTokenHeaders(headers))(identity)
      .map(stringPrefixCaseInsensitiveForList("Bearer"))
      .mapDecode(codec.decode)(codec.encode)
      .schema(codec.schema)

    EndpointInput.Auth(
      header[T](HeaderNames.Authorization)(authCodec),
      WWWAuthenticateChallenge.bearer,
      EndpointInput.AuthType.Http("Bearer"),
      EndpointInput.AuthInfo.Empty
    )
  }

  private def stringPrefixCaseInsensitiveForList(defaultPrefix: String): Mapping[List[String], List[String]] = {
    def removePrefix(v: List[String]): DecodeResult[List[String]] = {
      val authSchemeOpt = v.headOption.flatMap(token => token.split(" ").headOption)
      val prefixToRemove = authSchemeOpt.getOrElse("Bearer") + " "
      DecodeResult
        .sequence(v.map { s => cropPrefix(s, prefixToRemove) })
        .map(_.toList)
    }

    Mapping.fromDecode[List[String], List[String]](removePrefix)(v => v.map(d => s"$defaultPrefix$d"))
  }

  private def cropPrefix(s: String, prefix: String): DecodeResult[String] = {
    val prefixLength = prefix.length
    val prefixLower = prefix.toLowerCase
    if (s.toLowerCase.startsWith(prefixLower)) DecodeResult.Value(s.substring(prefixLength))
    else DecodeResult.Error(s, new IllegalArgumentException(s"The given value doesn't start with $prefix"))
  }
}
