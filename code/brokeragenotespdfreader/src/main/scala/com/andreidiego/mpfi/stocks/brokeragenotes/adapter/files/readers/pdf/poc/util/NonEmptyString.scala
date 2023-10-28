package com.andreidiego.mpfi.stocks.brokeragenotes.adapter.files.readers.pdf.poc.util

import scala.compiletime.{error, requireConst}

opaque type NonEmptyString <: String = String

object NonEmptyString:
  def apply(s: String): Option[NonEmptyString] =
    if s.isEmpty then None else Some(s)

  inline def from(inline s: String): NonEmptyString =
    requireConst(s)
    inline if s == "" then error("got an empty string") else s