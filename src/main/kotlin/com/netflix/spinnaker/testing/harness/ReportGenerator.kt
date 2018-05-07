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
package com.netflix.spinnaker.testing.harness

import com.netflix.spinnaker.testing.scenarios.ScenarioActivity
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtilities
import org.jfree.data.category.DefaultCategoryDataset
import java.io.File

interface ReportGenerator {
  fun generate(activities: List<ScenarioActivity>)
}

class ConsoleReportGenerator : ReportGenerator {

  override fun generate(activities: List<ScenarioActivity>) {
    throw UnsupportedOperationException("not implemented")
  }
}

class ChartReportGenerator : ReportGenerator {

  override fun generate(activities: List<ScenarioActivity>) {
    val reportDirectory = File("results", "${System.currentTimeMillis()}")
    reportDirectory.mkdirs()

    val durationDataset = DefaultCategoryDataset()
    for (activity in activities) {
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
    for (activity in activities) {
      lagDataset.addValue(
        activity.taskResult!!.getLag() * 1000,
        activity.scenarioName,
        activity.taskResult!!.getVariable("tickId")!!.value as Int)
    }

    val lagChart = ChartFactory.createLineChart(
      "Total Lag", "Scenarios", "milliseconds", lagDataset
    )

    ChartUtilities.saveChartAsPNG(File(reportDirectory, "scenarios_lag.png"), lagChart, 1440, 900);

    println("Reports written to $reportDirectory")
  }
}
