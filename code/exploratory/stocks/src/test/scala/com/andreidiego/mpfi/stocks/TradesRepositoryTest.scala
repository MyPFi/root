package com.andreidiego.mpfi.stocks

import org.scalatest.freespec.AnyFreeSpec

class TradesRepositoryTest extends AnyFreeSpec {
/*
  "A TradesRepository" - {
    "has a list of trades to which" - {
      "Trades can be added" in {}
//      assert(TradesRepository.trades == List[Trade]())
    }

  }

  "When trades are added" - {
    "they get accumulated, in a custody view" in {
      "a new purchase for an existing asset, adds up to the first one" in {}
      "a sale is taken out of the existing asset" in {}
    }
    "they get matched with existing trades for the same asset in a variety of ways" - {
      "a sale which zeroes out" - {
        "the only existing purchase" in {}
        "several existing purchases" in {}
      }
      "a partial sale for" - {
        "the only purchase" in {}
        "several existing purchases" in {}
      }
      "a sale which zeroes out the only purchase" in {}
    }
  }
  "given" - {
    "a map, whose key is a stock symbol and the value is a list of all trades with this stock ordered by date" - {
/*
      Map(
        'AMAR3 -> List[Trade](
          Purchase("09/08/2017", 'AMAR3, 1500, 6.75),
          Sale("28/08/2017", 'AMAR3, 1500, 7.12)),
        'BBAS3 -> List[Trade](
          Purchase("18/11/2016", 'BBAS3, 400, 25.45),
          Sale("21/11/2016", 'BBAS3, 400, 28.33)),
        'BEEF3 -> List[Trade](
          Purchase("21/02/2017", 'BEEF3, 900, 11.60),
          Sale("30/06/2017", 'BEEF3, 900, 12.25)),
        'BEES3 -> List[Trade](
          Purchase("21/02/2017", 'BEES3, 2900, 3.50),
          Sale("22/02/2017", 'BEES3, 2900, 4.11)),
        'BRKM5 -> List[Trade](
          Purchase("21/02/2017", 'BRKM5, 300, 33.70),
          Sale("10/05/2017", 'BRKM5, 300, 35.00)),
        'BSEV3 -> List[Trade](
          Purchase("30/11/2016", 'BSEV3, 400, 7.35),
          Purchase("01/12/2016", 'BSEV3, 600, 7.35)),
        'BTOW3 -> List[Trade](
          Purchase("30/11/2016", 'BTOW3, 1000, 10.85),
          Sale("08/02/2017", 'BTOW3, 1000, 13.15),
          Purchase("12/05/2017", 'BTOW3, 600, 13.40),
          Sale("14/08/2017", 'BTOW3, 600, 14.50)),
        'CESP6 -> List[Trade](
          Purchase("22/11/2016", 'CESP6, 1000, 13.97),
          Sale("17/01/2017", 'CESP6, 1000, 15.20),
          Purchase("07/03/2017", 'CESP6, 600, 18.00)),
        'CMIG4 -> List[Trade](
          Purchase("01/12/2016", 'CMIG4, 1000, 7.90),
          Sale("15/02/2017", 'CMIG4, 1000, 9.50),
          Purchase("08/08/2017", 'CMIG4, 900, 8.87)),
        'CSNA3 -> List[Trade](
          Purchase("15/05/2017", 'CSNA3, 1000, 7.65),
          Sale("07/08/2017", 'CSNA3, 1000, 8.13)),
        'DTEX3 -> List[Trade](
          Purchase("27/07/2017", 'DTEX3, 1100, 7.90),
          Sale("24/08/2017", 'DTEX3, 1100, 8.30)),
        'ECOR3 -> List[Trade](
          Purchase("18/11/2016", 'ECOR3, 1000, 7.83),
          Sale("15/02/2017", 'ECOR3, 1000, 9.40),
          Purchase("28/07/2017", 'ECOR3, 900, 10.80)),
        'ELET3 -> List[Trade](
          Purchase("21/02/2017", 'ELET3, 500, 21.20)),
        'EMBR3 -> List[Trade](
          Purchase("28/07/2017", 'EMBR3, 600, 16.00),
          Sale("08/08/2017", 'EMBR3, 600, 16.67)),
        'ETER3 -> List[Trade](
          Purchase("06/01/2017", 'ETER3, 7200, 1.40),
          Sale("16/01/2017", 'ETER3, 7200, 1.45),
          Purchase("06/02/2017", 'ETER3, 6200, 1.50),
          Sale("13/02/2017", 'ETER3, 6200, 1.60),
          Purchase("17/02/2017", 'ETER3, 4600, 1.46),
          Purchase("03/03/2017", 'ETER3, 2400, 1.46)),
        'FIBR3 -> List[Trade](
          Purchase("21/11/2016", 'FIBR3, 300, 27.85),
          Sale("18/05/2017", 'FIBR3, 300, 32.55)),
        'GFSA3 -> List[Trade](
          Purchase("24/11/2016", 'GFSA3, 3600, 2.18),
          Sale("01/02/2017", 'GFSA3, 3600, 2.55)),
        'GGBR4 -> List[Trade](
          Purchase("03/05/2017", 'GGBR4, 700, 9.57),
          Sale("16/05/2017", 'GGBR4, 700, 10.17)),
        'GOLL4 -> List[Trade](
          Purchase("06/02/2017", 'GOLL4, 1600, 6.25),
          Sale("15/02/2017", 'GOLL4, 1600, 7.80),
          Purchase("22/02/2017", 'GOLL4, 1200, 8.39),
          Sale("11/04/2017", 'GOLL4, 1200, 10.24),
          Purchase("12/05/2017", 'GOLL4, 800, 10.20),
          Purchase("18/05/2017", 'GOLL4, 1400, 7.59),
          Sale("07/06/2017", 'GOLL4, 800, 8.35),
          Sale("28/08/2017", 'GOLL4, 800, 11.00),
          Sale("28/08/2017", 'GOLL4, 600, 11.00)),
        'HYPE3 -> List[Trade](
          Purchase("21/02/2017", 'HYPE3, 400, 27.20),
          Sale("17/03/2017", 'HYPE3, 400, 29.25)),
        'ITSA4 -> List[Trade](
          Purchase("23/11/2016", 'ITSA4, 900, 8.75),
          Sale("03/02/2017", 'ITSA4, 900, 9.50),
          Purchase("05/07/2017", 'ITSA4, 600, 9.05),
          Sale("22/08/2017", 'ITSA4, 600, 10.20)),
        'JBSS3 -> List[Trade](
          Purchase("22/11/2016", 'JBSS3, 1000, 9.83),
          Sale("01/02/2017", 'JBSS3, 1000, 12.35),
          Purchase("17/05/2017", 'JBSS3, 700, 9.60)),
        'JHSF3 -> List[Trade](
          Purchase("18/01/2017", 'JHSF3, 2800, 2.40),
          Sale("17/02/2017", 'JHSF3, 2800, 2.60)),
        'KLBN4 -> List[Trade](
          Purchase("30/11/2016", 'KLBN4, 3000, 2.75),
          Sale("29/12/2016", 'KLBN4, 3000, 2.86),
          Purchase("23/01/2017", 'KLBN4, 4000, 2.78),
          Sale("24/04/2017", 'KLBN4, 4000, 2.89)),
        'LAME4 -> List[Trade](
          Purchase("15/02/2017", 'LAME4, 700, 16.40),
          Sale("29/03/2017", 'LAME4, 700, 16.37),
          Purchase("06/07/2017", 'LAME4, 400, 13.96),
          Sale("27/07/2017", 'LAME4, 400, 15.50)),
        'LINX3 -> List[Trade](
          Purchase("14/02/2017", 'LINX3, 700, 18.24),
          Sale("10/05/2017", 'LINX3, 700, 19.00)),
        'LOGN3 -> List[Trade](
          Purchase("03/03/2017", 'LOGN3, 1100, 3.78)),
        'MRFG3 -> List[Trade](
          Purchase("21/02/2017", 'MRFG3, 1500, 6.91),
          Sale("02/05/2017", 'MRFG3, 1500, 7.48)),
        'PDGR3 -> List[Trade](
          Purchase("18/01/2017", 'PDGR3, 3200, 4.44)),
        'POMO4 -> List[Trade](
          Purchase("02/08/2017", 'POMO4, 2300, 3.33),
          Sale("21/08/2017", 'POMO4, 2300, 3.52)),
        'POSI3 -> List[Trade](
          Purchase("15/05/2017", 'POSI3, 2900, 3.59),
          Sale("19/07/2017", 'POSI3, 2900, 3.90)),
        'QGEP3 -> List[Trade](
          Purchase("21/02/2017", 'QGEP3, 1700, 6.00),
          Sale("06/03/2017", 'QGEP3, 1700, 6.51)),
        'RAIL3 -> List[Trade](
          Purchase("21/02/2017", 'RUMO3, 1000, 9.00),
          Sale("04/05/2017", 'RAIL3, 900, 9.50),
          Sale("09/05/2017", 'RAIL3, 100, 9.50)),
        'SAPR4 -> List[Trade](
          Purchase("14/02/2017", 'SAPR4, 700, 14.40)),
        'TAEE11 -> List[Trade](
          Purchase("22/11/2016", 'TAEE11, 400, 18.85),
          Sale("02/01/2017", 'TAEE11, 400, 20.90)),
        'TIET4 -> List[Trade](
          Purchase("17/03/2017", 'TIET4, 3800, 3.07)),
        'TPIS3 -> List[Trade](
          Purchase("05/01/2017", 'TPIS3, 2500, 3.19),
          Sale("12/01/2017", 'TPIS3, 2500, 3.31),
          Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
          Purchase("19/01/2017", 'TPIS3, 3500, 2.90),
          Sale("20/01/2017", 'TPIS3, 3500, 3.00),
          Sale("02/02/2017", 'TPIS3, 2600, 3.25),
          Purchase("07/02/2017", 'TPIS3, 2900, 3.10),
          Sale("15/02/2017", 'TPIS3, 2900, 3.30),
          Purchase("08/05/2017", 'TPIS3, 2400, 3.70),
          Purchase("12/05/2017", 'TPIS3, 2500, 3.47),
          Sale("18/07/2017", 'TPIS3, 2400, 3.90),
          Sale("18/07/2017", 'TPIS3, 2500, 3.90)),
        'USIM5 -> List[Trade](
          Purchase("12/12/2016", 'USIM5, 200, 3.85),
          Sale("04/01/2017", 'USIM5, 200, 4.55),
          Purchase("11/04/2017", 'USIM5, 3100, 3.95),
          Sale("15/05/2017", 'USIM5, 3100, 4.35)),
        'VALE5 -> List[Trade](
          Purchase("03/05/2017", 'VALE5, 300, 25.75),
          Sale("30/05/2017", 'VALE5, 300, 27.15)))
*/
    }
  }
  "we blend a purchase with the corresponding sale in a unique record to form a WholeTrade" - {
    "a purchase and an exhausting sale" - {
      assert(
        TradeMatcher.`match`(
          List[Trade](
            Purchase("09/08/2017", 'AMAR3, 1500, 6.75),
            Sale("28/08/2017", 'AMAR3, 1500, 7.12)
          )
        ) == (
          Purchase("09/08/2017", 'AMAR3, 1500, 6.75),
          Sale("28/08/2017", 'AMAR3, 1500, 7.12)
        )
      )
    }
    "a purchase and a partial sale" in {
      assert(
        TradeMatcher.`match`(
          List[Trade](
            Purchase("21/02/2017", 'RAIL3, 1000, 9.00),
            Sale("04/05/2017", 'RAIL3, 900, 9.50)
          )
        ) == PartialSale(
          Purchase("21/02/2017", 'RAIL3, 1000, 9.00),
          Sale("04/05/2017", 'RAIL3, 900, 9.50)
        )
      )
    }
    "a purchase, a change of symbol and a sale" ignore {
      assert(
        TradeMatcher.`match`(
          List[Trade](
            Purchase("21/02/2017", 'RUMO3, 1000, 9.00),
            Sale("09/05/2017", 'RAIL3, 1000, 9.50)
          )
        ) == (
          Purchase("21/02/2017", 'RUMO3, 1000, 9.00),
          Sale("09/05/2017", 'RAIL3, 1000, 9.50)
        )
      )
    }
    "two purchases and an exhausting sale" in {
      assert(
        TradeMatcher.`match`(
          List[Trade](
            Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
            Purchase("19/01/2017", 'TPIS3, 3500, 2.90),
            Sale("02/02/2017", 'TPIS3, 6100, 3.25)
          )
        ) == (
          List[Purchase](
            Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
            Purchase("19/01/2017", 'TPIS3, 3500, 2.90)
          ),
          List[Sale](
            Sale("02/02/2017", 'TPIS3, 2600, 3.25),
            Sale("02/02/2017", 'TPIS3, 3500, 3.25)
          )
        )
      )
    }
    "two purchases and a partial sale" in {
      assert(
        TradeMatcher.`match`(
          List[Trade](
            Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
            Purchase("19/01/2017", 'TPIS3, 3500, 2.90),
            Sale("02/02/2017", 'TPIS3, 3000, 3.25)
          )
        ) == (
          List[Purchase](
            Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
            Purchase("19/01/2017", 'TPIS3, 3500, 2.90)
          ),
          List[Sale](
            Sale("02/02/2017", 'TPIS3, 2600, 3.25),
            Sale("02/02/2017", 'TPIS3, 3500, 3.25)
          )
        )
      )
      'TPIS3 -> List[Trade](
        Purchase("05/01/2017", 'TPIS3, 2500, 3.19),
        Sale("12/01/2017", 'TPIS3, 2500, 3.31),
        Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
        Purchase("19/01/2017", 'TPIS3, 3500, 2.90))
    }
    "two purchases and two sales" in {
      assert(
        TradeMatcher.`match`(
          List[Trade](
            Purchase("17/01/2017", 'TPIS3, 2600, 3.10),
            Purchase("19/01/2017", 'TPIS3, 3500, 2.90),
            Sale("20/01/2017", 'TPIS3, 3500, 3.00),
            Sale("02/02/2017", 'TPIS3, 2600, 3.25)
          )
        ) == (
          List[Purchase](
            Purchase("19/01/2017", 'TPIS3, 3500, 2.90),
            Purchase("17/01/2017", 'TPIS3, 2600, 3.10)
          ),
          List[Sale](
            Sale("20/01/2017", 'TPIS3, 3500, 3.00),
            Sale("02/02/2017", 'TPIS3, 2600, 3.25)
          )
        )
      )
    }
  }
*/
}
