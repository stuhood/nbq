import sbt._
import Process._
import com.twitter.sbt._

/**
 * Sbt project files are written in a DSL in scala.
 *
 * The % operator is just turning strings into maven dependency declarations, so lines like
 *     val example = "com.example" % "exampleland" % "1.0.3"
 * mean to add a dependency on exampleland version 1.0.3 from provider "com.example".
 */
class NbqProject(info: ProjectInfo) extends StandardServiceProject(info)
  with NoisyDependencies
  with DefaultRepos
  with SubversionPublisher
  with PublishSourcesAndJavadocs
  with PublishSite
  with CompileThriftRuby
  with CompileThriftScroogeMixin
{
  val finagleVersion = "1.9.6"

  val finagleC = "com.twitter" % "finagle-core" % finagleVersion
  val finagleT = "com.twitter" % "finagle-thrift" % finagleVersion
  val finagleO = "com.twitter" % "finagle-ostrich4" % finagleVersion

  val scrooge_runtime = "com.twitter" % "scrooge-runtime" % "1.0.3"
  override def scroogeVersion = "2.2.0"

  // for tests
  val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test" withSources()
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"

  override def mainClass = Some("com.twitter.nbq.Main")

  override def subversionRepository = Some("http://svn.local.twitter.com/maven")
}
