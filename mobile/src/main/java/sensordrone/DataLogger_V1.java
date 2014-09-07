/*
   Copyright 2013 Sensorcon, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package sensordrone;

import java.nio.ByteBuffer;
import java.util.concurrent.RejectedExecutionException;

public class DataLogger_V1 extends DroneSensor {

    private DroneEventObject initDataRead = new DroneEventObject(DroneEventObject.droneEventType
            .DATA_LOGGER_INIT_DATA_READ);
    private DroneEventObject logRead = new DroneEventObject(DroneEventObject.droneEventType.DATA_LOGGER_LOG_READ);

    public byte[] getDataLogInitPacket() {
        return dataLogInitPacket;
    }

    private byte[] dataLogInitPacket;





    public DataLogger_V1(CoreDrone drone) {
        super(drone);
    }

    public DataLogger_V1(CoreDrone drone, String tag) {
        super(drone, tag);
    }


    public boolean sendInitializationPacket(final byte[] packet) {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable init_Runnable = new Runnable() {
            public void run() {
                byte[] response = sdCallAndResponse(packet);
            }
        };

        try {
            myDrone.commService.submit(init_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }

        return true;
    }

    public boolean dataLogRequestInitData() {

        if (!myDrone.isConnected) {
            return false;
        }

        Runnable request_Runnable = new Runnable() {
            public void run() {
                byte[] request = {0x50, 0x02, 0x53, 0x00};

                dataLogInitPacket = sdCallAndResponse(request);

                if (dataLogInitPacket != null) {
                    myDrone.notifyDroneEventHandler(initDataRead);
                }
            }
        };

        try {
            myDrone.commService.submit(request_Runnable);
        } catch (RejectedExecutionException e) {
            return false;
        }

        return true;
    }

    public boolean readDataLog(final byte[] call) {
        if (!myDrone.isConnected) {
            return false;
        }

        Runnable readLogRunnable = new Runnable() {
            @Override
            public void run() {
                byte[] response = sdDataLoggerCallAndResponse(call);

                if (response!= null) {
                    myDrone.dataLogBuffer = ByteBuffer.wrap(response);
                    myDrone.notifyDroneEventHandler(logRead);
                }
                else {
                    //
               }
            }
        };

        try {
            myDrone.commService.submit(readLogRunnable);
        } catch (RejectedExecutionException e) {
            return false;
        }

        return true;
    }


}
