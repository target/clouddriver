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

package com.netflix.spinnaker.clouddriver.openstack.deploy.validators.loadbalancer

import com.netflix.spinnaker.clouddriver.openstack.deploy.description.loadbalancer.OpenstackLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.openstack.domain.LoadBalancerMethod
import com.netflix.spinnaker.clouddriver.openstack.domain.LoadBalancerProtocol
import com.netflix.spinnaker.clouddriver.openstack.domain.PoolHealthMonitor
import com.netflix.spinnaker.clouddriver.openstack.domain.PoolHealthMonitorType
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackCredentials
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class UpsertOpenstackLoadBalancerDescriptionValidatorSpec extends Specification {

  String context = 'upsertOpenstackLoadBalancerAtomicOperationDescription'
  Errors errors
  AccountCredentialsProvider provider
  UpsertOpenstackLoadBalancerAtomicOperationValidator validator
  OpenstackNamedAccountCredentials credentials
  OpenstackCredentials credz

  def "Validate no exception - id, no health monitor or ip address"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      1 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      1 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(account: 'foo'
      , region: 'west'
      , id: UUID.randomUUID().toString()
      , internalPort: 80
      , externalPort: 80
      , subnetId: UUID.randomUUID().toString()
      , method: LoadBalancerMethod.ROUND_ROBIN
      , protocol: LoadBalancerProtocol.HTTP)

    when:
    validator.validate([], description, errors)

    then:
    0 * errors.rejectValue(_, _)
  }

  def "Validate no exception - name, no health monitor or ip address"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      1 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      1 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(account: 'foo'
      , region: 'west'
      , name: 'testlb'
      , internalPort: 80
      , externalPort: 80
      , subnetId: UUID.randomUUID().toString()
      , method: LoadBalancerMethod.ROUND_ROBIN
      , protocol: LoadBalancerProtocol.HTTP)

    when:
    validator.validate([], description, errors)

    then:
    0 * errors.rejectValue(_, _)
  }

  def "Validate no exception - name, health monitor, and ip address"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      1 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      1 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(account: 'foo'
      , region: 'west'
      , name: 'testlb'
      , internalPort: 80
      , externalPort: 80
      , subnetId: UUID.randomUUID().toString()
      , method: LoadBalancerMethod.ROUND_ROBIN
      , protocol: LoadBalancerProtocol.HTTP
      , floatingIpId: UUID.randomUUID().toString()
      , healthMonitor: new PoolHealthMonitor(type: PoolHealthMonitorType.PING, delay: 10, timeout: 10, maxRetries: 10))

    when:
    validator.validate([], description, errors)

    then:
    0 * errors.rejectValue(_, _)
  }

  def "Validate empty account exception"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      0 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      0 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(account: '')

    when:
    validator.validate([], description, errors)

    then:
    1 * errors.rejectValue(_, _)
  }

  def "Validate empty load balancer id or name exception"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      1 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      1 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(account: 'foo')

    when:
    validator.validate([], description, errors)

    then:
    ['id', 'name'].each {
      1 * errors.rejectValue("${context}.${it}", "${context}.${it}.empty")
    }
  }

  def "Validate missing required field"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      1 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      1 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    Map<String, ?> inputMap = ['account'       : 'foo', 'id': UUID.randomUUID().toString(), 'region': 'west', 'internalPort': 80
                               , 'externalPort': 80, subnetId: UUID.randomUUID().toString()
                               , 'method'      : LoadBalancerMethod.ROUND_ROBIN, 'protocol': LoadBalancerProtocol.HTTP]
    inputMap.remove(field)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(inputMap)

    when:
    validator.validate([], description, errors)

    then:
    1 * errors.rejectValue("${context}.${field}", _)

    where:
    field << ['region', 'internalPort', 'externalPort', 'subnetId', 'method', 'protocol']
  }

  def "Validate optional fields"() {
    given:
    credz = Mock(OpenstackCredentials)
    credentials = Mock(OpenstackNamedAccountCredentials) {
      1 * getCredentials() >> credz
    }
    provider = Mock(AccountCredentialsProvider) {
      1 * getCredentials(_) >> credentials
    }
    errors = Mock(Errors)
    validator = new UpsertOpenstackLoadBalancerAtomicOperationValidator(accountCredentialsProvider: provider)
    Map<String, ?> inputMap = ['account'       : 'foo', 'id': UUID.randomUUID().toString(), 'region': 'west', 'internalPort': 80
                               , 'externalPort': 80, subnetId: UUID.randomUUID().toString()
                               , 'method'      : LoadBalancerMethod.ROUND_ROBIN, 'protocol': LoadBalancerProtocol.HTTP]
    inputMap.put(key, value)
    OpenstackLoadBalancerDescription description = new OpenstackLoadBalancerDescription(inputMap)

    when:
    validator.validate([], description, errors)

    then:
    1 * errors.rejectValue("${context}.${key}", _)

    where:
    key             | value
    'networkId'     | '123'
    'floatingIpId'  | '123'
  }
}
