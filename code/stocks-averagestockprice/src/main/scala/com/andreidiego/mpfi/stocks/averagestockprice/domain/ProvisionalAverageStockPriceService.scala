package com.andreidiego.mpfi.stocks.averagestockprice.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ProvisionalAverageStockPriceService extends AverageStockPriceService:
  enum OperationType:
    case BUYING, SELLING

  import OperationType.*

  private type TradingDate = LocalDate
  private type NoteNumber = Int
  private type Ticker = String
  private type Qty = Int
  private type Price = Double
  private type Volume = Double
  private type SettlementFee = Double
  private type NegotiationsFee = Double
  private type Brokerage = Double
  private type ServiceTax = Double
  private type IncomeTaxAtSource = Double
  private type Total = Double

  case class Operation(
    operationType: OperationType,
    tradingDate: TradingDate,
    noteNumber: NoteNumber,
    ticker: Ticker,
    qty: Qty,
    price: Price,
    volume: Volume,
    settlementFee: SettlementFee,
    negotiationsFee: NegotiationsFee,
    brokerage: Brokerage,
    serviceTax: ServiceTax,
    incomeTaxAtSource: IncomeTaxAtSource,
    total: Total
  )

  given Conversion[String, LocalDate] =
    LocalDate.parse(_, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

  private val operations: Seq[Operation] = Seq(
    Operation(BUYING, "05/11/2008", 1662, "VALE5", 100, 27.5, 2750.0, 0.22, 0.74, 15.99, 0.8, 0.0, 2766.95),
    Operation(BUYING, "27/04/2009", 1443, "VALE5", 200, 29.7, 5940.0, 0.47, 1.6, 15.99, 0.8, 0.0, 5958.06),
    Operation(SELLING, "04/05/2009", 2060, "VALE5", 200, 33.15, 6630.0, 0.39, 1.89, 15.99, 0.8, 0.29, 6611.73),
    Operation(BUYING, "11/05/2009", 1315, "VALE5", 200, 32.0, 6400.0, 0.38, 1.82, 15.99, 0.8, 0.0, 6418.19)
  )

  private val operationCost: Operation ⇒ Double = operation ⇒
    operation.volume - operation.settlementFee - operation.negotiationsFee - operation.brokerage - operation.serviceTax

  def forTicker(ticker: String): Double =
    val (totalCostForTicker, totalQtyForTicker) = operations
      .filter(_.ticker.equals(ticker))
      .foldLeft((0.0, 0)) { (acc: (Volume, Qty), operation: Operation) ⇒
        operation.operationType match
          case BUYING ⇒ (
            acc._1 + operationCost(operation), 
            acc._2 + operation.qty
          )
          case SELLING ⇒ (
              acc._1 - ((acc._1 / acc._2) * operation.qty),
              acc._2 - operation.qty
          )
      }
    if totalQtyForTicker > 0 then totalCostForTicker / totalQtyForTicker
    // TODO If the ticker can't be found, it means we don't have it on our portfolio so, just returning '0.0' is not the most appropriate thing going further. It should suffice for now, though.
    else 0.0