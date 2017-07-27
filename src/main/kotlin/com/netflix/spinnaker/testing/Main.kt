/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.testing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.netflix.spinnaker.testing.api.SpinnakerClient
import com.netflix.spinnaker.testing.scenarios.Scenario
import com.netflix.spinnaker.testing.scenarios.ScenarioRunner
import java.io.File
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>) {
  val yamlObjectMapper = ObjectMapper(YAMLFactory())
    .registerModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val jsonObjectMapper = ObjectMapper()
    .registerModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val config = yamlObjectMapper.readValue(
    File("/Users/ajordens/.spinnaker/spinnaker-performance.yml"),
    Config::class.java
  )
  config.init()

  val retrofit = Retrofit.Builder()
    .baseUrl(config.spinnakerClient.uri)
    .addConverterFactory(JacksonConverterFactory.create(jsonObjectMapper))
    .client(config.spinnakerClient.okHttpClient)
    .build()

  val spinnakerClient = retrofit.create(SpinnakerClient::class.java)
  if (!spinnakerClient.health().execute().isSuccessful) {
    // verify that Spinnaker can be accessed (and is healthy!)
    throw IllegalStateException("Unable to reach ${config.spinnakerClient.uri}/health")
  }

  val scenarios = config.scenarios.filter { it.enabled }.map {
    val scenarioClass = "${it.cloudProvider ?: ""}.${it.type.capitalize()}Scenario"
    val clazz = Class.forName("com.netflix.spinnaker.testing.scenarios.${scenarioClass}".replace("..", "."))
    val constructor = clazz.getConstructor(
      ObjectMapper::class.java, SpinnakerClient::class.java, ScenarioConfig::class.java
    )

    constructor.newInstance(yamlObjectMapper, spinnakerClient, it) as Scenario
  }

  val timer = Timer()
  timer.scheduleAtFixedRate(TickTask(ScenarioRunner(spinnakerClient, scenarios), timer), 1000, 1000)
}

private class TickTask(val scenarioRunner: ScenarioRunner, val timer: Timer) : TimerTask() {
  val count = AtomicInteger(1)

  override fun run() {
    if (scenarioRunner.tick(count.get())) {
      println("All tasks kicked off, waiting for results ...")
      cancel()
      timer.scheduleAtFixedRate(FetchResultsTask(scenarioRunner, timer), 5000, 30000)
    }

    count.addAndGet(1)
  }
}

private class FetchResultsTask(val scenarioRunner: ScenarioRunner, val timer: Timer) : TimerTask() {
  override fun run() {
    if (scenarioRunner.fetchResults()) {
      println("All results fetched")

      cancel()
      timer.schedule(GenerateReportTask(scenarioRunner, timer), 10000)
    }
  }
}

private class GenerateReportTask(val scenarioRunner: ScenarioRunner, val timer: Timer) : TimerTask() {
  override fun run() {
    scenarioRunner.generateReport()
    timer.cancel()
  }
}
