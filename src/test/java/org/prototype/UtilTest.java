package org.prototype;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilTest {


    @Test
    public void checkStringContains(){

        String resourceName = "spaship-ui-config";
        boolean result = resourceName.contains("spaship-ui");
        assertTrue(result);

    }


}
