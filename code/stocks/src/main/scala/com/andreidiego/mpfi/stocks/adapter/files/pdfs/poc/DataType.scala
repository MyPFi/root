package com.andreidiego.mpfi.stocks.adapter.files.pdfs.poc

object DataType:
  def apply(dataType: String): DataType =
    dataType match {
      case "Número" | "Numero" | "Number" => Number
      case "Caracter" | "Character" => Character
      case "Alfanumérico" | "Alfanumerico" | "String" => String
      case "Data" | "Date" => Date
      case "Moeda" | "Moeda-Real" | "Currency" => Currency
    }

enum DataType:
  case Number, Character, String, Date, Currency