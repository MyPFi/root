package com.andreidiego.mpfi.stocks.deprecated.quotes

object HTMLCleaner {

  /*
    import org.htmlcleaner.HtmlCleaner
    import org.htmlcleaner.PrettyXmlSerializer
    import java.net.URL

    val conn = new URL("https://homebroker.modalmais.com.br/hbnet2/hbweb2/Default.aspx").openConnection()

    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36")

    val cleaner = new HtmlCleaner
    val rootNode = cleaner clean conn.getInputStream

    new PrettyXmlSerializer(cleaner.getProperties).writeToFile(rootNode, "modal.xml", "utf-8")
  */

  /*
    val cotacoes = rootNode.getElementsByAttValue("class", "TableElement", true, false)(0).getElementListByName("tr", true)

    val test = cotacoes.map(dia => dia.getChildTagList.drop(5).take(2))
    1+1
  */
}
