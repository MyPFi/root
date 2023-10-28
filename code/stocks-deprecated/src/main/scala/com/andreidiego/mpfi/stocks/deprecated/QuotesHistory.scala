package com.andreidiego.mpfi.stocks.deprecated

import java.io.File

class QuotesHistory(fromFilesInDirectory: String) {

  buildHistoryFrom(new File(fromFilesInDirectory))

  def buildHistoryFrom(directory: File): List[File] = {
    
    if (directory.exists && directory.isDirectory) {

      directory.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }
}