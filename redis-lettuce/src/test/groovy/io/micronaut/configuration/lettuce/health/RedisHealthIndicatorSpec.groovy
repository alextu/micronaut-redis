/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.lettuce.health

import io.lettuce.core.RedisClient
import io.micronaut.context.ApplicationContext
import io.micronaut.core.io.socket.SocketUtils
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import reactor.core.publisher.Flux
import redis.embedded.RedisServer
import spock.lang.Specification

/**
 * @author graemerocher
 * @since 1.0
 */
class RedisHealthIndicatorSpec extends Specification {

    private static String MAX_HEAP_SETTING = "maxmemory 256M"

    void "test redis health indicator"() {
        given:
        int port = SocketUtils.findAvailableTcpPort()
        RedisServer redisServer = RedisServer.builder().port(port).setting(MAX_HEAP_SETTING).build()
        redisServer.start()

        when:
        ApplicationContext applicationContext = ApplicationContext.run('redis.port':port)
        RedisClient client = applicationContext.getBean(RedisClient)

        then:
        client != null

        when:
        RedisHealthIndicator healthIndicator = applicationContext.getBean(RedisHealthIndicator)
        HealthResult result = Flux.from(healthIndicator.getResult()).blockFirst()
        
        then:
        result != null
        result.status == HealthStatus.UP

        when:
        redisServer?.stop()
        result = Flux.from(healthIndicator.getResult()).blockFirst()

        then:
        result != null
        result.status == HealthStatus.DOWN

        cleanup:
        applicationContext.close()
    }

    void "redis health indicator is not loaded when disabled"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run([
                'redis.health.enabled': 'false',
                'redis.type': 'embedded',
        ])
        RedisClient client = applicationContext.getBean(RedisClient)

        then:
        client != null

        when:
        Optional<RedisHealthIndicator> healthIndicator = applicationContext.findBean(RedisHealthIndicator)

        then:
        !healthIndicator.isPresent()

        cleanup:
        applicationContext.close()
    }
}
