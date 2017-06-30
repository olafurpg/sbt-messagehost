import com.trueaccord.scalapb.compiler.Version.scalapbVersion

lazy val messagehost = project
  .settings(
    allSettings,
    publishSettings,
    protobufSettings,
    sbtPlugin := true,
    scalaVersion := scala210,
    crossScalaVersions := Seq(scala210),
    moduleName := "messagehost"
  )

lazy val allSettings = List(
  version := sys.props.getOrElse("messagehost.version", version.value),
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  testOptions in Test += Tests.Argument("-oD"),
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val publishSettings = Seq(
  publishTo := {
    Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
  },
  bintrayOrganization := Some("scalameta"),
  bintrayRepository := "maven",
  publishArtifact in Test := false,
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/olafurpg/sbt-messagehost")),
  autoAPIMappings := true,
  apiURL := Some(url("https://github.com/olafurpg/sbt-messagehost")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/olafurpg/sbt-messagehost"),
      "scm:git:git@github.com:olafurpg/sbt-messagehost.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-language:existentials",
  //  "-Ywarn-numeric-widen", // TODO(olafur) enable
  "-Xfuture",
  "-Xlint"
)

lazy val protobufSettings = Seq(
  PB.targets.in(Compile) := Seq(
    scalapb.gen(
      flatPackage = true // Don't append filename to package
    ) -> sourceManaged.in(Compile).value
  ),
//  PB.protoSources.in(Compile) := Seq(file("/src/main/protobuf")),
  libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % scalapbVersion
)

lazy val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

allSettings
noPublish

lazy val scala210 = "2.10.6"
lazy val scala211 = "2.11.11"
lazy val scala212 = "2.12.2"
