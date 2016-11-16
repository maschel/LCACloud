/*
 *  LCACloud
 *
 *  MIT License
 *
 *  Copyright (c) 2016
 *
 *  Geoffrey Mastenbroek, geoffrey.mastenbroek@student.hu.nl
 *  Feiko Wielsma, feiko.wielsma@student.hu.nl
 *  Robbin van den Berg, robbin.vandenberg@student.hu.nl
 *  Arnoud den Haring, arnoud.denharing@student.hu.nl
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.maschel.lca.cloud.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Created by robbi on 16-11-2016.
 */
public class CloudDeviceAgent extends Agent {

    private static final String SENSOR_ONTOLOGY = "sensor";
    private static final String SENSOR_LIST_ONTOLOGY = "sensorlist";
    private static final String ACTUATOR_ONTOLOGY = "actuator";
    private static final String JSON_ENCODING = "json";

    protected void setup() {
        addBehaviour(new MessagePerformer());
    }

    // RECEIVE MESSAGES
    private class MessagePerformer extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mtPerformative = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mtPerformative);
            if(msg != null) {
                switch(msg.getOntology()) {
                    case SENSOR_ONTOLOGY:
                        myAgent.addBehaviour(new SensorBehaviour(msg));
                        break;
                    case SENSOR_LIST_ONTOLOGY:
                        break;
                    case ACTUATOR_ONTOLOGY:
                        break;
                }
            }
            else {
                block();
            }
        }

    }

    /**
     * SensorBehaviour, is called on a sensor request message
     */
    private class SensorBehaviour extends OneShotBehaviour {

        private ACLMessage message;

        public SensorBehaviour(ACLMessage msg) {
            this.message = msg;
        }

        @Override
        public void action() {
            // (json) string containing sensor name, type and value
            String content = message.getContent();
        }
    }

    // SEND MESSAGES
    protected void sendMessage(ACLMessage message, String content) {
        message.setEncoding(JSON_ENCODING);
        message.setContent(content);
        send(message);
    }

    public void sendSensorMessage(String sensorName) {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.setOntology(SENSOR_ONTOLOGY);
        sendMessage(message, sensorName);
    }

    public void sendActuatorMessage(String actuatorName, List<Object> args) {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.setOntology(ACTUATOR_ONTOLOGY);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", actuatorName);
        jsonObject.put("arguments", JSONArray.toJSONString(args));

        sendMessage(message, jsonObject.toJSONString());
    }
}
