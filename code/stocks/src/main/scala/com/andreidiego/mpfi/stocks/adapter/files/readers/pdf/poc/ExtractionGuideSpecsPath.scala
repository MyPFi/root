package com.andreidiego.mpfi.stocks.adapter.files.readers.pdf.poc

import scala.annotation.experimental
import scala.util.Try
import com.andreidiego.mpfi.stocks.adapter.files
import files.FileSystemPath
import files.extractionguide.poc.ExtractionGuideSpecPath

// FIXME Revisit the relationship between contents, children and Sorters and try to make it more type safe, if possible
@experimental class ExtractionGuideSpecsPath[F[_]] private(path: String) extends FileSystemPath[F](path: String):
  import scala.unsafeExceptions.canThrowAny
  import cats.syntax.functor.*
  import cats.syntax.traverse.*
  import cats.instances.lazyList.*
  import FileSystemPath.InteractsWithTheFileSystemAndReturns

  def children(sortedWith: (String, String) ⇒ Boolean = (_, _) ⇒ false): InteractsWithTheFileSystemAndReturns[LazyList[ExtractionGuideSpecPath[F]]][F] =
    contents(sortedWith).map { tentativeContents ⇒
      tentativeContents.flatMap { contents ⇒
        contents.map { path =>
          println(s"ExtractionGuideSpecsPath.children: $path")
          Try(ExtractionGuideSpecPath.from[F](path))
        }.filter(e ⇒ e.isSuccess).sequence
      }.get
    }

object ExtractionGuideSpecsPath:
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

  def from[F[_]](path: String): InteractsWithTheFileSystemAndReturns[Try[ExtractionGuideSpecsPath[F]]][F] =
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
          else if contents.hasNoExtractionGuideSpecPath[F] then
            Failure(UnexpectedContentValue(noExtractionGuideSpecPathFoundIn(path)))
          else
            Success(ExtractionGuideSpecsPath(path))
        }
      }
    catch case ex ⇒ summon[Monad[F]].pure(Failure(ex))

  object Sorters:
    import java.util.regex.Pattern
    import FileSystemPath.Sorter

    val fullFilenameSorter: Sorter = (path1, path2) ⇒ {
      extension (path: String)
        private def fileName: String = path.pathSegments(path.pathSegments.length - 1)
      path1.fileName.compareTo(path2.fileName) < 0
    }
    val documentTypeSorter: Sorter = (path1, path2) ⇒ path1.documentType.compareTo(path2.documentType) < 0
    val documentIssuerSorter: Sorter = (path1, path2) ⇒ path1.documentIssuer.compareTo(path2.documentIssuer) < 0
    val documentVersionSorter: Sorter = (path1, path2) ⇒
      path1.documentVersion.replaceFirst("[vV]", "").toInt.compareTo(
        path2.documentVersion.replaceFirst("[vV]", "").toInt
      ) < 0

    extension (path: String)
      private def pathSegments: Array[String] = path.split(s"[/${Pattern.quote("\\")}]")
      private def fileName: Array[String] = pathSegments(pathSegments.length - 1).split("-")
      private def documentType: String = fileName(0)
      private def documentIssuer: String = fileName(1)
      private def documentVersion: String = fileName(2).replaceFirst(raw"\.[a-zA-Z0-9]{2,4}", "")

  object Messages:
    val filePathNotAllowed: String ⇒ String =
      path ⇒ s"A 'ExtractionGuideSpecsPath' represents a folder. It cannot be created from a file path as ${Path.of(path)}."
    val emptyFolderNotAllowed: String ⇒ String =
      path ⇒ s"A 'ExtractionGuideSpecsPath' cannot be created from a path representing a folder that is empty, like ${Path.of(path)}."
    val noExtractionGuideSpecPathFoundIn: String ⇒ String =
      path ⇒ s"A 'ExtractionGuideSpecsPath' cannot be created from a path representing a folder that, while non-empty, contains no valid 'ExtractionGuideSpecPath', like ${Path.of(path)}."

  extension (contents: LazyList[String])
    private def hasNoExtractionGuideSpecPath[F[_]]: Boolean =
      !contents.exists(line ⇒ Try(ExtractionGuideSpecPath.from[F](line)).isSuccess)