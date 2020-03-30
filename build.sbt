// *****************************************************************************
// Projects
// *****************************************************************************

lazy val akkluster =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, DockerPlugin, JavaAppPackaging)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaMgmClusterBootstrap,
        library.akkaClusterShardingTyped,
        library.akkaDiscovery,
        library.akkaMgmDiscoveryK8n,
        library.akkaHttp,
        library.akkaHttpCirce,
        library.akkaHttpSprayJson,
        // To use the Lightbend SBR you need a Lightbend account (demo or commercial)
        // library.akkaSbr,
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
      val akka          = "2.6.4"
      val akkaHttp      = "10.1.11"
      val akkaMgm       = "1.0.6"
      val akkaHttpJson  = "1.31.0"
      val akkaSbr       = "1.1.4"
      val circe         = "0.13.0"
      val disruptor     = "3.4.2"
      val log4j         = "2.13.1"
      val log4jApiScala = "11.0"
      val mockito       = "3.3.3"
      val pureConfig    = "0.12.3"
      val scalaCheck    = "1.14.3"
      val utest         = "0.7.4"
    }
    val akkaActorTestkitTyped    = "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % Version.akka
    val akkaClusterShardingTyped = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % Version.akka
    val akkaDiscovery            = "com.typesafe.akka"             %% "akka-discovery"                    % Version.akka
    val akkaHttp                 = "com.typesafe.akka"             %% "akka-http"                         % Version.akkaHttp
    val akkaHttpCirce            = "de.heikoseeberger"             %% "akka-http-circe"                   % Version.akkaHttpJson
    val akkaHttpSprayJson        = "com.typesafe.akka"             %% "akka-http-spray-json"              % Version.akkaHttp
    val akkaHttpTestkit          = "com.typesafe.akka"             %% "akka-http-testkit"                 % Version.akkaHttp
    val akkaMgmClusterBootstrap  = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Version.akkaMgm
    val akkaMgmDiscoveryK8n      = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % Version.akkaMgm
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
    scalaVersion := "2.13.1",
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
      "-Ywarn-unused:imports",
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
    Docker / maintainer := organizationName.value,
    Docker / version := "latest",
    dockerBaseImage := "hseeberger/openjdk-iptables:11.0.6",
    dockerExposedPorts := Seq(8080, 8558, 25520),
  )

lazy val commandAliases =
  addCommandAlias(
    "r1",
    """|reStart
       |---
       |-Dakka.cluster.seed-nodes.0=akka://akkluster@127.0.0.1:25520
       |-Dakka.cluster.roles.0=static
       |-Dakka.management.cluster.bootstrap.contact-point-discovery.discovery-method=config
       |-Dakka.management.http.hostname=127.0.0.1
       |-Dakka.remote.artery.canonical.hostname=127.0.0.1
       |-Dakkluster.http-server.interface=127.0.0.1
       |""".stripMargin
  ) ++
  addCommandAlias(
    "r2",
    """|reStart
       |---
       |-Dakka.cluster.seed-nodes.0=akka://akkluster@127.0.0.1:25520
       |-Dakka.cluster.roles.0=dynamic
       |-Dakka.management.cluster.bootstrap.contact-point-discovery.discovery-method=config
       |-Dakka.management.http.hostname=127.0.0.2
       |-Dakka.remote.artery.canonical.hostname=127.0.0.2
       |-Dakkluster.http-server.interface=127.0.0.2
       |""".stripMargin
 )
