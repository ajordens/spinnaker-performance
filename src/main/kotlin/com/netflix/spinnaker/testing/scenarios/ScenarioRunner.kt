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

import com.netflix.spinnaker.testing.api.SpinnakerClient
import com.netflix.spinnaker.testing.api.Task
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtilities
import org.jfree.data.category.DefaultCategoryDataset
import java.io.File

class ScenarioRunner(val spinnakerClient: SpinnakerClient, val scenarios: List<Scenario>) {
  val allActivities = mutableMapOf<Int, List<ScenarioActivity>>().withDefault { mutableListOf() }

  init {
    plan()
  }

  fun plan() {
    allActivities.putAll(
      scenarios.flatMap { it.plan() }.groupBy { it.secondsOffset }
    )
    println(allActivities)
  }

  fun tick(secondsOffset: Int) : Boolean {
    for (activity in allActivities.getValue(secondsOffset)) {
      val response = spinnakerClient.submitTask(
        Task(
          arrayListOf(activity.job),
          activity.application,
          activity.description
        )
      ).execute()

      activity.taskId = response.body()?.ref?.replace("/tasks/", "")
    }

    val maxSecondsOffset = allActivities.keys.max() ?: 0
    return secondsOffset > maxSecondsOffset
  }

  fun fetchResults(): Boolean {
    val activitiesMissingResults = allActivities.values.flatten().filter { it.taskResult == null }

    activitiesMissingResults.forEach {
      val taskResult = spinnakerClient.getTask(it.taskId!!).execute().body()
      if (taskResult != null && taskResult.status != "RUNNING") {
        it.taskResult = taskResult
      }
    }

    return activitiesMissingResults.filter { it.taskResult == null }.isEmpty()
  }

  fun generateReport() {
    val reportDirectory = File("results", "${System.currentTimeMillis()}")
    reportDirectory.mkdirs()

    val durationDataset = DefaultCategoryDataset()
    for (activity in allActivities.values.flatten()) {
      try {
        durationDataset.addValue(
          activity.taskResult!!.getDuration(),
          activity.scenarioName,
          activity.taskResult!!.getVariable("tickId")!!.value as Int)
      } catch (e: Exception) {
        println("Error: ${activity.taskId}")
        e.printStackTrace()
      }
    }

    val durationChart = ChartFactory.createLineChart(
      "Total Duration", "Scenarios", "seconds", durationDataset
    )

    ChartUtilities.saveChartAsPNG(File(reportDirectory, "scenarios_duration.png"), durationChart, 1440, 900);

    val lagDataset = DefaultCategoryDataset()
    for (activity in allActivities.values.flatten()) {
      lagDataset.addValue(
        activity.taskResult!!.getLag() * 1000,
        activity.scenarioName,
        activity.taskResult!!.getVariable("tickId")!!.value as Int)
    }

    val lagChart = ChartFactory.createLineChart(
      "Total Lag", "Scenarios", "milliseconds", lagDataset
    )

    ChartUtilities.saveChartAsPNG(File(reportDirectory, "scenarios_lag.png"), lagChart, 1440, 900);

    println("Reports written to ${reportDirectory}")
  }
}
