/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.siddhi.extension.tfl;

import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.exception.ExecutionPlanCreationException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.ExecutionPlanValidationException;

import java.lang.Long;
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Invoke extends StreamProcessor {
    private static Map<Long, Long> diffMap = new HashMap<Long, Long>();
    private long diff = 0L;
    private long reOrderDiff = 0L;
    private VariableExpressionExecutor timeStampExecuter;

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
                                   ExecutionPlanContext executionPlanContext) {
        if (attributeExpressionExecutors.length != 2) {
            throw new ExecutionPlanValidationException("Invalid no of arguments passed to tfl:invoke(<attr> " +
                    "timeStamp, <long> timeDiff) function, required 2 arguments, but " +
                    "found " + attributeExpressionExecutors.length);
        }
        if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.LONG) {
            throw new ExecutionPlanCreationException("Second parameter should be of type long");
        }
        timeStampExecuter = ((VariableExpressionExecutor) attributeExpressionExecutors[0]);
        diff = (Long) attributeExpressionExecutors[1].execute(null);
        reOrderDiff = diff * 2;

        // invoke attribute
        ArrayList<Attribute> attributes = new ArrayList<Attribute>(1);
        attributes.add(new Attribute("invoke", Attribute.Type.BOOL));
        return attributes;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> complexEventChunk, Processor processor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        Object[] data = new Object[]{Boolean.TRUE};
        boolean found = false;
        long lastTimeStamp = 0L;
        if (diffMap.containsKey(diff)) {
            lastTimeStamp = diffMap.get(diff);
        }
        while (complexEventChunk.hasNext()) {
            ComplexEvent complexEvent = complexEventChunk.next();
            Long currentTimeStamp = (Long) timeStampExecuter.execute(complexEvent);
            if (currentTimeStamp - lastTimeStamp <= diff) {
                if (lastTimeStamp - currentTimeStamp >= reOrderDiff) {
                    diffMap.put(diff, currentTimeStamp);
                    lastTimeStamp = currentTimeStamp;
                }
                complexEventChunk.remove();
            } else {
                if (!found) {
                    complexEventPopulater.populateComplexEvent(complexEvent, data);
                    diffMap.put(diff, currentTimeStamp);
                    found = true;
                } else {
                    complexEventChunk.remove();
                }
            }
        }
        nextProcessor.process(complexEventChunk);
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        // Do nothing
    }

    @Override
    public Object[] currentState() {
        return new Object[0];
    }

    @Override
    public void restoreState(Object[] state) {
        // Do nothing
    }
}


