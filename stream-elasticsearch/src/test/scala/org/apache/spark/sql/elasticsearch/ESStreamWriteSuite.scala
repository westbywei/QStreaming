/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.elasticsearch


import java.util.concurrent.TimeUnit

import com.alibaba.fastjson.JSON
import com.qiniu.stream.core.PipelineRunner
import com.qiniu.stream.core.config.{ResourcePipelineConfig, Settings}
import de.flapdoodle.embed.process.runtime.Network
import org.apache.http.HttpHost
import org.apache.spark.sql.streaming.StreamTest
import org.scalatest.BeforeAndAfter
import pl.allegro.tech.embeddedelasticsearch.{EmbeddedElastic, PopularProperties}


class ESStreamWriteSuite extends StreamTest with BeforeAndAfter {
  val port: Int = Network.getFreeServerPort
  var embeddedElastic: EmbeddedElastic = _

  before {
    embeddedElastic = EmbeddedElastic.builder()
      .withElasticVersion("6.8.13")
      .withSetting(PopularProperties.HTTP_PORT, port)
      .withSetting(PopularProperties.TRANSPORT_TCP_PORT, port + 1)
      .withSetting(PopularProperties.CLUSTER_NAME, "test-cluster")
      .withIndex("test")
      .withStartTimeout(2, TimeUnit.MINUTES)
      .build()
      .start()

    embeddedElastic.createIndex("test")

  }

  after {
    if (embeddedElastic != null) {
      embeddedElastic.stop()
    }

  }

  test("Basic Write ElasticSearch") {
    withTempDir { checkpointDir => {
      val pipeline = PipelineRunner(ResourcePipelineConfig("write/es.dsl"), Some(Settings.empty
        .withValue("stream.debug", "true")
        .withValue("stream.template.vars.port", port.toString)
        .withValue("stream.template.vars.checkPointDir", checkpointDir.getCanonicalPath)))
      pipeline.run()

      val indexDocuments = embeddedElastic.fetchAllDocuments("test")

      assert(indexDocuments.size() == 10)
    }
    }

  }


}