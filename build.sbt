// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization     := "rocks.heikoseeberger",
    organizationName := "Heiko Seeberger",
    startYear        := Some(2018),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := "2.13.6",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-encoding",
      "UTF-8",
      "-Ywarn-unused:imports",
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalafmtOnCompile := true,
    dynverSeparator   := "_", // the default `+` is not compatible with docker tags
  )
)

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val akkluster =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin, DockerPlugin, JavaAppPackaging)
    .settings(commonSettings)
    .settings(
      // Library dependencies
      libraryDependencies ++= Seq(
        library.akkaMgmClusterBootstrap,
        library.akkaMgmClusterHttp,
        library.akkaClusterShardingTyped,
        library.akkaDiscovery,
        library.akkaMgmDiscoveryK8n,
        library.akkaHttp,
        library.akkaHttpSprayJson,
        // To use the Lightbend SBR you need a Lightbend account (demo or commercial)
        // library.akkaSbr,
        library.akkaSlf4j,
        library.akkaStreamTyped,
        library.borerDerivation,
        library.disruptor,
        library.log4jApi,
        library.log4jCore,
        library.log4jSlf4j,
        library.pureConfig,
        library.akkaActorTestkitTyped % Test,
        library.akkaHttpTestkit       % Test,
        library.akkaStreamTestkit     % Test,
        library.mUnit                 % Test,
        library.mockitoScala          % Test,
      ),
      // Docker settings
      dockerBaseImage        := "hseeberger/openjdk-iptables:11.0.10",
      dockerRepository       := Some("hseeberger"),
      dockerExposedPorts     := Seq(8080, 8558, 25520),
      Docker / daemonUser    := "root",
      Docker / daemonUserUid := None,
      Docker / maintainer    := organizationName.value,
      // Publish settings
      Compile / packageDoc / publishArtifact := false, // speed up building Docker images
      Compile / packageSrc / publishArtifact := false, // speed up building Docker images
    )
    .settings(commandAliases)

// *****************************************************************************
// Project settings
// *****************************************************************************

lazy val commonSettings =
  Seq(
    // Also (automatically) format build definition together with sources
    Compile / scalafmt := {
      val _ = (Compile / scalafmtSbt).value
      (Compile / scalafmt).value
    }
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

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka         = "2.6.16"
      val akkaHttp     = "10.2.6"
      val akkaMgm      = "1.1.1"
      val akkaSbr      = "1.1.4"
      val borer        = "1.7.2"
      val disruptor    = "3.4.4"
      val log4j        = "2.14.1"
      val mockitoScala = "1.16.42"
      val pureConfig   = "0.16.0"
      val scalaCheck   = "1.15.3"
      val mUnit        = "0.7.29"
    }
    // format: off
    val akkaActorTestkitTyped    = "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % Version.akka
    val akkaClusterShardingTyped = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % Version.akka
    val akkaDiscovery            = "com.typesafe.akka"             %% "akka-discovery"                    % Version.akka
    val akkaHttp                 = "com.typesafe.akka"             %% "akka-http"                         % Version.akkaHttp
    val akkaHttpSprayJson        = "com.typesafe.akka"             %% "akka-http-spray-json"              % Version.akkaHttp
    val akkaHttpTestkit          = "com.typesafe.akka"             %% "akka-http-testkit"                 % Version.akkaHttp
    val akkaMgmClusterBootstrap  = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Version.akkaMgm
    val akkaMgmClusterHttp       = "com.lightbend.akka.management" %% "akka-management-cluster-http"      % Version.akkaMgm
    val akkaMgmDiscoveryK8n      = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % Version.akkaMgm
    val akkaSlf4j                = "com.typesafe.akka"             %% "akka-slf4j"                        % Version.akka
    val akkaSbr                  = "com.lightbend.akka"            %% "akka-split-brain-resolver"         % Version.akkaSbr
    val akkaStreamTestkit        = "com.typesafe.akka"             %% "akka-stream-testkit"               % Version.akka
    val akkaStreamTyped          = "com.typesafe.akka"             %% "akka-stream-typed"                 % Version.akka
    val borerDerivation          = "io.bullet"                     %% "borer-derivation"                  % Version.borer
    val disruptor                = "com.lmax"                       % "disruptor"                         % Version.disruptor
    val log4jApi                 = "org.apache.logging.log4j"       % "log4j-api"                         % Version.log4j
    val log4jCore                = "org.apache.logging.log4j"       % "log4j-core"                        % Version.log4j
    val log4jSlf4j               = "org.apache.logging.log4j"       % "log4j-slf4j-impl"                  % Version.log4j
    val mockitoScala             = "org.mockito"                   %% "mockito-scala"                     % Version.mockitoScala
    val mUnit                    = "org.scalameta"                 %% "munit"                             % Version.mUnit
    val pureConfig               = "com.github.pureconfig"         %% "pureconfig"                        % Version.pureConfig
    // format: on
  }
