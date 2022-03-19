package com.andreidiego.mpfi.stocks.adapter.spreadsheets

import excel.poi.Worksheet

class BrokerageNotesWorksheetReader(val brokerageNotes: Seq[BrokerageNote])

class BrokerageNote

object BrokerageNotesWorksheetReader:

  def from(worksheet: Worksheet): BrokerageNotesWorksheetReader = BrokerageNotesWorksheetReader(worksheet.groups.map(_ ⇒ BrokerageNote()))