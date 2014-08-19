package org.zachary.aws_repl

import java.io.{CharArrayWriter, PrintWriter}

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.sqs.AmazonSQSClient

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop

object Main extends App {

  def repl = new MainLoop(args)

  val settings = new Settings
  settings.Yreplsync.value = true
//  settings.Xnojline.value = true  // Turns off tab completion
  settings.deprecation.value = true

  def isRunFromSBT = {
    val c = new CharArrayWriter()
    new Exception().printStackTrace(new PrintWriter(c))
    c.toString.contains("at sbt.")
  }

  if (isRunFromSBT) {
    //an alternative to 'usejavacp' setting, when launching from within SBT
    settings.embeddedDefaults[Main.type]
  } else {
    //use when launching normally outside SBT
    settings.usejavacp.value = true
  }

  repl.process(settings)
}

class MainLoop(args: Array[String]) extends ILoop {
  val parser = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]("proxyPort") action { (x, c) => c.copy(proxyPort = Option(x))} optional()
    opt[String]("proxyHost") action { (x, c) => c.copy(proxyHost = Option(x))} optional()
  }

  private val configuration: ClientConfiguration = new ClientConfiguration

  parser.parse(args, Config()) map { config => {
    config.proxyHost.foreach(configuration.setProxyHost)
    config.proxyPort.foreach(configuration.setProxyPort)
  }}

  val s3 = new AmazonS3Client(configuration)
  val sqs = new AmazonSQSClient(configuration)

  override def loop(): Unit = {
    intp.bind("s3", s3.getClass.getCanonicalName, s3)
    intp.bind("sqs", sqs.getClass.getCanonicalName, sqs)
    super.loop()
  }

    addThunk {
      intp.beQuietDuring {
        intp.addImports("com.amazonaws.services.s3.AmazonS3Client")
        intp.addImports("com.amazonaws.services.sqs.AmazonSQSClient")
      }
    }
}

case class Config(proxyHost: Option[String] = None, proxyPort: Option[Int] = None)