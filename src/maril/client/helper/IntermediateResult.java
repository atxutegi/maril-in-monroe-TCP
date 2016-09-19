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

package maril.client.helper;

public class IntermediateResult
{
    public long pingNano;
    public long downBitPerSec;
    public long upBitPerSec;
    public TestStatus status;
    public float progress;
    public double downBitPerSecLog;
    public double upBitPerSecLog;
    public long remainingWait;
    
    public void setLogValues()
    {
        downBitPerSecLog = toLog(downBitPerSec);
        upBitPerSecLog = toLog(upBitPerSec);
    }
    
    public static double toLog(final long value)
    {
        if (value < 1e5)
            return 0;
        return (2d + Math.log10(value / 1e7)) / 4d;
        // value in bps
        // < 0.1 -> 0
        // 0.1 Mbps -> 0
        // 1000 Mbps -> 1
        // > 1000 Mbps -> >1
    }
}
