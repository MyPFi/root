package com.andreidiego.mpfi.stocks.averagestockprice.domain

// TODO This will become a separate service soon
trait AverageStockPriceService:
  def forTicker(ticker: String): Double