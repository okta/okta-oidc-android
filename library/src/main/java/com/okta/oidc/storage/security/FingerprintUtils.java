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

package com.okta.oidc.storage.security;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

/**
 * Finger prints utils encapsulates API's for preforming Finger print authentication.
 */
public final class FingerprintUtils {
    private FingerprintUtils() {
    }

    /**
     * State of finger print sensor.
     */
    public enum SensorState {
        NOT_SUPPORTED,
        NOT_BLOCKED,
        NO_FINGERPRINTS,
        READY
    }

    /**
     * Checks whether device has finger print hardware.
     *
     * @param context application context.
     * @return true if device supports fingerprints.
     */
    public static boolean checkFingerprintCompatibility(@NonNull Context context) {
        return FingerprintManagerCompat.from(context).isHardwareDetected();
    }

    /**
     * Checks state of finger print mahanaer state.
     *
     * @param context application context.
     * @return current state.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static SensorState checkSensorState(@NonNull Context context) {
        if (checkFingerprintCompatibility(context)) {

            KeyguardManager keyguardManager = (KeyguardManager) context
                    .getSystemService(Context.KEYGUARD_SERVICE);
            if (!keyguardManager.isKeyguardSecure()) {
                return SensorState.NOT_BLOCKED;
            }

            if (!FingerprintManagerCompat.from(context).hasEnrolledFingerprints()) {
                return SensorState.NO_FINGERPRINTS;
            }

            return SensorState.READY;

        } else {
            return SensorState.NOT_SUPPORTED;
        }

    }

    /**
     * Runs check for finger print manager status.
     * @param state expected state
     * @param context application context.
     * @return true if state the same as expected.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isSensorStateAt(@NonNull SensorState state, @NonNull Context context) {
        return checkSensorState(context) == state;
    }
}
