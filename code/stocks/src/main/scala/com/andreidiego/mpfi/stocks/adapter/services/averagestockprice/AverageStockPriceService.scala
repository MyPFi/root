package com.andreidiego.mpfi.stocks.adapter.services.averagestockprice

// TODO This will become a separate service soon
trait AverageStockPriceService:
  def forTicker(ticker: String): Double