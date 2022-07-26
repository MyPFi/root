package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path

trait FileSystem[F[_]]:
  def createFile(path: Path): F[Unit]
  def createFolder(path: Path): F[Unit]
  def delete(path: Path, force: Boolean = false): F[Unit]
  def exists(path: Path): F[Boolean]
  def isAFile(path: Path): F[Boolean]
  def isAFolder(path: Path): F[Boolean]

object FileSystem:
  def apply[F[_]](using F: FileSystem[F]): FileSystem[F] = F

// TODO Add a Production instance of FileSystem and test it using ScalaMock
// TODO Look into introducing laws and law testing with Discipline