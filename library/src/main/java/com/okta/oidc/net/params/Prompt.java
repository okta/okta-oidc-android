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
package com.okta.oidc.net.params;

@SuppressWarnings("unused")
public interface Prompt {
    String NONE = "none";
    String LOGIN = "login";
    String CONSENT = "consent";
    //TODO is this supported? not documented on okta api page.
    //https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1
    String SELECT_ACCOUNT = "select_account";
}
