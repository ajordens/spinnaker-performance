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

import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class Config {
    var spinnakerClient: OkHttpClientConfiguration = OkHttpClientConfiguration()
    var scenarios: List<ScenarioConfig> = emptyList()

    fun init() {
        spinnakerClient.init()
    }
}

data class ScenarioConfig(val name: String,
                          val cloudProvider: String?,
                          val type: String,
                          val config: Map<String, Any>,
                          val executionConfig: ExecutionConfig?,
                          val enabled: Boolean = true)

data class ExecutionConfig(val total: Int,
                           val perSecondRate: Double)

data class OkHttpClientConfiguration(var uri: String = "",
                                     var skipHostnameVerification: Boolean = false,
                                     var keyStore: String = "",
                                     var keyStorePassword: String = "",
                                     var keyStorePasswordFile: String = "") {
    companion object {
        val logger = LoggerFactory.getLogger(OkHttpClient::class.java)
    }

    var okHttpClient = OkHttpClient()

    fun init() {
        if (!keyStore.isNullOrEmpty()) {
            val keyStorePassword = if (!keyStorePassword.isNullOrEmpty()) {
                keyStorePassword
            } else if (!keyStorePasswordFile.isNullOrEmpty()) {
                File(keyStorePasswordFile).readText()
            } else {
                throw IllegalStateException("No `keystorePassword` or `keyStorePasswordFile` specified")
            }

            val jksKeyStore = KeyStore.getInstance("JKS")

            File(this.keyStore).inputStream().use {
                jksKeyStore.load(it, keyStorePassword.toCharArray())
            }

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(jksKeyStore, keyStorePassword.toCharArray());

            val keyManagers = kmf.keyManagers
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagers, null, null);

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)

            this.okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .sslSocketFactory(
                            sslContext.socketFactory,
                            Platform.get().trustManager(sslContext.socketFactory))
                    .build()
        }
    }
}
