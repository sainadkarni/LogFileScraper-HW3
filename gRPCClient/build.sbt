
name := "gRPCClient"

version := "0.1"

scalaVersion := "2.13.7"

val logbackVersion = "1.3.0-alpha10"
val sfl4sVersion = "2.0.0-alpha5"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"

resolvers += Resolver.jcenterRepo

Compile / PB.targets := Seq(
	scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
	"ch.qos.logback" % "logback-core" % logbackVersion,
	"ch.qos.logback" % "logback-classic" % logbackVersion,
	"org.slf4j" % "slf4j-api" % sfl4sVersion,
	"com.typesafe" % "config" % typesafeConfigVersion,
	"commons-io" % "commons-io" % apacheCommonIOVersion,
	"org.scalactic" %% "scalactic" % scalacticVersion,
	"org.scalatest" %% "scalatest" % scalacticVersion % Test,
	"org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test,
	"com.typesafe" % "config" % typesafeConfigVersion,
	"io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
	"com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
	"com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
)

idePackagePrefix := Some("com.grpcclient")
