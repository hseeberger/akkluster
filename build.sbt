// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akkluster` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, DockerPlugin, JavaAppPackaging)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaClusterBootstrap,
        library.akkaClusterHttp,
        library.akkaClusterShardingTyped,
        library.akkaDiscoveryConfig, // For running locally only!
        library.akkaDiscoveryK8s,
        library.akkaHttp,
        library.akkaHttpCirce,
        library.akkaSbr,
        library.akkaSlf4j,
        library.akkaStreamTyped,
        library.circeGeneric,
        library.circeParser,
        library.disruptor,
        library.log4jApi,
        library.log4jCore,
        library.log4jSlf4j,
        library.pureConfig,
        library.akkaActorTestkitTyped % Test,
        library.akkaHttpTestkit       % Test,
        library.akkaStreamTestkit     % Test,
        library.mockitoInline         % Test,
        library.scalaCheck            % Test,
        library.utest                 % Test,
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka           = "2.5.19"
      val akkaHttp       = "10.1.5"
      val akkaHttpJson   = "1.22.0"
      val akkaManagement = "0.20.0"
      val akkaSbr        = "1.1.4"
      val circe          = "0.10.1"
      val disruptor      = "3.4.2"
      val log4j          = "2.11.1"
      val log4jApiScala  = "11.0"
      val mockito        = "2.23.4"
      val pureConfig     = "0.10.1"
      val scalaCheck     = "1.14.0"
      val utest          = "0.6.6"
    }
    val akkaActorTestkitTyped    = "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % Version.akka
    val akkaClusterBootstrap     = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Version.akkaManagement
    val akkaClusterHttp          = "com.lightbend.akka.management" %% "akka-management-cluster-http"      % Version.akkaManagement
    val akkaClusterShardingTyped = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % Version.akka
    val akkaDiscoveryConfig      = "com.lightbend.akka.discovery"  %% "akka-discovery-config"             % Version.akkaManagement
    val akkaDiscoveryK8s         = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % Version.akkaManagement
    val akkaHttp                 = "com.typesafe.akka"             %% "akka-http"                         % Version.akkaHttp
    val akkaHttpCirce            = "de.heikoseeberger"             %% "akka-http-circe"                   % Version.akkaHttpJson
    val akkaHttpTestkit          = "com.typesafe.akka"             %% "akka-http-testkit"                 % Version.akkaHttp
    val akkaSlf4j                = "com.typesafe.akka"             %% "akka-slf4j"                        % Version.akka
    val akkaSbr                  = "com.lightbend.akka"            %% "akka-split-brain-resolver"         % Version.akkaSbr
    val akkaStreamTestkit        = "com.typesafe.akka"             %% "akka-stream-testkit"               % Version.akka
    val akkaStreamTyped          = "com.typesafe.akka"             %% "akka-stream-typed"                 % Version.akka
    val circeGeneric             = "io.circe"                      %% "circe-generic"                     % Version.circe
    val circeParser              = "io.circe"                      %% "circe-parser"                      % Version.circe
    val disruptor                = "com.lmax"                      %  "disruptor"                         % Version.disruptor
    val log4jApi                 = "org.apache.logging.log4j"      %  "log4j-api"                         % Version.log4j
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
    scalaVersion := "2.12.8",
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
      "-Ywarn-unused-import",
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := false,
    testFrameworks += new TestFramework("utest.runner.Framework"),
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
  )

lazy val dockerSettings =
  Seq(
    Docker / daemonUser := "root",
    Docker / maintainer := "Heiko Seeberger",
    Docker / version := "latest",
    dockerBaseImage := "hseeberger/openjdk-iptables:8u181-slim",
    dockerExposedPorts := Seq(80, 8558, 25520),
    dockerRepository := Some("hseeberger"),
  )

lazy val commandAliases =
  addCommandAlias(
    "r0",
    """|reStart
       |---
       |-Dakkluster.api.port=8080
       |-Dakka.cluster.seed-nodes.0=akka://akkluster@127.0.0.1:25520
       |-Dakka.cluster.roles.0=static
       |-Dakka.discovery.method=config
       |-Dakka.management.http.hostname=127.0.0.1
       |-Dakka.management.http.port=8558
       |-Dakka.remote.artery.canonical.hostname=127.0.0.1
       |-Dakka.remote.artery.canonical.port=25520
       |""".stripMargin
  ) ++
  addCommandAlias(
    "r1",
    """|reStart
       |---
       |-Dakkluster.api.port=8081
       |-Dakka.cluster.seed-nodes.0=akka://akkluster@127.0.0.1:25520
       |-Dakka.cluster.roles.0=dynamic
       |-Dakka.discovery.method=config
       |-Dakka.management.http.hostname=127.0.0.1
       |-Dakka.management.http.port=8559
       |-Dakka.remote.artery.canonical.hostname=127.0.0.1
       |-Dakka.remote.artery.canonical.port=25521
       |""".stripMargin
 )
