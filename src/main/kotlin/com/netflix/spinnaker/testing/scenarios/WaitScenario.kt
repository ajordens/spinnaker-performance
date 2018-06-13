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

package com.netflix.spinnaker.testing.scenarios

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.testing.ExecutionConfig
import com.netflix.spinnaker.testing.ScenarioConfig
import com.netflix.spinnaker.testing.api.SpinnakerClient
import java.util.*

class WaitScenario(val objectMapper: ObjectMapper,
                   val spinnakerClient: SpinnakerClient,
                   val scenarioConfig: ScenarioConfig) : Scenario {
  private val waitConfig = objectMapper.convertValue(scenarioConfig.config, WaitConfig::class.java)
  private val executionConfig = objectMapper.convertValue(scenarioConfig.executionConfig, ExecutionConfig::class.java)

  val scenarioId = UUID.randomUUID().toString()

  override fun plan(): List<ScenarioActivity> {
    return (1..executionConfig.total).map {
      ScenarioActivity(
        it,
        mutableMapOf<String, Any>(
          Pair("type", "wait"),
          Pair("waitTime", waitConfig.waitTimeSeconds),
          Pair("scenarioId", scenarioId),
          Pair("tickId", it)
        ),
        scenarioConfig.name,
        waitConfig.application,
        "Wait ${waitConfig.waitTimeSeconds} (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${it})"
      )
    }
  }
}

private data class WaitConfig(val waitTimeSeconds: Int, val application: String)
