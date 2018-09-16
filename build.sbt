// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akka-cluster-up-and-running` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, DockerPlugin, JavaAppPackaging)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaClusterBootstrap,
        library.akkaClusterHttp,
        library.akkaClusterShardingTyped,
        library.akkaDiscoveryDns,
        library.akkaHttp,
        library.akkaHttpCirce,
        library.akkaSlf4j,
        library.akkaStreamTyped,
        library.circeGeneric,
        library.disruptor,
        library.log4jApi,
        library.log4jApiScala,
        library.log4jCore,
        library.log4jSlf4j,
        library.pureConfig,
        library.akkaHttpTestkit       % Test,
        library.akkaActorTestkitTyped % Test,
        library.mockitoInline         % Test,
        library.scalaCheck            % Test,
        library.utest                 % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka           = "2.5.16"
      val akkaHttp       = "10.1.5"
      val akkaHttpJson   = "1.21.0"
      val akkaManagement = "0.16.0" // 0.17.0 requires async-dns and does not work, see https://github.com/akka/akka-management/issues/257
      val circe          = "0.9.3"
      val disruptor      = "3.4.2"
      val log4j          = "2.11.1"
      val log4jApiScala  = "11.0"
      val mockito        = "2.22.0"
      val pureConfig     = "0.9.2"
      val scalaCheck     = "1.14.0"
      val utest          = "0.6.4"
    }
    val akkaClusterBootstrap     = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Version.akkaManagement
    val akkaClusterHttp          = "com.lightbend.akka.management" %% "akka-management-cluster-http"      % Version.akkaManagement
    val akkaClusterShardingTyped = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % Version.akka
    val akkaDiscoveryDns         = "com.lightbend.akka.discovery"  %% "akka-discovery-dns"                % Version.akkaManagement
    val akkaHttp                 = "com.typesafe.akka"             %% "akka-http"                         % Version.akkaHttp
    val akkaHttpCirce            = "de.heikoseeberger"             %% "akka-http-circe"                   % Version.akkaHttpJson
    val akkaHttpTestkit          = "com.typesafe.akka"             %% "akka-http-testkit"                 % Version.akkaHttp
    val akkaSlf4j                = "com.typesafe.akka"             %% "akka-slf4j"                        % Version.akka
    val akkaStreamTyped          = "com.typesafe.akka"             %% "akka-stream-typed"                 % Version.akka
    val akkaActorTestkitTyped    = "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % Version.akka
    val circeGeneric             = "io.circe"                      %% "circe-generic"                     % Version.circe
    val disruptor                = "com.lmax"                      %  "disruptor"                         % Version.disruptor
    val log4jApi                 = "org.apache.logging.log4j"      %  "log4j-api"                         % Version.log4j
    val log4jApiScala            = "org.apache.logging.log4j"      %% "log4j-api-scala"                   % Version.log4jApiScala
    val log4jCore                = "org.apache.logging.log4j"      %  "log4j-core"                        % Version.log4j
    val log4jSlf4j               = "org.apache.logging.log4j"      %  "log4j-slf4j-impl"                  % Version.log4j
    val mockitoInline            = "org.mockito"                   %  "mockito-inline"                    % Version.mockito
    val pureConfig               = "com.github.pureconfig"         %% "pureconfig"                        % Version.pureConfig
    val scalaCheck               = "org.scalacheck"                %% "scalacheck"                        % Version.scalaCheck
    val utest                    = "com.lihaoyi"                   %% "utest"                             % Version.utest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  scalafmtSettings ++
  dockerSettings ++
  commandAliases

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.6",
    organization := "rocks.heikoseeberger",
    organizationName := "Heiko Seeberger",
    startYear := Some(2018),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    testFrameworks += new TestFramework("utest.runner.Framework")
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

lazy val dockerSettings =
  Seq(
    Docker / daemonUser := "root",
    Docker / maintainer := "Heiko Seeberger",
    Docker / version := "latest",
    dockerBaseImage := "hseeberger/openjdk-iptables:8u181-slim",
    dockerExposedPorts := Seq(80, 8558),
    dockerRepository := Some("hseeberger")
  )

lazy val commandAliases =
  addCommandAlias(
    "r0",
    """|reStart
       |---
       |-Dacuar.api.port=8080
       |-Dakka.management.http.hostname=127.0.0.1
       |-Dakka.management.http.port=8558
       |-Dakka.remote.artery.canonical.hostname=127.0.0.1
       |-Dakka.remote.artery.canonical.port=25520
       |-Dakka.cluster.seed-nodes.0=akka://acuar@127.0.0.1:25520""".stripMargin
  ) ++
  addCommandAlias(
    "r1",
    """|reStart
       |---
       |-Dacuar.api.port=8081
       |-Dakka.management.http.hostname=127.0.0.1
       |-Dakka.management.http.port=8559
       |-Dakka.remote.artery.canonical.hostname=127.0.0.1
       |-Dakka.remote.artery.canonical.port=25521
       |-Dakka.cluster.seed-nodes.0=akka://acuar@127.0.0.1:25520""".stripMargin
 )
