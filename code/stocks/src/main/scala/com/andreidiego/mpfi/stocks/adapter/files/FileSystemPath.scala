package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path
import scala.annotation.experimental

enum FileSystemPathException(message: String) extends Exception(message):
  case RequiredValueMissingException(message: String) extends FileSystemPathException(message)
  case UnexpectedContentValueException(message: String) extends FileSystemPathException(message)

@experimental class FileSystemPath(path: String):
  import java.nio.file.Files
  import FileSystemPath.*
  
  def exists: Boolean = Files.exists(Path.of(path))
  
  def doesNotExist: Boolean = !exists
  
  def isAFile: Boolean = 
    Files.isRegularFile(Path.of(path)) || path.hasExtension || path.doesNotEndWithSlash
  
  def isNotAFile: Boolean = !isAFile
  
  def isAFolder: Boolean = Files.isDirectory(Path.of(path)) || path.endsWithSlash
  
  def isNotAFolder: Boolean = !isAFolder

@experimental object FileSystemPath:
  import java.nio.file.InvalidPathException
  import language.experimental.saferExceptions
  import FileSystemPathMessages.*
  import FileSystemPathException.*
	
  def from(path: String): FileSystemPath throws FileSystemPathException = 
    if path.isBlank() then 
      throw RequiredValueMissingException(fileSystemPathMissing) 
    else 
      val validatedPath = try Path.of(path) 
      catch case ex: InvalidPathException => throw UnexpectedContentValueException(invalidFileSystemPath(path))
      
      if !validatedPath.isAbsolute() then
        throw UnexpectedContentValueException(relativeFileSystemPathNotAllowed(path))
      else FileSystemPath(path)

  def fileExtensionRegex = """\.[^.\\/:*?"<>|\r\n]+$$""".r
  def folderRegex = """[\\/]$$""".r

  extension(path: String)
    private def hasExtension: Boolean = fileExtensionRegex.findFirstIn(path).isDefined
    private def endsWithSlash: Boolean = folderRegex.findFirstIn(path).isDefined
    private def doesNotEndWithSlash: Boolean = !endsWithSlash

object FileSystemPathMessages:
  val fileSystemPathMissing = "Path cannot be empty."
  val invalidFileSystemPath: String => String = path => s"$path does not represent a valid filesytem path."
  val relativeFileSystemPathNotAllowed: String => String = path => s"Relative filesystem paths (like '$path') are not allowed."