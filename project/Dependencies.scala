import sbt._
import org.scalajs.sbtplugin._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val monocleVersion = "1.5.0-cats"

  /** Dependencies that are jvm/js */
  val commonDependencies = Def.setting(Seq(

    "org.scalatest"          %%% "scalatest"    % "3.0.5" % "test", //% "3.2.0-SNAP10" % "test",
    "co.fs2" %%% "fs2-core" % "1.0.0",
    "org.typelevel" %%% "cats-core" % "1.5.0",
    "org.typelevel" %%% "cats-effect" % "1.0.0",
    "org.scala-js" %%% "scalajs-java-time" % "latest.version",
    "org.scala-sbt" % "test-interface" % "1.0",
    "io.estatico" %% "newtype" % "0.4.2",
  ))

  val cliDependencies = Def.setting(Seq(
    "com.definitelyscala" % "scala-js-xmldoc_sjs0.6_2.12" % "1.1.0",
    "com.github.julien-truffaut" %%%  "monocle-core"  % monocleVersion,
    "com.github.julien-truffaut" %%%  "monocle-macro" % monocleVersion,
    "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
    "ru.pavkin" %%% "scala-js-momentjs" % "0.9.2",    
  ))

  val appDependencies = Def.setting(Seq(
    "ru.pavkin" %%% "scala-js-momentjs" % "0.9.1",
  ))

  val npmver = "0.4.2"

  /** js only libraries */
  val myJSDependencies = Def.setting(Seq(
    "io.scalajs.npm" %%% "node-fetch" % npmver,
    "io.scalajs"             %%% "nodejs"      % npmver,
    "io.scalajs.npm" %%% "xml2js" % npmver
  ))

  val commonScalacOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_", // maybe shrink this a little...
    "-unchecked",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Xfuture",
    "-Ypartial-unification"
  )
}
