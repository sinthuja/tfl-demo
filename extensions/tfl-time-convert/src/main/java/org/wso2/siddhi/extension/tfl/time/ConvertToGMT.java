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

package org.wso2.siddhi.extension.tfl.time;

import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.exception.ExecutionPlanRuntimeException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.ExecutionPlanValidationException;

import java.util.Calendar;

public class ConvertToGMT extends FunctionExecutor {

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ExecutionPlanContext executionPlanContext) {
        if (attributeExpressionExecutors.length != 1) {
            throw new ExecutionPlanValidationException("Invalid no of arguments passed to tfl:toGMT() function, " +
                    "required 1, but found " + attributeExpressionExecutors.length);
        }
        Attribute.Type attributeType = attributeExpressionExecutors[0].getReturnType();
        if (attributeType != Attribute.Type.LONG) {
            throw new ExecutionPlanValidationException("Invalid parameter type found for the argument of tfl:toGMT() function, " +
                    "required " + Attribute.Type.LONG +
                    ", but found " + attributeType.toString());
        }
    }

    @Override
    protected Object execute(Object[] data) {
        return null;  // Since the sin function takes in only 1 parameter, this method does not get called. Hence, not implemented.
    }

    @Override
    protected Object execute(Object data) {
        Calendar calInstance;
        Long timestamp;
        Long updatedTimeStamp;
        if (data != null) {
            timestamp = (Long) data;
            calInstance = Calendar.getInstance();
            int offset = calInstance.get(Calendar.ZONE_OFFSET);
            updatedTimeStamp = timestamp - offset;
        } else {
            throw new ExecutionPlanRuntimeException("Input to the tfl:toGMT() function cannot be null");
        }
        return updatedTimeStamp;
    }

    @Override
    public void start() {
        // Nothing to start.
    }

    @Override
    public void stop() {
        // Nothing to stop.
    }

    @Override
    public Attribute.Type getReturnType() {
        return Attribute.Type.LONG;
    }

    @Override
    public Object[] currentState() {
        return null;    // No need to maintain state.
    }

    @Override
    public void restoreState(Object[] state) {
        // Since there's no need to maintain a state, nothing needs to be done here.
    }
}
