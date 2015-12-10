/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.extension.tfl.route.test;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;

public class BusRouteFinderTestcase {
    static final Logger logger = Logger.getLogger(BusRouteFinderTestcase.class);
    protected static SiddhiManager siddhiManager;
    private int count;
    private double betaZero, betaTwo, forecastY;
    private boolean outlier;

    @Before
    public void init() {
        count = 0;
    }

    @Test
    public void simpleRegressionTest() throws Exception {
        logger.info("Simple Regression TestCase");

        siddhiManager = new SiddhiManager();
        String inputStream = "@config(async = 'true')define stream InputStream (y int, x int);";

        String executionPlan = ("@info(name = 'query1') from InputStream#timeseries:regress(1, 100, 0.95, y, x) "
                + "select * "
                + "insert into OutputStream;");
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inputStream + executionPlan);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents,
                                Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                count = count + inEvents.length;
                betaZero = (Double) inEvents[inEvents.length - 1].getData(3);
            }
        });
        InputHandler inputHandler = executionPlanRuntime.getInputHandler("InputStream");
        executionPlanRuntime.start();

        System.out.println(System.currentTimeMillis());

        inputHandler.send(new Object[]{2500.00, 17.00});

        Thread.sleep(100);

        Assert.assertEquals("No of events: ", 50, count);
        Assert.assertEquals("Beta0: ", 573.1418421169493, betaZero, 573.1418421169493 - betaZero);

        executionPlanRuntime.shutdown();

    }

}
