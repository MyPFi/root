package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path
import scala.util.Try

trait FileSystem[F[_]]:
  def createFile(path: Path, content: String): F[Try[Path]]
  def createFolder(path: Path): F[Try[Path]]
  def delete(path: Path, force: Boolean = false): F[Try[Path]]
  def exists(path: Path): F[Boolean]
  def isAFile(path: Path): F[Boolean]
  def isAFolder(path: Path): F[Boolean]
  def contentsOf(path: Path): F[Try[LazyList[String]]]

object FileSystem:
  import java.lang.Exception as JavaException

  enum Exception(message: String) extends JavaException(message):
    case ResourceNotFound(resource: Path)
    extends Exception(s"Resource '$resource' cannot be found.")

  def apply[F[_]](using F: FileSystem[F]): FileSystem[F] = F

// TODO Add a Production instance of FileSystem and test it using ScalaMock
// TODO Look into introducing laws and law testing with Discipline