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

package com.netflix.spinnaker.clouddriver.openstack.deploy.description.servergroup

import spock.lang.Specification

class ServerGroupParametersSpec extends Specification {

  def "test toParamsMap"() {
    given:
    ServerGroupParameters.Scaler scaleup = new ServerGroupParameters.Scaler(cooldown: 60, period: 60, adjustment: 1, threshold: 50)
    ServerGroupParameters.Scaler scaledown = new ServerGroupParameters.Scaler(cooldown: 60, period: 600, adjustment: -1, threshold: 15)
    ServerGroupParameters params = new ServerGroupParameters(instanceType: "m1.medium", image: "image",
      internalPort: 8443, maxSize: 5, minSize: 3, desiredSize: 4,
      networkId: "net", subnetId: "sub", poolId: "poop",
      securityGroups: ["sg1"],
      autoscalingType: ServerGroupParameters.AutoscalingType.CPU,
      scaleup: scaleup, scaledown: scaledown)
    Map expected = [flavor:'m1.medium', image:'image', internal_port:8443, max_size:5, min_size:3, desired_size:4,
                    network_id:'net', subnet_id:'sub', pool_id:'poop', security_groups:'sg1', autoscaling_type:'cpu_util',
                    scaleup_cooldown:60, scaleup_adjustment:1, scaleup_period:60, scaleup_threshold:50,
                    scaledown_cooldown:60, scaledown_adjustment:-1, scaledown_period:600, scaledown_threshold:15]


    when:
    Map result = params.toParamsMap()

    then:
    //need to compare string values due to some map formatting issue
    result.toString() == expected.toString()
  }

  def "test fromParamsMap"() {
    given:
    ServerGroupParameters.Scaler scaleup = new ServerGroupParameters.Scaler(cooldown: 60, period: 60, adjustment: 1, threshold: 50)
    ServerGroupParameters.Scaler scaledown = new ServerGroupParameters.Scaler(cooldown: 60, period: 600, adjustment: -1, threshold: 15)
    ServerGroupParameters expected = new ServerGroupParameters(instanceType: "m1.medium", image: "image",
      internalPort: 8443, maxSize: 5, minSize: 3, desiredSize: 4,
      networkId: "net", subnetId: "sub", poolId: "poop",
      securityGroups: ["sg1"],
      autoscalingType: ServerGroupParameters.AutoscalingType.CPU,
      scaleup: scaleup, scaledown: scaledown)
    Map params = [flavor:'m1.medium', image:'image', internal_port:8443, max_size:5, min_size:3, desired_size:4,
                    network_id:'net', subnet_id:'sub', pool_id:'poop', security_groups:'sg1', autoscaling_type:'cpu_util',
                    scaleup_cooldown:60, scaleup_adjustment:1, scaleup_period:60, scaleup_threshold:50,
                    scaledown_cooldown:60, scaledown_adjustment:-1, scaledown_period:600, scaledown_threshold:15]


    when:
    def result = ServerGroupParameters.fromParamsMap(params)

    then:
    result == expected
  }

}
