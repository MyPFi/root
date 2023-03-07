package com.andreidiego.mpfi.stocks.adapter.files.readers.pdf.poc

import scala.annotation.experimental
import scala.util.Try
import com.andreidiego.mpfi.stocks.adapter.files
import files.FileSystemPath

// FIXME Revisit the relationship between contents, children and Sorters and try to make it more type safe, if possible
@experimental class PDFBrokerageNotesPath[F[_]] private(path: String) extends FileSystemPath[F](path: String):
  import scala.unsafeExceptions.canThrowAny
  import cats.syntax.functor.*
  import cats.syntax.traverse.*
  import cats.instances.lazyList.*
  import FileSystemPath.InteractsWithTheFileSystemAndReturns

  def children(sortedWith: (String, String) ⇒ Boolean = (_, _) ⇒ false): InteractsWithTheFileSystemAndReturns[LazyList[PDFBrokerageNotePath[F]]][F] =
    contents.map { tentativeContents ⇒
      tentativeContents
        .flatMap { contents ⇒
          contents
            .filter(path ⇒ Try(PDFBrokerageNotePath.from[F](path)).isSuccess)
            .sortWith(sortedWith)
            .map { path =>
              println(s"PDFBrokerageNotesPath.children: $path")
              Try(PDFBrokerageNotePath.from[F](path))
            }.sequence
        }.get
    }

@experimental object PDFBrokerageNotesPath:
  import java.nio.file.Path
  import scala.util.{Success, Failure}
  import scala.unsafeExceptions.canThrowAny
  import cats.Monad
  import cats.syntax.apply.*
  import cats.syntax.flatMap.*
  import files.FileSystem
  import FileSystemPath.InteractsWithTheFileSystemAndReturns
  import FileSystemPath.Exceptions.UnexpectedContentValue
  import Messages.*

  def from[F[_]](path: String): InteractsWithTheFileSystemAndReturns[Try[PDFBrokerageNotesPath[F]]][F] =
    try
      val fileSystemPath = FileSystemPath.from[F](path)

      (fileSystemPath.doesNotExist, fileSystemPath.isNotAFolder, fileSystemPath.contents).mapN { (doesNotExist, notAFolder, contents) ⇒
        if doesNotExist then
          Failure(FileSystem.Exception.ResourceNotFound(Path.of(path)))
        else if notAFolder then
          Failure(UnexpectedContentValue(filePathNotAllowed(path)))
        else contents.flatMap { contents ⇒
          if contents.isEmpty then
            Failure(UnexpectedContentValue(emptyFolderNotAllowed(path)))
          else if contents.hasNoPDFBrokerageNotePath[F] then
            Failure(UnexpectedContentValue(noPDFBrokerageNotePathFoundIn(path)))
          else
            Success(PDFBrokerageNotesPath(path))
        }
      }
    catch case ex ⇒ summon[Monad[F]].pure(Failure(ex))

  object Sorters:
    import java.util.regex.Pattern
    import FileSystemPath.Sorter

    extension (string: String)
      private def pathSegments: Array[String] = string.split(s"[/${Pattern.quote("\\")}]")
      private def fileName: Array[String] = pathSegments(pathSegments.length - 1).split("- | - | -")
      private def operationsDescription: String = fileName(0)
      private def noteNumber: String = fileName(1)
      private def tradingDate: String = fileName(2).replaceFirst(raw"\.[a-zA-Z0-9]{2,4}", "")

    val fullFilenameSorter: Sorter = (line1, line2) ⇒ {
      extension (string: String)
        private def fileName: String = string.pathSegments(string.pathSegments.length - 1) //.split("- | - | -")
      line1.fileName.compareTo(line2.fileName) < 0
    }
    val operationsDescriptionSorter: Sorter = (line1, line2) ⇒ line1.operationsDescription.compareTo(line2.operationsDescription) < 0
    val noteNumberSorter: Sorter = (line1, line2) ⇒ line1.noteNumber.compareTo(line2.noteNumber) < 0
    val tradingDateSorter: Sorter = (line1, line2) ⇒ line1.tradingDate.compareTo(line2.tradingDate) < 0

  object Messages:
    val filePathNotAllowed: String ⇒ String =
      path ⇒ s"A 'PDFBrokerageNotesPath' represents a folder. It cannot be created from a file path as ${Path.of(path)}."
    val emptyFolderNotAllowed: String ⇒ String =
      path ⇒ s"A 'PDFBrokerageNotesPath' cannot be created from a path representing a folder that is empty, like ${Path.of(path)}."
    val noPDFBrokerageNotePathFoundIn: String ⇒ String =
      path ⇒ s"A 'PDFBrokerageNotesPath' cannot be created from a path representing a folder that, while non-empty, contains no valid 'PDFBrokerageNotePath', like ${Path.of(path)}."

  extension (contents: LazyList[String])
    private def hasNoPDFBrokerageNotePath[F[_]]: Boolean =
      !contents.exists(line ⇒ Try(PDFBrokerageNotePath.from[F](line)).isSuccess)