/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.server.common.infra.property.env.service.env;

import im.turms.server.common.infra.property.metadata.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;

/**
 * @author James Chen
 */
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
public class FakeProperties {

    @Description("Whether to fake data. Note that faking only works in non-production environments")
    private boolean enabled;

    @Description("the total number of users to fake")
    @Min(0)
    private int userCount = 1000;

    @Description("Whether to clear all collections before faking at startup")
    private boolean clearAllCollectionsBeforeFaking;

    @Description("Whether to fake data even if the collection has already existed")
    private boolean fakeIfCollectionExists;

}