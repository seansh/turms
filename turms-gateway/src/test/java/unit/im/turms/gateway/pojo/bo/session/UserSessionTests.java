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

package unit.im.turms.gateway.pojo.bo.session;

import im.turms.common.constant.DeviceType;
import im.turms.gateway.pojo.bo.session.UserSession;
import im.turms.gateway.throttle.TokenBucketContext;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author James Chen
 */
class UserSessionTests {

    private final int version = 1;
    private final Long userId = 1L;
    private final DeviceType deviceType = DeviceType.ANDROID;
    private final Point loginLocation = new Point(1F, 1F);
    private final TokenBucketContext requestTokenBucketContext = new TokenBucketContext();

    @Test
    void constructor_shouldReturnInstance() {
        UserSession userSession = new UserSession(
                version,
                userId,
                deviceType,
                loginLocation,
                requestTokenBucketContext);
        assertThat(userSession).isNotNull();
    }

    @Test
    void getters_shouldGetValues() {
        UserSession userSession = new UserSession(
                version,
                userId,
                deviceType,
                loginLocation,
                requestTokenBucketContext);
        assertThat(userSession.getDeviceType()).isEqualTo(deviceType);
        assertThat(userSession.getLoginLocation()).isEqualTo(loginLocation);
    }

}
