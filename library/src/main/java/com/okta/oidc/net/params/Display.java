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

/**
 * Optional string value that specifies how the Authorization Server displays the
 * authentication and consent user interface pages to the End-User.
 */
@SuppressWarnings("unused")
public interface Display {
    /**
     * Display the authentication and consent UI consistent with a full User Agent page view.
     * If the display parameter is not specified, this is the default display mode.
     */
    String PAGE = "page";

    /**
     * Display the authentication and consent UI consistent with a popup User Agent window.
     * The popup User Agent window should be of an appropriate size for a login-focused dialog and
     * should not obscure the entire window that it is popping up over.
     */
    String POPUP = "popup";

    /**
     * Display the authentication and consent UI consistent with a device that leverages a touch interface.
     */
    String TOUCH = "touch";
}
