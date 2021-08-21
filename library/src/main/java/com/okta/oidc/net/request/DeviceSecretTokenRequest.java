package com.okta.oidc.net.request;


import android.net.Uri;

import androidx.annotation.RestrictTo;

import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.params.TokenTypeHint;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DeviceSecretTokenRequest extends TokenRequest {
    private static final String TAG = DeviceSecretTokenRequest.class.getSimpleName();

    DeviceSecretTokenRequest() {
    }

    DeviceSecretTokenRequest(HttpRequestBuilder.DeviceSecretTokenExchange b) {
        super();
        mRequestType = b.mRequestType;
        mConfig = b.mConfig;
        mProviderConfiguration = b.mProviderConfiguration;
        mUri = Uri.parse(mProviderConfiguration.token_endpoint);
        client_id = b.mConfig.getClientId();
        grant_type = b.mGrantType;
        mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.POST)
                .setRequestProperty("Accept", ConnectionParameters.JSON_CONTENT_TYPE)
                .setPostParameters(buildParameters(b))
                .setRequestType(mRequestType)
                .create();
    }

    protected Map<String, String> buildParameters(HttpRequestBuilder.DeviceSecretTokenExchange b) {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", client_id);
        params.put("grant_type", grant_type);
        params.put("actor_token", b.mActorToken);
        params.put("actor_token_type", TokenTypeHint.ACTOR_TOKEN_TYPE);
        params.put("subject_token", b.mSubjectToken);
        params.put("subject_token_type", TokenTypeHint.SUBJECT_TOKEN_TYPE);
        //requires API 24
        //params.put("scope", Arrays.stream(b.mConfig.getScopes()).reduce("", (a, e) -> a + " " + e));
        String scope = b.mConfig.getScopes()[0];
        for (int i=1; i<b.mConfig.getScopes().length; scope += " " + b.mConfig.getScopes()[i++])
        params.put("scope", scope);
        params.put("audience", b.mConfig.getAudience());
        return params;
    }
}
