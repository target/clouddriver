/*
 * Copyright 2016 Target, Inc.
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

package com.netflix.spinnaker.clouddriver.openstack.deploy.validators.loadbalancer

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.openstack.OpenstackOperation
import com.netflix.spinnaker.clouddriver.openstack.deploy.description.loadbalancer.OpenstackLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.openstack.deploy.validators.OpenstackAttributeValidator
import com.netflix.spinnaker.clouddriver.openstack.domain.PoolHealthMonitor
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@OpenstackOperation(AtomicOperations.UPSERT_LOAD_BALANCER)
@Component
class UpsertOpenstackLoadBalancerAtomicOperationValidator extends DescriptionValidator<OpenstackLoadBalancerDescription> {
  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Override
  void validate(List priorDescriptions, OpenstackLoadBalancerDescription description, Errors errors) {
    String context = "upsertOpenstackLoadBalancerAtomicOperationDescription"
    def validator = new OpenstackAttributeValidator(context, errors)

    if (!validator.validateCredentials(description.account, accountCredentialsProvider)) {
      return
    }

    // One or the other
    if (!description.id && !description.name) {
      ['id', 'name'].each {
        errors.rejectValue("${context}.${it}", "${context}.${it}.empty")
      }
      return
    }

    if (description.id) {
     validator.validateUUID(description.id, 'id')
    }

    if (description.name) {
      validator.validateNotEmpty(description.name, 'name')
    }

    // Required fields
    validator.validateNotEmpty(description.region, 'region')
    validator.validatePort(description.internalPort, 'internalPort')
    validator.validatePort(description.externalPort, 'externalPort')
    validator.validateUUID(description.subnetId, 'subnetId')

    if (!description.method) {
      validator.reject('method', 'method')
    }

    if (!description.protocol) {
      validator.reject('protocol', 'protocol')
    }

    // Optional
    if (description.healthMonitor) {
      PoolHealthMonitor healthMonitor = description.healthMonitor
      if (!healthMonitor.type) {
        validator.reject('type', 'type')
      }
      validator.validatePositive(healthMonitor.delay, 'delay')
      validator.validatePositive(healthMonitor.timeout, 'timeout')
      validator.validatePositive(healthMonitor.maxRetries, 'max retries')
      if (healthMonitor.httpMethod) {
        validator.validateHttpMethod(healthMonitor.httpMethod, 'httpMethod')
      }
      if (healthMonitor.expectedHttpStatusCodes) {
        healthMonitor.expectedHttpStatusCodes.each {
          validator.validateHttpStatusCode(it, 'statusCodes')
        }
      }
      if (healthMonitor.url) {
        validator.validateURL(healthMonitor.url, 'url')
      }
    }

    if (description.floatingIpId) {
      validator.validateUUID(description.floatingIpId, 'floatingIpId')
    }

    if (description.networkId) {
      validator.validateUUID(description.networkId, 'networkId')
    }
  }
}
