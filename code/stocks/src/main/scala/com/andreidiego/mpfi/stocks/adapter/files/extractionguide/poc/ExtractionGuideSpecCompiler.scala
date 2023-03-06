package com.andreidiego.mpfi.stocks.adapter.files.extractionguide.poc

/**
 * Full Grammar:
 *
 *  ExtractionGuide     ::= Instruction, { Instruction }, EOF
 *  Instruction         ::= RegularInstruction | RepetitionBlock
 *  RegularInstruction  ::= FieldName, SpacedArrow, MatrixCoordinate, SpacedArrow, FieldType, EOL
 *  FieldName           ::= PrintableCharacter, { PrintableCharacter }
 *  PrintableCharacter  ::= '\u0020' | … | '\u007E'
 *  SpacedArrow         ::= WhiteSpaces, ArrowSymbol, WhiteSpaces
 *  WhiteSpaces         ::= { WhiteSpace }
 *  WhiteSpace          ::= '\u0020' | '\u0009' | '\u000D' | '\u000A'
 *                        #   " "    |   '\t'   |   '\r'   |   '\n'
 *  ArrowSymbol         ::= "->"
 *  MatrixCoordinate    ::= LineSymbol, LineNumber, ColumnSymbol, ColumnNumber
 *  LineSymbol          ::= "L"
 *  LineNumber          ::= PositiveInteger
 *  PositiveInteger     ::= NonZeroDigit, { Digit }
 *  NonZeroDigit        ::= '1' | … | '9'
 *  Digit               ::= '0' | NonZeroDigit
 *  ColumnSymbol        ::= "C"
 *  ColumnNumber        ::= PositiveInteger
 *  FieldType           ::= "Number" | "Character" | "String" | "Date" | "Currency"
 *  EOL                 ::= WhiteSpaces, EOLSymbol
 *  EOLSymbol           ::= '\n'
 *  RepetitionBlock     ::= RepetitionMarker, RepeatedItem, { RepeatedItem }, [JumpLine], RepetitionMarker
 *  RepetitionMarker    ::= RepetitionSymbol, DashSymbol, LineSymbol, LineNumber, EllipsisSymbol, LineNumber, EOL
 *  RepetitionSymbol    ::= "R"
 *  DashSymbol          ::= "-"
 *  EllipsisSymbol      ::= "..."
 *  RepeatedItem        ::= FieldName, SpacedArrow, TokenCoordinate, SpacedArrow, FieldType, EOL
 *  TokenCoordinate     ::= FixedToken | RelativeToken
 *  FixedToken          ::= TokenSymbol, FixedTokenIndex
 *  TokenSymbol         ::= "T"
 *  FixedTokenIndex     ::= PositiveInteger
 *  RelativeToken       ::= TokenSymbol, OpenBracketSymbol, LineSymbol, DashSymbol, ??? AdjustingFactor ???, CloseBracketSymbol
 *  OpenBracketSymbol   ::= "("
 *  AdjustingFactor     ::= ???
 *  CloseBracketSymbol  ::= ")"
 *  JumpLine            ::= JumpLineSymbol, EOL
 *  JumpLineSymbol      ::= "JL"
 *  EOF                 ::= ???
 */

import scala.util.parsing.combinator.*

// FIXME Improve error reporting: e.g. Msg: 'R' expected but 'N' found
object ExtractionGuideSpecCompiler extends JavaTokenParsers:
  import java.nio.charset.StandardCharsets
  import scala.annotation.experimental
  import FieldType.*
  import FieldName.*
  
  override def skipWhitespace: Boolean = false

  private lazy val space                                  = "\u0020" // ' '
  private lazy val horizontalTab                          = "\u0009" // '\t'
  private lazy val carriageReturn                         = "\u000D" // '\r'
  private lazy val lineFeed                               = "\u000A" // '\n'
  private lazy val lowercaseUnaccentedLatinLetter         = """[\u0061-\u007A]"""
  private lazy val uppercaseUnaccentedLatinLetter         = """[\u0041-\u005A]"""
  private lazy val lowercaseAccentedLatinLetter           = """[\u00DE-\u00F6\u00F8-\u00FF]"""
  private lazy val uppercaseAccentedLatinLetter           = """[\u00C0-\u00D6\u00D8-\u00DE]"""
  private lazy val asciiPunctuationAndSymbols             = """[\u0020-\u002F\u003A-\u0040\\u005B-\u0060\u007B-\u007E]"""
  private lazy val latin1PunctuationAndSymbols            = """[\u00A0-\u00BF]"""
  private lazy val asciiDigits                            = """[\u0030-\u0039]"""
  private lazy val restrictedASCIIPunctuationAndSymbols   = """[\u0021-\u002C\u002E\u002F\u003A-\u0040\\u005B-\u0060\u007B-\u007E]"""
  private lazy val dashNotFollowedByArrowHead             = """-(?!>)"""
  private lazy val spaceNotFollowedBySpacedArrow          = s"""$space(?![$space$horizontalTab]*->)"""

  private lazy val arrowSymbol                            = "->"
  private lazy val lineSymbol                             = "L"
  private lazy val columnSymbol                           = "C"
  private lazy val eolSymbol                              = s"$carriageReturn$lineFeed"
  private lazy val repetitionSymbol                       = "R"
  private lazy val dashSymbol                             = "-"
  private lazy val ellipsisSymbol                         = "..."
  private lazy val tokenSymbol                            = "T"
  private lazy val openBracketSymbol                      = "("
  private lazy val closeBracketSymbol                     = ")"
  private lazy val jumpLineSymbol                         = "JL"

  private lazy val printableCharacterNotFollowedByAnArrow = s"""$lowercaseUnaccentedLatinLetter|$uppercaseUnaccentedLatinLetter|
                                                                #$lowercaseAccentedLatinLetter|$uppercaseAccentedLatinLetter|
                                                                #$restrictedASCIIPunctuationAndSymbols|$latin1PunctuationAndSymbols|
                                                                #$asciiDigits|$dashNotFollowedByArrowHead|$spaceNotFollowedBySpacedArrow
                                                            #""".stripMargin('#')
                                                              .replaceAll(s"[$carriageReturn$lineFeed]", "")
                                                              .r
  private val nonZeroDigit                                = """[1-9]""".r
  private val eof                                         = "$".r

  @experimental lazy val extractionGuideCompiler: Parser[ExtractionGuide] = rep1(instruction) ~ eof ^^ {
    case instructions ~ _ ⇒ ExtractionGuide(instructions)
  }
  private lazy val instruction          = regularInstruction | repetitionBlock
  private lazy val regularInstruction   = fieldName ~ spacedArrow ~ matrixCoordinate ~ spacedArrow ~ fieldType ~ (eol | eof) ^^ {
    case fieldName ~ _ ~ matrixCoordinate ~ _ ~ columnType ~ _ ⇒ RegularInstruction(fieldName, matrixCoordinate, columnType)
  } //  private lazy val fieldName      = rep1(printableCharacterNotFollowedByAnArrow) ^^ { (a: List[_]) ⇒ FieldName(a.mkString) }  
  private lazy val fieldName            = "NoteNumber" ^^^ NoteNumber | "TradingDate" ^^^ TradingDate 
                                        | "BankInstitutionNumber" ^^^ BankInstitutionNumber 
                                        | "TransitNumber" ^^^ TransitNumber | "AccountNumber" ^^^ AccountNumber 
                                        | "OperationType" ^^^ OperationType | "Ticker" ^^^ Ticker | "Qty" ^^^ Qty 
                                        | "Price" ^^^ Price | "Volume" ^^^ Volume | "SellingsTotal" ^^^ SellingsTotal 
                                        | "BuyingsTotal" ^^^ BuyingsTotal | "OperationsTotal" ^^^ OperationsTotal 
                                        | "SettlementFee" ^^^ SettlementFee | "TradingFees" ^^^ TradingFees 
                                        | "Brokerage" ^^^ Brokerage | "ServiceTax" ^^^ ServiceTax 
                                        | "IncomeTaxAtSource" ^^^ IncomeTaxAtSource | "Total" ^^^ Total 
                                        | "SettlementDate" ^^^ SettlementDate | failure(
                                          """Couldn't find any of the allowed field names:
                                          | - NoteNumber;
                                          | - TradingDate;
                                          | - BankInstitutionNumber;
                                          | - TransitNumber;
                                          | - AccountNumber;
                                          | - OperationType;
                                          | - Ticker;
                                          | - Qty;
                                          | - Price;
                                          | - Volume;
                                          | - SellingsTotal;
                                          | - BuyingsTotal;
                                          | - OperationsTotal;
                                          | - SettlementFee;
                                          | - TradingFees;
                                          | - Brokerage;
                                          | - ServiceTax;
                                          | - IncomeTaxAtSource;
                                          | - Total;
                                          | - SettlementDate;
                                          |""".stripMargin
                                        )
  private lazy val spacedArrow          = whiteSpaces ~ arrowSymbol ~ whiteSpaces
  private lazy val whiteSpaces          = rep(whitespace)
  private lazy val whitespace           = space | horizontalTab | carriageReturn | lineFeed
  private lazy val matrixCoordinate     = lineSymbol ~ lineNumber ~ columnSymbol ~ columnNumber ^^ {
    case _ ~ lineNumber ~ _ ~ columnNumber ⇒ MatrixCoordinate(lineNumber, columnNumber)
  }
  private lazy val lineNumber           = positiveInteger ^^ LineNumber
  private lazy val positiveInteger      = nonZeroDigit ~ rep(digit) ^^ {
    case nonZeroDigit ~ digits ⇒ PositiveInteger(s"$nonZeroDigit${digits.mkString}".toInt)
  }
  private lazy val digit                = "0" | nonZeroDigit
  private lazy val columnNumber         = positiveInteger ^^ ColumnNumber
  private lazy val fieldType            = "Number" ^^^ Number | "Character" ^^^ Character | "String" ^^^ String
    | "Date" ^^^ Date | "Currency" ^^^ Currency
  private val eol                       = rep(space) ~ eolSymbol

  private lazy val repetitionBlock      = repetitionMarker ~ rep1(repeatedItem) ~ opt(jumpLine) ~ repetitionMarker ^^ {
    case repetitionMarker ~ repeatedItems ~ jumpLine ~ _ ⇒ RepetitionBlock(repetitionMarker, repeatedItems, jumpLine.isDefined)
  }
  private lazy val repetitionMarker     = repetitionSymbol ~ dashSymbol ~ lineSymbol ~ lineNumber ~ ellipsisSymbol ~ lineNumber ~ (eol | eof) ^^ {
    case _ ~ _ ~ _ ~ start ~ _ ~ finish ~ _ ⇒ LineRange(start, finish)
  }
  private lazy val repeatedItem         = fieldName ~ spacedArrow ~ tokenCoordinate ~ spacedArrow ~ fieldType ~ eol ^^ {
    case fieldName ~ _ ~ tokenCoordinate ~ _ ~ columnType ~ _ ⇒ RepeatedItem(fieldName, tokenCoordinate, columnType)
  }
  private lazy val tokenCoordinate      = fixedToken | relativeToken
  private lazy val fixedToken           = tokenSymbol ~ fixedTokenIndex ^^ { case _ ~ index ⇒ FixedTokenCoordinate(index) }
  private lazy val fixedTokenIndex      = positiveInteger
  private lazy val relativeToken        = tokenSymbol ~ openBracketSymbol ~ lineSymbol ~ dashSymbol ~ adjustingFactor ~ closeBracketSymbol ^^ {
    case _ ~ _ ~ lineSymbol ~ dashSymbol ~ adjustingFactor ~ _ ⇒ RelativeTokenCoordinate(lineSymbol, dashSymbol, adjustingFactor)
  }
  private lazy val adjustingFactor      = positiveInteger
  private lazy val jumpLine             = jumpLineSymbol ~ eol ^^ { case jumpLineSymbol ~ _ ⇒ jumpLineSymbol }