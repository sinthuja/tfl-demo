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

package org.wso2.siddhi.extension.tfl.route;

import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.ExecutionPlanValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusRouteFinder extends StreamProcessor {
    private static Map<String, String> nextStopMap = new HashMap<>();
    private static Map<String, Double[]> stopPointCoords = new HashMap<>();
    private VariableExpressionExecutor startLatitudeExecutor;
    private VariableExpressionExecutor startLongitudeExecutor;
    private VariableExpressionExecutor endLatitudeExecutor;
    private VariableExpressionExecutor endLongitudeExecutor;
    //    private VariableExpressionExecutor geoFenceExecutor;
//    private GeoWithinFunctionExecutor withinFunctionExecutor;

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
                                   ExecutionPlanContext executionPlanContext) {
        if (attributeExpressionExecutors.length != 4) {
            throw new ExecutionPlanValidationException("Invalid no of arguments passed to tfl:busroute(<attr> " +
                    "startLat, <attr> startLon, <attr> endLat, <attr> endLon) function, required 4 arguments, but " +
                    "found " + attributeExpressionExecutors.length);
        }
        startLatitudeExecutor = ((VariableExpressionExecutor) attributeExpressionExecutors[0]);
        startLongitudeExecutor = ((VariableExpressionExecutor) attributeExpressionExecutors[1]);
        endLatitudeExecutor = ((VariableExpressionExecutor) attributeExpressionExecutors[2]);
        endLongitudeExecutor = ((VariableExpressionExecutor) attributeExpressionExecutors[3]);

//        ConstantExpressionExecutor constantExpressionExecutor =
//                "{'type': 'Circle', 'radius': 110575, 'coordinates':[" + latitude + ", " + longitude + "]}", Attribute.Type.STRING);
//
//        withinFunctionExecutor = new GeoWithinFunctionExecutor();
//        withinFunctionExecutor.initExecutor(new ExpressionExecutor[]{latitudeExecutor, longitudeExecutor, geoFenceConstantExpressionExecutor});

        ArrayList<Attribute> attributes = new ArrayList<Attribute>(2);
        attributes.add(new Attribute("startStopCode", Attribute.Type.STRING));
        attributes.add(new Attribute("endStopCode", Attribute.Type.STRING));
        return attributes;
    }

    private double distance(double lat1, double lat2, double lon1,
                            double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    private String findNearestStopPoint(double latitude, double longitude) {
        double minDistance = Double.MAX_VALUE;
        double currentDistance;
        String nearestStopPoint = null;
        for (Map.Entry<String, Double[]> stopCoordEntry : stopPointCoords.entrySet()) {
            currentDistance = distance(latitude, longitude, stopCoordEntry.getValue()[0], stopCoordEntry.getValue()[1], 0.0, 0.0);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                nearestStopPoint = stopCoordEntry.getKey();
            }
        }

        return nearestStopPoint;
    }

    private boolean isPossiblePath(String startStopPoint, String endStopPoint) {
        String currentStopPoint = startStopPoint;
        while (!currentStopPoint.equals(endStopPoint)) {
            currentStopPoint = nextStopMap.get(currentStopPoint);
            if (currentStopPoint == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> complexEventChunk, Processor processor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        Object[] data = new Object[]{Boolean.TRUE};
        while (complexEventChunk.hasNext()) {
            ComplexEvent complexEvent = complexEventChunk.next();
            double startLat = (Double) startLatitudeExecutor.execute(complexEvent);
            double startLon = (Double) startLongitudeExecutor.execute(complexEvent);
            double endLat = (Double) endLatitudeExecutor.execute(complexEvent);
            double endLon = (Double) endLongitudeExecutor.execute(complexEvent);
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


