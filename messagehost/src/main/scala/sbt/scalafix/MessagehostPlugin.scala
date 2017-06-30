package sbt
package scalafix

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.collection.mutable
import sbt.Keys._
import xsbti.Problem
import xsbti.Position
import xsbti.Reporter
import xsbti.Severity

// An sbt plugin that produces .semanticdb files with only messages.
object MessagehostPlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(reporterSettings) ++
      inConfig(Test)(reporterSettings)

  private val reporterSettings = Seq(
    compilerReporter in compile := {
      val logger = streams.value.log
      val parent = compilerReporter
        .in(compile)
        .value
        .orElse(Some(new LoggerReporter(10, logger)))
      val sourceRoot = baseDirectory.value.toPath
      val targetRoot = classDirectory.value.toPath
      Some(new MessagehostReporter(logger, parent, sourceRoot, targetRoot))
    }
  )
}

class MessagehostReporter(logger: Logger,
                          parent: Option[Reporter],
                          sourceRoot: Path,
                          targetRoot: Path)
    extends Reporter {
  import scala.meta.internal.semantic.schema._
  private val messages = mutable.HashMap.empty[Path, Attributes]

  def database: Database = Database(messages.values.toSeq)
  override def log(pos: Position, msg: String, sev: Severity): Unit = {
    parent.foreach(_.log(pos, msg, sev))
    if (pos.sourceFile().isDefined) {
      val path = sourceRoot.relativize(pos.sourceFile().get().toPath)
      val attrs =
        messages.getOrElseUpdate(path, Attributes(filename = path.toString))
      val range =
        if (pos.offset().isDefined)
          Some(Range(pos.offset().get(), pos.offset().get()))
        else None
      val severity =
        if (sev == Severity.Info) Message.Severity.INFO
        else if (sev == Severity.Warn) Message.Severity.WARNING
        else Message.Severity.ERROR
      val message = Message(range, severity, msg)
      messages(path) = attrs.copy(messages = message +: attrs.messages)
    }
  }
  override def reset(): Unit = {
    parent.foreach(_.reset())
    messages.clear()
  }

  override def printSummary(): Unit = {
    parent.foreach(_.printSummary())
    val root = targetRoot.resolve("META-INF").resolve("semanticdb")
    database.entries.foreach { attrs =>
      val out = root.resolve(Paths.get(attrs.filename + ".semanticdb"))
      out.getParent.toFile.mkdirs()
      Files.write(out, attrs.toByteArray)
    }
  }

  // Forwarding methods.
  override def hasWarnings: Boolean = parent.exists(_.hasWarnings)
  override def hasErrors: Boolean = parent.exists(_.hasErrors)
  override def comment(pos: Position, msg: String): Unit =
    parent.foreach(_.comment(pos, msg))
  override def problems(): Array[Problem] =
    parent.map(_.problems()).getOrElse(Array.empty)
}
