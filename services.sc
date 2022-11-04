#!/usr/bin/env amm
// HttpApi.sc

import $ivy.{`com.lihaoyi::requests:0.2.0 compat`, `com.lihaoyi::ujson:0.7.5 compat`}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

val apiBase = "https://api.fastly.com"

def vcl(serviceId: String, versionId: Int) = {
  val json = ujson.read(
    requests
      .get(s"$apiBase/service/$serviceId/version/$versionId/generated_vcl", headers = Map("Fastly-Key" -> System.getenv("API_KEY")))
      .text()
  )
  for {
    content <- json.obj.get("content")
  } yield {
    content.str
  }
}

@main
def services() = {
  val json = ujson.read(
    requests
      .get(s"$apiBase/services", headers = Map("Fastly-Key" -> System.getenv("API_KEY")))
      .text()
  )
  for {
    data <- json.obj.get("data").toList
    item <- data.arr
    attributes <- item.obj.get("attributes")
    optName <- attributes.obj.get("name")
    name <- optName.strOpt
    optVersion <- attributes.obj.get("active_version")
    version <- optVersion.numOpt.map(_.toInt)
    optId <- item.obj.get("id")
    id <- optId.strOpt
  } yield {
    //println(s"$name - $id")
    val v = vcl(id, version)
    Files.write(Paths.get(s"$name.vcl"), v.get.getBytes(StandardCharsets.UTF_8))
    println(s"$name - $id - $version")
  }
  ""
//  val names = for{
//    item <- json.arr
//    name <- item.obj.get("name")
//  } yield name.str
//  names.mkString(",")
}
