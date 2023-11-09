package com.andreidiego.mpfi.stocks

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec

class QuotesHistoryTest extends AnyFlatSpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = {

  }

  override def afterEach(): Unit = {

  }

  "An empty Set" should "have size 0" in {
    assert(Set.empty.size == 0)
  }

  it should "produce NoSuchElementException when head is invoked" in {
    assertThrows[NoSuchElementException] {
      Set.empty.head
    }
  }
}
