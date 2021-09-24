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

package im.turms.server.common.property.env.gateway.clientapi;

import com.fasterxml.jackson.annotation.JsonView;
import im.turms.server.common.property.metadata.annotation.Description;
import im.turms.server.common.property.metadata.annotation.GlobalProperty;
import im.turms.server.common.property.metadata.view.MutablePropertiesView;
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
public class RateLimitingProperties {

    @Description("The maximum number of tokens that the bucket can hold")
    @GlobalProperty
    @JsonView(MutablePropertiesView.class)
    @Min(1)
    private int capacity = 50;

    @Description("The initial number of tokens for new session")
    @GlobalProperty
    @JsonView(MutablePropertiesView.class)
    @Min(0)
    private int initialTokens = 50;

    @Description("Refills the bucket with the specified number of tokens per period if the bucket isn't full")
    @GlobalProperty
    @JsonView(MutablePropertiesView.class)
    @Min(1)
    private int tokensPerPeriod = 1;

    @Description("The time interval to refill. 0 means never refill")
    @GlobalProperty
    @JsonView(MutablePropertiesView.class)
    @Min(0)
    private int refillIntervalMillis = 1000;

}