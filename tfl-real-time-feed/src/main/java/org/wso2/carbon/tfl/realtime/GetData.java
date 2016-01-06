/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.tfl.realtime;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.tfl.realtime.bus.BusStream;
import org.wso2.carbon.tfl.realtime.busstop.BusStop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetData extends Thread {
    public static final String busStopURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?LineID=%s&DirectionID=%s&ReturnList=StopPointName,StopID,StopPointType,Latitude,Longitude";
    public static final String liveBusURL = "http://countdown.api.tfl.gov.uk/interfaces/ura/instant_V1?LineID=%s&ReturnList=StopID,LineID,DirectionID,VehicleID,RegistrationNumber,EstimatedTime";
    public static final String[] busLineIds = new String[]{"29", "25", "38", "N29", "N25", "N38"};
    public static final List<String> validStopTypes = Arrays.asList("STBR", "STBC", "STBS", "STSS");
    public static final int[] directions = new int[]{1, 2};
    public static String busURL;
    private static Log log = LogFactory.getLog(GetData.class);

    public GetData() {
        super();
        busURL = String.format(liveBusURL, StringUtils.join(busLineIds, ','));
    }

    private static void getStops() {
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            ArrayList<String> stops = new ArrayList<String>();
            for (String lineId : busLineIds) {
                for (int direction : directions) {
                    String[] arr;
                    URL obj = new URL(String.format(busStopURL, lineId, direction));
                    con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");

                    int responseCode = con.getResponseCode();
                    log.info("\nSending 'GET' request to URL : " + busStopURL);
                    log.info("Response Code : " + responseCode);

                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    inputLine = in.readLine();
                    inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
                    arr = inputLine.split(",");
                    TflStream.timeOffset = System.currentTimeMillis() - Long.parseLong(arr[2]);

                    while ((inputLine = in.readLine()) != null) {
                        inputLine = inputLine.replaceAll("[\\[\\]\"]", "");
                        arr = inputLine.split(",");
                        if (!stops.contains(arr[2]) && validStopTypes.contains(arr[3])) {
                            String line = (lineId.contains("N")) ? lineId.substring(1) : lineId;
                            BusStop busStop = new BusStop(arr[2], arr[1], direction, Double.parseDouble(arr[4]), Double.parseDouble(arr[5]), line);
                            TflStream.map.put(arr[2], busStop);
                            stops.add(arr[2]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("IOException while reading bus stop data: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Exception while reading bus stop data: " + e.getMessage(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (con != null) {
                    con.disconnect();
                }
            } catch (IOException e) {
                log.error("Error while closing stream: " + e.getMessage(), e);
            }
        }
    }

    public void run() {
        getStops();
        getBus();
    }

    private void getBus() {
        BusStream b;
        long time = System.currentTimeMillis();
        int i = 0;
        while (true) {
            String url = busURL;
            if (busURL.contains("localhost"))
                url += i + ".txt";
            log.info(url);
            b = new BusStream(url);
            b.start();
            try {
                time += 30000;
                Thread.sleep(time - System.currentTimeMillis());
            } catch (InterruptedException ignored) {
            }
            i = (i + 1) % 100;
        }
    }
}

