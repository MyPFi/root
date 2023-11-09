package com.andreidiego.mpfi.stocks

import SingleTimeUnit.MINUTE
import TimeSpan.*
import org.scalatest.freespec.AnyFreeSpec

import java.time.{Clock, LocalDateTime, ZoneId}

class OutcomeTest extends AnyFreeSpec {

  "An outcome" - {
    "when requested with a time span, should" - {
      val REFERENCE_DATE_TIME: LocalDateTime = LocalDateTime.of(2017, 8, 23, 19, 23)
      val defaultZone: ZoneId = ZoneId.systemDefault
      implicit val FIXED_CLOCK: Clock = Clock.fixed(REFERENCE_DATE_TIME.atZone(defaultZone).toInstant, defaultZone)

      "tell the time span it was requested for" in {
        assert(Outcome.of(`this`(MINUTE)).timeSpan == Minute(23))
      }
/*
      "collect all the stock sales made within that time span" in {

        //        assert(Outcome.of(August).sellings == List[Sell] (pomo4, itsa4, dtex3))
        assert(
          Outcome.of(
            Purchase(LocalDateTime.of(9, 8, 2017, 17, 23), 'AMAR3, 1500, 6.75)
          ) == PurchaseResult(
            Purchase("09/08/2017", 'AMAR3, 1500, 6.75),
            volume = purchase.quantity * purchase.unitPrice,
            liquidationFee = purchase.volume * 0.0275 %,
            emoluments = purchase.volume * (if(tradeWithinMarketTime/*10:00 - 16:55*/) 0.005% else 0.007%),
            brokerageFee = Broker.brokerageFee,
            shareInServicePlan == 4,
            total = purchase.volume - purchase.liquidationFee - purchase.emoluments - purchase.brokerageFee - purchase.incomeTaxInTheSource - purchase.shareInServicePlan
          )
        )
        assert(
          Outcome.of(
            Sale("28/08/2017", 'AMAR3, 1500, 7.12)
          ) == SaleOutcome(
            Sale("28/08/2017", 'AMAR3, 1500, 7.12),
            volume = sale.quantity * sale.unitPrice,
            liquidationFee = sale.volume * 0.0275 %,
            emoluments = sale.volume * 0.005 %,
            brokerageFee = 4,
            incomeTaxInTheSource == 4,
            shareInServicePlan == 4,
            total = sale.volume - sale.liquidationFee - sale.emoluments - sale.brokerageFee - sale.incomeTaxInTheSource - sale.shareInServicePlan
          )
        )
        Outcome.of()
      }
      "Volume" in {
        assert(sale.volume == sale.quantity * sale.unitPrice)
      }
      "Liquidation Fee" in {
        assert(sale.liquidationFee == sale.volume * 0.0275 %)
      }
      "Emoluments" in {
        assert(sale.emoluments == sale.volume * 0.005 %)
      }
      "Brokerage Fee" in {
        assert(sale.brokerageFee == 4)
      }
      "IncomeTaxInTheSource" in {
        assert(sale.incomeTaxInTheSource == 4)
      }
      "Share in the Service Plan" in {
        assert(sale.shareInServicePlan == 4)
      }
      "Total" in {
        assert(sale.total == sale.volume - sale.liquidationFee - sale.emoluments - sale.brokerageFee)
      }
*/
    }
  }
}

case class Sell(localDateTime: LocalDateTime, symbol: Symbol, int: Int, double: Double)