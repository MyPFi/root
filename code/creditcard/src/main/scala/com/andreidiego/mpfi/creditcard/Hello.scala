package com.andreidiego.mpfi.creditcard

import org.apache.pdfbox.Loader.loadPDF
import org.apache.pdfbox.text.PDFTextStripper

import java.io.{File, PrintWriter}

object Hello extends App {
  val pd = loadPDF(
    new File("F:\\OneDrive\\Documentos\\Financeiros\\Investimentos\\Bolsa\\Notas de Corretagem\\2021\\compraITUB4-MGLU3 - 6049 - 04-10-2021 (2).pdf")
  )

  val text = new PDFTextStripper getText pd

  val pw1 = new PrintWriter(new File("src/main/resources/stockmarket/hello1.txt"))
  val pw2 = new PrintWriter(new File("src/main/resources/stockmarket/hello2.txt"))

  // val textTuple = text.partition(text.indexOf(_) > text.indexOf("Histórico das Despesas"))

  val index = text.indexOf("Histórico das Despesas")

  println(index)

  pw1.write(text)
  //  pw1.write(text.substring(0, index))
  //  pw2.write(text.substring(index))

  /*
  Scanner sc = new Scanner(text.substring(index));

  s.findInLine("(\\d+) fish (\\d+) fish (\\w+) fish (\\w+)");

  MatchResult result = s.match();

  for (int i=1; i<=result.groupCount(); i++)
        System.out.println(result.group(i));

  s.close();
  Invoice Record:
    (\d{2}/\d{2}) ([\w\s\-\.]+) (-?\d*?\.?\d{1,3},\d{2})
    08/10 PIEGEL PAES CONVENIENC 25,18

  Histórico das Despesas
  ANDREI D CARDOSO
  Transações Nacionais
  MARCIA O P CARDOSO
  6206
  Data Descrição R$ US$ Data Descrição R$ US$
  SANTANDER FREE
  Nº DO CARTÃO 4415 XXXX XXXX 5466 | VISA  4/4
  BANCO SANTANDER (BRASIL) S.A. – CNPJ/MF90.400.888/0001-42 – AVENIDA PRESIDENTE JUSCELINO
  KUBITSCHEK, 2.041 E 2.235, BLOCO A, VILA OLÍMPIA, SÃO PAULO-SP
  */
  pd.close()
  pw1.close()
  pw2.close()
}
