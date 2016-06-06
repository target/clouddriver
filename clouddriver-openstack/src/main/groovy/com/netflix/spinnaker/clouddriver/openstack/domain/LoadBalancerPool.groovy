/*
 * Copyright 2016 Target, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.openstack.domain

import groovy.transform.AutoClone
import groovy.transform.Canonical

import java.util.regex.Matcher
import java.util.regex.Pattern

@AutoClone
@Canonical
class LoadBalancerPool {
  static final int DEFAULT_INTERAL_PORT = 8000
  static final String POOL_NAME_PREFIX = 'pool'

  String id
  String name
  String derivedName
  LoadBalancerProtocol protocol
  LoadBalancerMethod method
  String subnetId
  Integer internalPort
  String description

  void setName(String name) {
    this.name = name
    this.derivedName = String.format("%s-%s-%d", name, POOL_NAME_PREFIX, System.currentTimeMillis())
  }

  void setInternalPort(Integer port) {
    this.internalPort = port
    this.description = "internal_port=${internalPort}"
  }

  void setDescription(String description) {
    this.description = description
    this.internalPort = convertDescriptionToPort(description, DEFAULT_INTERAL_PORT)
  }

  static Integer convertDescriptionToPort(String description, int defaultPort) {
    Integer result = defaultPort

    Pattern p = Pattern.compile("(\\w*)=(\\d*)")
    Matcher m = p.matcher(description)
    if (m.find()) {
      try {
        result = Integer.parseInt(m.group(2))
      } catch (IndexOutOfBoundsException | NumberFormatException e) {
        // Do nothing
      }
    }
    result
  }
}
