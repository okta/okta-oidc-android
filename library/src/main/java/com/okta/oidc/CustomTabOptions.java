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

package com.okta.oidc;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.AnimRes;
import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;

/**
 * Custom tab options for color and animation transitions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CustomTabOptions implements Parcelable {
    public CustomTabOptions() {
    }

    @ColorInt
    private int mCustomTabColor;
    @AnimRes
    private int mStartEnterResId;
    @AnimRes
    private int mStartExitResId;
    @AnimRes
    private int mEndEnterResId;
    @AnimRes
    private int mEndExitResId;

    public int getCustomTabColor() {
        return mCustomTabColor;
    }

    public void setCustomTabColor(@ColorInt int customTabColor) {
        mCustomTabColor = customTabColor;
    }

    public int getStartEnterResId() {
        return mStartEnterResId;
    }

    public void setStartEnterResId(@AnimRes int startEnterResId) {
        mStartEnterResId = startEnterResId;
    }

    public int getStartExitResId() {
        return mStartExitResId;
    }

    public void setStartExitResId(@AnimRes int startExitResId) {
        mStartExitResId = startExitResId;
    }

    public int getEndEnterResId() {
        return mEndEnterResId;
    }

    public void setEndEnterResId(@AnimRes int endEnterResId) {
        mEndEnterResId = endEnterResId;
    }

    public int getEndExitResId() {
        return mEndExitResId;
    }

    public void setEndExitResId(@AnimRes int endExitResId) {
        mEndExitResId = endExitResId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCustomTabColor);
        dest.writeInt(mStartEnterResId);
        dest.writeInt(mStartExitResId);
        dest.writeInt(mEndEnterResId);
        dest.writeInt(mEndExitResId);
    }

    public static final Parcelable.Creator<CustomTabOptions> CREATOR =
            new Parcelable.Creator<CustomTabOptions>() {
                @Override
                public CustomTabOptions createFromParcel(Parcel source) {
                    CustomTabOptions options = new CustomTabOptions();
                    options.setCustomTabColor(source.readInt());
                    options.setStartEnterResId(source.readInt());
                    options.setStartExitResId(source.readInt());
                    options.setEndEnterResId(source.readInt());
                    options.setEndExitResId(source.readInt());
                    return options;
                }

                @Override
                public CustomTabOptions[] newArray(int size) {
                    return new CustomTabOptions[size];
                }
            };
}
