package com.okta.oidc.net.request;

import com.okta.oidc.util.AsciiStringListUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RefreshTokenRequest extends TokenRequest {
    RefreshTokenRequest(HttpRequestBuilder b) {
        super(b);
    }

    protected Map<String, String> buildParameters(HttpRequestBuilder b) {
        scope = b.mTokenResponse.getScope();
        refresh_token = b.mTokenResponse.getRefreshToken();
        Map<String, String> params = new HashMap<>();
        params.put("client_id", client_id);
        params.put("grant_type", grant_type);
        params.put("refresh_token", refresh_token);

        params.put("scope", AsciiStringListUtil.iterableToString(Collections.singletonList(scope)));
        return params;
    }
}
