package com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.filewatcher

import com.sun.jna.platform.win32.{Kernel32, WinBase}
import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import org.slf4j.{Logger, LoggerFactory}

class NamedPipeServer:
  private val log: Logger = LoggerFactory.getLogger(getClass)
  private val pipeName = "\\\\.\\pipe\\MyAppPipe"

  def start(process: String ⇒ Unit): Unit =
    log.debug("Starting NamedPipeServer...")
    
    while (true) {
      val handle = Kernel32.INSTANCE.CreateNamedPipe(
        pipeName,
        WinBase.PIPE_ACCESS_DUPLEX,
        WinBase.PIPE_TYPE_MESSAGE | WinBase.PIPE_READMODE_MESSAGE | WinBase.PIPE_WAIT,
        WinBase.PIPE_UNLIMITED_INSTANCES,
        1024,
        1024,
        0,
        null
      )

      if (handle == WinBase.INVALID_HANDLE_VALUE) {
        log.debug("Error: CreateNamedPipe failed")
        return
      }

      val connected = Kernel32.INSTANCE.ConnectNamedPipe(handle, null)
      if (!connected) {
        log.debug("Error: ConnectNamedPipe failed")
        return
      }

      val reader = new BufferedReader(
        new InputStreamReader(
          null,
//          new WinBase.HandleIn(handle),
          StandardCharsets.UTF_8
        )
      )

      val message = reader.readLine()
      process(message)

      reader.close()
      Kernel32.INSTANCE.DisconnectNamedPipe(handle)
    }
    log.debug("NamedPipeServer Started.")