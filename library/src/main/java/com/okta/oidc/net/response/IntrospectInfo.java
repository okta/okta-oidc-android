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
package com.okta.oidc.net.response;

@SuppressWarnings("unused")
public final class IntrospectInfo {
    private boolean active;
    private String token_type;
    private String scope;
    private String client_id;
    private String device_id;
    private String username;
    private int nbf;
    private int exp;
    private int iat;
    private String sub;
    private String aud;
    private String iss;
    private String jti;
    private String uid;

    public boolean isActive() {
        return active;
    }

    public String getToken_type() {
        return token_type;
    }

    public String getScope() {
        return scope;
    }

    public String getClient_id() {
        return client_id;
    }

    public String getDevice_id() {
        return device_id;
    }

    public String getUsername() {
        return username;
    }

    public int getNbf() {
        return nbf;
    }

    public int getExp() {
        return exp;
    }

    public int getIat() {
        return iat;
    }

    public String getSub() {
        return sub;
    }

    public String getAud() {
        return aud;
    }

    public String getIss() {
        return iss;
    }

    public String getJti() {
        return jti;
    }

    public String getUid() {
        return uid;
    }
}
