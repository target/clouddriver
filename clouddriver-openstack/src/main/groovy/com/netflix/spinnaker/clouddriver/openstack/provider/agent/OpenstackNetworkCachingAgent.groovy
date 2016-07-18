/*
 * Copyright 2016 Target, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.openstack.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.openstack.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.openstack.cache.Keys
import com.netflix.spinnaker.clouddriver.openstack.model.OpenstackNetwork
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackNamedAccountCredentials
import groovy.util.logging.Slf4j
import org.openstack4j.model.network.Network

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.openstack.provider.OpenstackInfrastructureProvider.ATTRIBUTES

@Slf4j
class OpenstackNetworkCachingAgent extends AbstractOpenstackCachingAgent {

  Collection<AgentDataType> providedDataTypes = Collections.unmodifiableSet([
    AUTHORITATIVE.forType(Keys.Namespace.NETWORKS.ns)
  ] as Set)

  final ObjectMapper objectMapper

  String agentType = "${accountName}/${region}/${OpenstackNetworkCachingAgent.simpleName}"

  OpenstackNetworkCachingAgent(OpenstackNamedAccountCredentials account, String region, final ObjectMapper objectMapper) {
    super(account, region)
    this.objectMapper = objectMapper
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<Network> networkList = clientProvider.listNetworks(region)
    buildCacheResult(networkList)
  }

  private CacheResult buildCacheResult(List<Network> networkList) {
    log.info("Describing items in ${agentType}")

    def cacheResultBuilder = new CacheResultBuilder()

    networkList.each { Network network ->
      String networkKey = Keys.getNetworkKey(network.id, accountName, region)

      Map<String, Object> networkAttributes = objectMapper.convertValue(OpenstackNetwork.from(network, accountName, region), ATTRIBUTES)

      cacheResultBuilder.namespace(Keys.Namespace.NETWORKS.ns).keep(networkKey).with {
        attributes = networkAttributes
      }
    }

    log.info("Caching ${cacheResultBuilder.namespace(Keys.Namespace.NETWORKS.ns).keepSize()} networks in ${agentType}")

    cacheResultBuilder.build()
  }

}
