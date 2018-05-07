/*
 * Copyright 2018 Netflix, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.testing.api.SpinnakerClient
import com.netflix.spinnaker.testing.scenarios.Scenario
import com.netflix.spinnaker.testing.harness.ScenarioRunner

fun loadScenarios(yamlObjectMapper: ObjectMapper,
                  spinnakerClient: SpinnakerClient,
                  config: List<ScenarioConfig>): List<Scenario> {
  return config
    .filter { it.enabled }
    .map {
      val scenarioClass = "${it.cloudProvider ?: ""}.${it.type.capitalize()}Scenario"
      val clazz = Class.forName("com.netflix.spinnaker.testing.scenarios.$scenarioClass".replace("..", "."))
      val constructor = clazz.getConstructor(
        ObjectMapper::class.java, SpinnakerClient::class.java, ScenarioConfig::class.java
      )

      constructor.newInstance(yamlObjectMapper, spinnakerClient, it) as Scenario
    }
}

fun loadRunner(spinnakerClient: SpinnakerClient,
               scenarios: List<Scenario>,
               name: String): ScenarioRunner {
  val runnerClass = "${name}ScenarioRunner"
  val clazz = Class.forName("com.netflix.spinnaker.testing.harness.$runnerClass")
  println("Using ${clazz.simpleName}")
  val constructor = clazz.getConstructor(
    SpinnakerClient::class.java, List::class.java
  )

  return constructor.newInstance(spinnakerClient, scenarios) as ScenarioRunner
}
