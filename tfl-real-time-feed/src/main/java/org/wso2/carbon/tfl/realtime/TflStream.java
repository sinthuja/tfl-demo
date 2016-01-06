/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.tfl.realtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wso2.carbon.tfl.realtime.bus.Bus;
import org.wso2.carbon.tfl.realtime.busstop.BusStop;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class TflStream {
    public static final String endPointBus =
            "http://ec2-52-77-236-192.ap-southeast-1.compute.amazonaws.com:9763/endpoints/BusTrafficReceiver";
    public static HashMap<String, BusStop> map = new HashMap<String, BusStop>();
    public static ConcurrentHashMap<String, Bus> buses = new ConcurrentHashMap<String, Bus>();
    public static long timeOffset;
    public static long lastTime = 0;
    private static Log log = LogFactory.getLog(TflStream.class);

    public static void main(String[] args) throws XMLStreamException {
        try {
            Update update = new Update(System.currentTimeMillis(), 1000, endPointBus);
            GetData busData = new GetData();
            busData.start();
            log.info("started collecting data");
            Thread.sleep(30000);
            update.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void send(ArrayList<String> jsonList, String endPoint) {
        for (String data : jsonList) {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(endPoint);
            try {
                StringEntity entity = new StringEntity(data);
                post.setEntity(entity);
                HttpResponse response = client.execute(post);
                log.info("data sent : " + data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
