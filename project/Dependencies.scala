import sbt._
import org.scalajs.sbtplugin._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  val monocleVersion = "2.0.0"

  /** Dependencies that are jvm/js */
  val commonDependencies = Def.setting(Seq(
    "org.scalatest"          %%% "scalatest"    % "3.1.0-RC3" % "test", //% "3.2.0-SNAP10" % "test",
    "org.scala-js" %%% "scalajs-java-time" % "0.2.5",
    "org.scala-sbt" % "test-interface" % "1.0",
    "io.estatico" %% "newtype" % "0.4.3",
  ))

  val catsDependencies = Def.setting(Seq(
    "co.fs2" %%% "fs2-core" % "2.0.0",
    "org.typelevel" %%% "cats-core" % "2.0.0",
    "org.typelevel" %%% "cats-effect" % "2.0.0",
  ))

  val cliDependencies = Def.setting(Seq(
    //"com.definitelyscala" % "scala-js-xmldoc_sjs0.6_2.12" % "1.1.0",
    "com.github.julien-truffaut" %%%  "monocle-copre"  % monocleVersion,
    "com.github.julien-truffaut" %%%  "monocle-macro" % monocleVersion,
    "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
    "ru.pavkin" %%% "scala-js-momentjs" % "0.9.3",    
  ))

  val appDependencies = Def.setting(Seq(
    "ru.pavkin" %%% "scala-js-momentjs" % "0.9.3",
  ))

  val npmver = "0.4.2"

  /** js only libraries */
  val jsServerDependencies = Def.setting(Seq(
    "io.scalajs.npm" % "node-fetch_sjs0.6_2.12" % npmver,
    "io.scalajs"             % "nodejs_sjs0.6_2.12"      % npmver,
    "io.scalajs.npm" % "xml2js_sjs0.6_2.12" % npmver
  ))

  val commonScalacOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_", // maybe shrink this a little...
    "-unchecked",
    "-Ywarn-numeric-widen",
    "-Ymacro-annotations"
  )
}
