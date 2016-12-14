package com.a5corp.weather.test;

import android.test.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by Xparker0i on 14-12-2016.
 */

public class FullTestSuite extends TestSuite {
    public static Test suite () {
        return new
                TestSuiteBuilder(FullTestSuite.class)
                .includeAllPackagesUnderHere()
                .build();
    }

    public FullTestSuite() {
        super();
    }
}
