import scala.sys.process._

resolvers in ThisBuild += Resolver.sonatypeRepo("releases")
resolvers in ThisBuild += Resolver.sonatypeRepo("snapshots")
resolvers in ThisBuild += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers in ThisBuild += Resolver.typesafeRepo("snapshots")
//resolvers += Resolver.bintrayRepo("softprops", "maven") // for retry, what else?
resolvers in ThisBuild += Resolver.bintrayRepo("scalameta", "maven") // for latset scalafmt
resolvers in ThisBuild += Resolver.bintrayRepo("definitelyscala", "maven") // for latest facades
resolvers in ThisBuild += Resolver.jcenterRepo

autoCompilerPlugins := true

lazy val licenseSettings = Seq(
  headerMappings := headerMappings.value +
    (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    headerLicense  := Some(HeaderLicense.Custom(
      """|Copyright (c) 2018 The Trapelo Group LLC
         |This software is licensed under the MIT License (MIT).
         |For more information see LICENSE or https://opensource.org/licenses/MIT
         |""".stripMargin
    )))

lazy val buildSettings = Seq(
  organization := "ttg",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.12.7",
  scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true))),
) ++ licenseSettings

lazy val noPublishSettings = Seq(
  skip in publish := true,
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/aappddeevv/odata-client"))
)

lazy val commonSettings = Seq(
  autoAPIMappings := true,
  scalacOptions ++=
    Dependencies.commonScalacOptions ++
    (if (scalaJSVersion.startsWith("0.6."))
      Seq("-P:scalajs:sjsDefinedByDefault")
    else Nil),
  libraryDependencies ++=
    (Dependencies.commonDependencies.value ++
      Dependencies.myJSDependencies.value),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
)

lazy val dynamicsSettings = buildSettings ++ commonSettings

lazy val root = project.in(file("."))
  .settings(dynamicsSettings)
  .settings(noPublishSettings)
  .settings(name := "odata-client")
  .aggregate(http, client, clientcommon, `scalajs-common`, docs, adal, `node-fetch`, `browser-fetch`)
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)

lazy val `scalajs-common` = project
  .settings(dynamicsSettings)
  .settings(description := "Common components")
  .settings(name := "scalajs-common")
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)

lazy val clientcommon = project
  .settings(dynamicsSettings)
  .settings(description := "Common client components")
  .settings(name := "odata-client-client-common")
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)
  .dependsOn(`scalajs-common`, http)

lazy val http = project
  .settings(dynamicsSettings)
  .settings(name := "odata-client-http")
  .settings(description := "odata client ")
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)
//  .dependsOn(`scalajs-common`)

lazy val `node-fetch` = project
  .settings(dynamicsSettings)
  .settings(name := "odata-client-node-fetch")
  .settings(description := "odata client based on node fetch")
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)
  .dependsOn(http,client,`scalajs-common`)

lazy val `browser-fetch` = project
  .settings(dynamicsSettings)
  .settings(name := "odata-client-browser-fetch")
  .settings(description := "odata client based on a browser's fetch")
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)
  .dependsOn(http,client,`scalajs-common`)

lazy val adal = project
  .settings(dynamicsSettings)
  .settings(name := "odata-client-adal")
  .settings(description := "odata active directory authentication")
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)
  .dependsOn(client, `scalajs-common`)

lazy val client = project
  .settings(dynamicsSettings)
  .settings(name := "odata-client-clients")
  .settings(description := "odata client")
  // need to get rid of dependency on `scalajs-common`i
  .dependsOn(http, client-common`, `scalajs-common`)
  .enablePlugins(ScalaJSPlugin, AutomateHeaderPlugin)

lazy val docs = project
  .settings(buildSettings)
  .settings(noPublishSettings)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Dependencies.appDependencies.value)
  .enablePlugins(MicrositesPlugin, ScalaUnidocPlugin, ScalaJSPlugin)
  .aggregate(clientcommon, client, http, `scalajs-common`, `node-fetch`, `browser-fetch`)
  .settings(
    micrositeName := "odata-client",
    micrositeDescription := "A Microsoft Dynamics CLI swiss-army knife and browser/server library.",
    micrositeBaseUrl := "/odata-client",
    micrositeGitterChannel := false,
    micrositeDocumentationUrl := "/odata-client/docs",
    micrositeAuthor := "aappddeevv",
    micrositeGithubRepo := "odata-client",
    micrositeGithubOwner := sys.env.get("GITHUB_USER").getOrElse("unknown"),
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    micrositePushSiteWith := GitHub4s
  )
  .settings(
    siteSubdirName in ScalaUnidoc := "api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc)
  )

val npmBuild = taskKey[Unit]("fullOptJS then webpack")
npmBuild := {
  //(fullOptJS in (`cli-main`, Compile)).value
  "npm run afterScalaJSFull" !
}

val npmBuildFast = taskKey[Unit]("fastOptJS then webpack")
npmBuildFast := {
  //(fastOptJS in (`cli-main`, Compile)).value
  "npm run afterScalaJSFast" !
}

addCommandAlias("watchit", "~ ;fastOptJS; npmBuildFast")
addCommandAlias("fmt", ";scalafmt")

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
// buildInfoPackage := "dynamics-client"

bintrayReleaseOnPublish in ThisBuild := false
bintrayPackageLabels := Seq("scalajs", "odata", "scala")
bintrayVcsUrl := Some("git:git@github.com:aappddeevv/odata-client")
bintrayRepository := "maven"

