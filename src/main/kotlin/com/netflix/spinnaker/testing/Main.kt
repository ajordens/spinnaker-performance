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
import com.netflix.spinnaker.testing.harness.ContinuousScenarioRunner
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File

fun main(args: Array<String>) {
  val yamlObjectMapper = ObjectMapper(YAMLFactory())
    .registerModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val jsonObjectMapper = ObjectMapper()
    .registerModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val config = yamlObjectMapper.readValue(
    File("${System.getProperty("user.home")}/.spinnaker/spinnaker-performance.yml"),
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

  val scenarios = loadScenarios(yamlObjectMapper, spinnakerClient, config.scenarios)
  val scenarioRunner = loadRunner(spinnakerClient, scenarios, config.scenarioRunner).also { it.plan() }

  when (scenarioRunner) {
    is ContinuousScenarioRunner -> ContinuousDriver()
    else -> DefaultDriver()
  }.drive(scenarioRunner)
}
