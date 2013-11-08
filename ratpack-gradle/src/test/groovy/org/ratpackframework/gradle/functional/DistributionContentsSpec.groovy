/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.gradle.functional

class DistributionContentsSpec extends FunctionalSpec {

  def "distribution contains src/ratpack"() {
    given:
    file("src/ratpack/ratpack.properties") << "foo"

    when:
    run "distZip"

    then:
    unzip(distZip, file("unpacked"))
    file("unpacked/test-app-1.0/app/ratpack.properties").text == "foo"
  }

}
