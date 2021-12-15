package com.andreidiego.mpfi.stocks.deprecated

import java.time.LocalDateTime
import scala.language.deprecated.symbolLiterals

object Sales {

  private val sales = List(
    /*
        Sale(LocalDateTime.of(2016, 11, 21, 0, 0), 'BBAS3, 400, 28.33),
        Sale(LocalDateTime.of(2016, 12, 29, 0, 0), 'KLBN4, 3000, 2.86),
        Sale(LocalDateTime.of(2017, 1, 2, 0, 0), 'TAEE11, 400, 20.90),
        Sale(LocalDateTime.of(2017, 1, 4, 0, 0), 'USIM5, 200, 4.55),
        Sale(LocalDateTime.of(2017, 1, 12, 0, 0), 'TPIS3, 2500, 3.31),
        Sale(LocalDateTime.of(2017, 1, 16, 0, 0), 'ETER3, 7200, 1.45),
        Sale(LocalDateTime.of(2017, 1, 17, 0, 0), 'CESP6, 1000, 15.20),
        Sale(LocalDateTime.of(2017, 1, 20, 0, 0), 'TPIS3, 3500, 3.00),
        Sale(LocalDateTime.of(2017, 2, 1, 0, 0), 'JBSS3, 1000, 12.35),
        Sale(LocalDateTime.of(2017, 2, 1, 0, 0), 'GFSA3, 3600, 2.55),
        Sale(LocalDateTime.of(2017, 2, 2, 0, 0), 'TPIS3, 2600, 3.25),
        Sale(LocalDateTime.of(2017, 2, 3, 0, 0), 'ITSA4, 900, 9.50),
        Sale(LocalDateTime.of(2017, 2, 8, 0, 0), 'BTOW3, 1000, 13.15),
        Sale(LocalDateTime.of(2017, 2, 13, 0, 0), 'ETER3, 6200, 1.60),
        Sale(LocalDateTime.of(2017, 2, 15, 0, 0), 'ECOR3, 1000, 9.40),
        Sale(LocalDateTime.of(2017, 2, 15, 0, 0), 'CMIG4, 1000, 9.50),
        Sale(LocalDateTime.of(2017, 2, 15, 0, 0), 'TPIS3, 2900, 3.30),
        Sale(LocalDateTime.of(2017, 2, 15, 0, 0), 'GOLL4, 1600, 7.80),
        Sale(LocalDateTime.of(2017, 2, 17, 0, 0), 'JHSF3, 2800, 2.60),
        Sale(LocalDateTime.of(2017, 2, 22, 0, 0), 'BEES3, 2900, 4.11),
        Sale(LocalDateTime.of(2017, 3, 6, 0, 0), 'QGEP3, 1700, 6.51),
        Sale(LocalDateTime.of(2017, 3, 17, 0, 0), 'HYPE3, 400, 29.25),
        Sale(LocalDateTime.of(2017, 3, 29, 0, 0), 'LAME4, 700, 16.37),
        Sale(LocalDateTime.of(2017, 4, 11, 0, 0), 'GOLL4, 1200, 10.24),
        Sale(LocalDateTime.of(2017, 5, 2, 0, 0), 'MRFG3, 1500, 7.48),
        Sale(LocalDateTime.of(2017, 5, 4, 0, 0), 'RAIL3, 900, 9.50),
        Sale(LocalDateTime.of(2017, 5, 9, 0, 0), 'RAIL3, 100, 9.50),
        Sale(LocalDateTime.of(2017, 5, 10, 0, 0), 'BRKM5, 300, 35.00),
        Sale(LocalDateTime.of(2017, 5, 10, 0, 0), 'LINX3, 700, 19.00),
        Sale(LocalDateTime.of(2017, 5, 15, 0, 0), 'USIM5, 3100, 4.35),
        Sale(LocalDateTime.of(2017, 5, 16, 0, 0), 'GGBR4, 700, 10.17),
        Sale(LocalDateTime.of(2017, 5, 18, 0, 0), 'FIBR3, 300, 32.55),
        Sale(LocalDateTime.of(2017, 4, 24, 0, 0), 'KLBN4, 4000, 2.89),
        Sale(LocalDateTime.of(2017, 5, 30, 0, 0), 'VALE5, 300, 27.15),
        Sale(LocalDateTime.of(2017, 8, 14, 0, 0), 'BTOW3, 600, 14.50),
        Sale(LocalDateTime.of(2017, 8, 21, 11, 39, 39), 'POMO4, 2300, 3.52),
        Sale(LocalDateTime.of(2017, 8, 22, 12, 0, 23), 'ITSA4, 600, 10.20),
        Sale(LocalDateTime.of(2017, 8, 24, 3, 28, 14), 'DTEX3, 1100, 8.30),
        Sale(LocalDateTime.of(2017, 8, 28, 0, 0), 'AMAR3, 1500, 7.12),
        Sale(LocalDateTime.of(2017, 8, 28, 0, 0), 'GOLL4, 800, 11.00),
        Sale(LocalDateTime.of(2017, 8, 28, 0, 0), 'GOLL4, 600, 11.00)
    */
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 6, 7, 0, 0)), 'GOLL4, 800, 8.35),
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 6, 30, 0, 0)), 'BEEF3, 900, 12.25),
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 7, 18, 10, 2)), 'TPIS3, 4900, 3.90),
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 7, 19, 13, 59)), 'POSI3, 2900, 3.90),
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 7, 27, 12, 14)), 'LAME4, 400, 15.50),
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 8, 7, 0, 0)), 'CSNA3, 1000, 8.13),
    Sale(Minute.fromDateTime(LocalDateTime.of(2017, 8, 8, 0, 0)), 'EMBR3, 600, 16.67),
  )

  def of(timeSpan: TimeSpan) = new Sales(sales.filter(timeSpan includes _.moment))
}

class Sales private(val list: List[Sale])