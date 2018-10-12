package coursier.cli.publish

import coursier.core.{Configuration, Type}
import coursier.{ModuleName, Organization}

import scala.collection.mutable
import scala.xml.NodeSeq

object Pom {

  def create(
    organization: Organization,
    moduleName: ModuleName,
    version: String,
    packaging: Option[Type] = None,
    description: Option[String] = None,
    url: Option[String] = None,
    name: Option[String] = None,
    // TODO Accept full-fledged coursier.Dependency
    dependencies: Seq[(Organization, ModuleName, String, Option[Configuration])] = Nil
  ): String = {

    val nodes = new mutable.ListBuffer[NodeSeq]

    nodes ++= Seq(
      <modelVersion>4.0.0</modelVersion>,
      <groupId>{organization.value}</groupId>,
      <artifactId>{moduleName.value}</artifactId>,
      <version>{version}</version>
    )

    for (p <- packaging)
      nodes += <packaging>{p.value}</packaging>

    for (u <- url)
      nodes += <url>{u}</url>

    for (d <- description)
      nodes += <description>{d}</description>

    for (n <- name)
      nodes += <name>{n}</name>

    nodes += {
      val urlNodeOpt = url.fold[NodeSeq](Nil)(u => <url>{u}</url>)
      <organization>
        <name>{organization.value}</name>
        {urlNodeOpt}
      </organization>
    }

    // TODO Licenses
    //   <licenses>
    //     <license>
    //       <name>Apache 2.0</name>
    //       <url>http://opensource.org/licenses/Apache-2.0</url>
    //       <distribution>repo</distribution>
    //     </license>
    //   </licenses>

    // TODO SCM
    //   <scm>
    //     <url>https://github.com/coursier/coursier.git</url>
    //     <connection>scm:git:github.com/coursier/coursier.git</connection>
    //     <developerConnection>scm:git:git@github.com:coursier/coursier.git</developerConnection>
    //   </scm>

    // TODO Developers
    // <developers>
    //   <developer>
    //     <id>jane-d</id>
    //     <name>Jane Doe</name>
    //     <url>https://github.com/jane-d</url>
    //   </developer>
    // </developers>
    //   + optional mail

    nodes +=
      <dependencies>
        {
          dependencies.map {
            case (depOrg, depName, ver, confOpt) =>
              <dependency>
                <groupId>{depOrg.value}</groupId>
                <artifactId>{depName.value}</artifactId>
                <version>{version}</version>
                {confOpt.fold[NodeSeq](Nil)(c => <scope>{c}</scope>)}
              </dependency>
          }
        }
      </dependencies>

    val printer = new scala.xml.PrettyPrinter(Int.MaxValue, 2)

    """<?xml version="1.0" encoding="UTF-8"?>""" + '\n' + printer.format(
      <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
        {nodes.result()}
      </project>
    )
  }

}
