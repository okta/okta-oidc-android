package com.okta.oidc.net.response;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class UserInfoTest {
    UserInfo validUserInfo;
    UserInfo userInfoWithNull;

    private final String FIRST_KEY = "FIRST_KEY";
    private final String FIRST_VALUE = "FIRST_VALUE";
    private JSONObject valid;

    @Before
    public void setUp() throws Exception {
        valid = new JSONObject();
        valid.put(FIRST_KEY, FIRST_VALUE);
        validUserInfo = new UserInfo(valid);

        userInfoWithNull = new UserInfo(null);
    }


    @Test
    public void getValueFromTest() {
        assertEquals(validUserInfo.get(FIRST_KEY),FIRST_VALUE);
        assertEquals(validUserInfo.getRaw(),valid);
    }

    @Test
    public void getValueFromInvalidObjectTest() {
        assertNull(userInfoWithNull.get(FIRST_KEY));
        assertNull(userInfoWithNull.getRaw());
    }
}
