/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.oidc.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static Date getNow() {
        long nowMillis = System.currentTimeMillis();
        return new Date(nowMillis);
    }

    public static Date getTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }

    public static Date getYesterday() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, -1);
        return c.getTime();
    }

    public static Date getExpiredFromTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, 2);
        return c.getTime();
    }
}
