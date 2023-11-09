package com.andreidiego.mpfi.stocks

object Outcome {
  def of(timeSpan: TimeSpan): Outcome = {
    val outcome = new Outcome
    outcome.timeSpan = timeSpan
    outcome
  }
}

class Outcome {

//  TODO Restrict the modifying access to only the companion object
  var timeSpan: TimeSpan = _
}