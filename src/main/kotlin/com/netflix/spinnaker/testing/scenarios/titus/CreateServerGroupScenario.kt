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


package com.netflix.spinnaker.testing.scenarios.titus

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.testing.ExecutionConfig
import com.netflix.spinnaker.testing.ScenarioConfig
import com.netflix.spinnaker.testing.api.SpinnakerClient
import com.netflix.spinnaker.testing.scenarios.Scenario
import com.netflix.spinnaker.testing.scenarios.ScenarioActivity
import java.util.*

class CreateServerGroupScenario(val objectMapper: ObjectMapper,
                                val spinnakerClient: SpinnakerClient,
                                val scenarioConfig: ScenarioConfig) : Scenario {
  private val config = objectMapper.convertValue(scenarioConfig.config, CreateServerGroupScenarioConfig::class.java)
  private val executionConfig = objectMapper.convertValue(scenarioConfig.executionConfig, ExecutionConfig::class.java)

  val scenarioId = UUID.randomUUID().toString()
  override fun plan(): List<ScenarioActivity> {
    return (1..executionConfig.total).map {
      val job = config.toJob(it.toString())
      job.put("scenarioId", scenarioId)
      job.put("tickId", it)

      ScenarioActivity(
        it,
        job,
        scenarioConfig.name,
        config.application,
        "CreateServerGroup (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${it})"
      )
    }
  }
}

private data class CreateServerGroupScenarioConfig(val application: String,
                                                   val registry: String,
                                                   val image: String,
                                                   val region: String,
                                                   val account: String,
                                                   val iamRole: String,
                                                   val cpu: Int,
                                                   val memory: String,
                                                   val disk: String,
                                                   val networkMbps: String,
                                                   val timeoutSeconds: Int,
                                                   val capacity: Int,
                                                   val interestingHealthProviderNames: String?,
                                                   val entryPoint: String?
) {
  fun toJob(stack: String): MutableMap<String, Any> {
    val job = mutableMapOf(
      Pair("application", application),
      Pair("stack", stack),
      Pair("credentials", account),
      Pair("account", account),
      Pair("region", region),
      Pair("inService", true),
      Pair("deferredInitialization", true),
      Pair("network", "default"),
      Pair("strategy", ""),
      Pair("capacity", mutableMapOf(
        Pair("min", capacity),
        Pair("max", capacity),
        Pair("desired", capacity)
      )),
      Pair("resources", mutableMapOf(
        Pair("allocateIpAddress", false),
        Pair("cpu", cpu),
        Pair("networkMbps", networkMbps),
        Pair("disk", disk),
        Pair("memory", memory)
      )),
      Pair("registry", registry),
      Pair("imageId", image),
      Pair("type", "createServerGroup"),
      Pair("cloudProvider", "titus"),
      Pair("stageTimeoutMs", timeoutSeconds * 1000)
    )

    if (interestingHealthProviderNames != null) {
      job.put("interestingHealthProviderNames", interestingHealthProviderNames.split(","));
    }

    if (entryPoint != null) {
      job.put("entryPoint", entryPoint);
    }

    return job;
  }
}
