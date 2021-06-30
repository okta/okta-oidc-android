package com.okta.oidc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AuthClientPayloadTest.class,
        AuthenticationResultHandlerTest.class,
        CustomConfigurationTest.class,
        OIDCConfigTest.class,
        OktaAuthClientActivityTest.class,
        OktaIdTokenTest.class,
        OktaRedirectActivityTest.class,
        OktaResultFragmentTest.class,
        OktaStateTest.class,
        OktaTest.class,
        RequestDispatcherTest.class,
        TokensTest.class,
})
class SuiteAllTests {
}