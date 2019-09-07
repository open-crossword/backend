name := "crossword-backend"

version := "0.1"

scalaVersion := "2.13.0"


lazy val akkaHttpVersion = "10.1.8"
lazy val akkaVersion = "2.6.0-M7"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,

  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,

  "org.typelevel" %% "cats-core" % "2.0.0-M4",

  "com.lihaoyi" %% "requests" % "0.2.0",
  "com.lihaoyi" %% "os-lib" % "0.3.0",

  "ch.megard" %% "akka-http-cors" % "0.4.1",

  "org.slf4j" % "slf4j-simple" % "2.0.0-alpha0"
)

// ScalaTest
libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

// Akka TestKit
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-typed-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
)

// shamelessly stolen from doobie's build.sbt because cats was giving me lip
// I'm sure we don't really need all of this
lazy val compilerFlags = Seq(
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 11 => // for 2.11 all we care about is capabilities, not warnings
        Seq(
          "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
          "-language:higherKinds", // Allow higher-kinded types
          "-language:implicitConversions", // Allow definition of implicit functions called views
          "-Ypartial-unification" // Enable partial unification in type constructor inference
        )
      case _ =>
        Seq(
          "-deprecation", // Emit warning and location for usages of deprecated APIs.
          "-encoding", "utf-8", // Specify character encoding used by source files.
          "-explaintypes", // Explain type errors in more detail.
          "-feature", // Emit warning and location for usages of features that should be imported explicitly.
          "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
          "-language:higherKinds", // Allow higher-kinded types
          "-language:implicitConversions", // Allow definition of implicit functions called views
          "-unchecked", // Enable additional warnings where generated code depends on assumptions.
          "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
          "-Xfatal-warnings", // Fail the compilation if there are any warnings.
          "-Xfuture", // Turn on future language features.
          "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
          "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
          "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
          "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
          "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
          "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
          "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
          "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
          "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
          "-Xlint:option-implicit", // Option.apply used implicit view.
          "-Xlint:package-object-classes", // Class or object defined in package object.
          "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
          "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
          "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
          "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
          // "-Yno-imports",                      // No predef or default imports
          "-Ywarn-dead-code", // Warn when dead code is identified.
          "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
          "-Ywarn-numeric-widen", // Warn when numerics are widened.
          "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
          "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
          "-Ywarn-unused:locals", // Warn if a local definition is unused.
          "-Ywarn-unused:params", // Warn if a value parameter is unused.
          "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
          "-Ywarn-unused:privates", // Warn if a private member is unused.
          "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
        )
    }
    ),
  // flags removed in 2.13
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n == 12 =>
        Seq(
          "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
          "-Ypartial-unification" // Enable partial unification in type constructor inference
        )
      case _ =>
        Seq.empty
    }
    ),
  scalacOptions in(Test, compile) --= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 11 =>
        Seq("-Yno-imports")
      case _ =>
        Seq(
          "-Ywarn-unused:privates",
          "-Ywarn-unused:locals",
          "-Ywarn-unused:imports",
          "-Yno-imports"
        )
    }
    ),
  scalacOptions in(Compile, console) --= Seq("-Xfatal-warnings", "-Ywarn-unused:imports", "-Yno-imports"),
  scalacOptions in(Compile, console) ++= Seq("-Ydelambdafy:inline"), // http://fs2.io/faq.html
  scalacOptions in(Compile, doc) --= Seq("-Xfatal-warnings", "-Ywarn-unused:imports", "-Yno-imports")
)
