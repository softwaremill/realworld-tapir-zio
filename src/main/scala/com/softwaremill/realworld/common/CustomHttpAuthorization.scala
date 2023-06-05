package com.softwaremill.realworld.common

import sttp.model.HeaderNames
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.{Codec, CodecFormat, DecodeResult, EndpointInput, Mapping, Schema, header}

// This authorization allows the use of both the "Bearer" header and the "Token" parameter at the beginning of the token.
object CustomHttpAuthorization {

  type StringListCodec[T] = Codec[List[String], T, CodecFormat.TextPlain]

  def authorizeBearerAndTokenHeaders[T: StringListCodec]: EndpointInput.Auth[T, EndpointInput.AuthType.Http] = {
    val codec = implicitly[Codec[List[String], T, CodecFormat.TextPlain]]

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

  private def stringPrefixCaseInsensitiveForList(defaultPrefix: String) = {
    def removePrefix(v: List[String]): DecodeResult[List[String]] = {
      val authSchemeOpt = v.headOption.flatMap(token => token.split(" ").headOption)
      val prefixToRemove = authSchemeOpt.getOrElse("Bearer") + " "
      DecodeResult
        .sequence(v.map { s => cropPrefix(s, prefixToRemove) })
        .map(_.toList)
    }

    Mapping.fromDecode[List[String], List[String]](removePrefix)(v => v.map(d => s"$defaultPrefix$d"))
  }

  private def cropPrefix(s: String, prefix: String) = {
    val prefixLength = prefix.length
    val prefixLower = prefix.toLowerCase
    if (s.toLowerCase.startsWith(prefixLower)) DecodeResult.Value(s.substring(prefixLength))
    else DecodeResult.Error(s, new IllegalArgumentException(s"The given value doesn't start with $prefix"))
  }
}
