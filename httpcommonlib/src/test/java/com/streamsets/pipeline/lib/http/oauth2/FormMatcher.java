/*
 * Copyright 2017 StreamSets Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.lib.http.oauth2;

import org.mockito.ArgumentMatcher;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

class FormMatcher implements ArgumentMatcher<Entity<Form>> {

  private final Form expectedForm;

  public FormMatcher(Entity<Form> expected) {
    this.expectedForm = expected.getEntity();
  }

  @Override
  public boolean matches(Entity<Form> argument) {
    return expectedForm.asMap().equalsIgnoreValueOrder(argument.getEntity().asMap());
  }
}
