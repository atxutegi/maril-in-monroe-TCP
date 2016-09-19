/**
 * *****************************************************************************
 * Copyright 2016-2017 University of the Basque Country (UPV/EHU)
 *
 * Code adaptation and development based on
 * https://github.com/alladin-IT/open-rmbt/tree/master/RMBTClient
 *
 * This code includes an adaptation and simplication of the software developed at:
 * alladin-IT GmbH (https://alladin.at/),
 * Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH) (https://www.rtr.at/)
 * and Specure GmbH (https://www.specure.com/).
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************
 */

package maril.client;

public class testParameters
{
     
    private final String host;
    private final int port;
    private final String token;
    private final int pretestDuration = 2;
    private final int duration;
    private final int numThreads;
    private final int numPings;
    private final long startTime;
    private final boolean pre_test;
    
    public testParameters(final String host, final int port, final String token,
            final int duration, final int numThreads, final int numPings, final long startTime, final boolean pre_test)
    {
        super();
        this.host = host;
        this.port = port;
        this.token = token;
        this.duration = duration;
        this.numThreads = numThreads;
        this.numPings = numPings;
        this.startTime = startTime;
        this.pre_test = pre_test; 
    }
    
    public testParameters(final String host, final int port, final String token,
            final int duration, final int numThreads, final int numPings, final boolean pre_test)
    {
        super();
        this.host = host;
        this.port = port;
        this.token = token;
        this.duration = duration;
        this.numThreads = numThreads;
        this.numPings = numPings;
        this.startTime = 0;
        this.pre_test = pre_test;
    }
    
    public String getHost()
    {
        return host;
    }
    
    public int getPort()
    {
        return port;
    }
            
    public String getToken()
    {
        return token;
    }
    
    public String getUUID()
    {
        if (token == null)
            return null;
        final String[] parts = token.split("_");
        if (parts == null || parts.length <= 0)
            return null;
        return parts[0];
    }
    
    public int getDuration()
    {
        return duration;
    }
    
    public int getPretestDuration()
    {
        return pretestDuration;
    }
    
    public int getNumThreads()
    {
        return numThreads;
    }
    
    public int getNumPings()
    {
        return numPings;
    }
    
    public long getStartTime()
    {
        return startTime;
    }
    
    public boolean getPreTest()
    {
        return pre_test;
    }

}
