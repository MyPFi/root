package com.andreidiego.mpfi.stocks.adapter.files.pdfs.inception.poc

import os.Path

object ListFiles {
  def main(args: Array[String]): Unit = {
    val datePattern = raw"(\d{2})-(\d{2})-(\d{4})".r.unanchored
    val yearPattern = raw"(\d{4})".r.unanchored

    os.list(Path("F:\\OneDrive\\Documentos\\Financeiros\\Investimentos\\Bolsa\\Notas de Corretagem"))
      .filter(os.isDir(_))
      .filter(osPath => yearPattern.matches(osPath.toString))
      .flatMap(os.list)
      .map(_.toString)
      .filter(_ endsWith ".pdf")
      .sortWith((_, _) match
        case (datePattern(day1, month1, year1), datePattern(day2, month2, year2)) =>
          (year1 + month1 + day1).toInt > (year2 + month2 + day2).toInt
      )
      .foreach(println)
  }
}
