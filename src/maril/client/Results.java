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

import java.util.Arrays;

public class Results
{
    public final long[] bytes;
    public final long[] nsec;
    
    public Results(int numResults)
    {
        bytes = new long[numResults];
        nsec = new long[numResults];
    }
    
    public Results(long[] bytes, long[] nsec)
    {
        if (bytes.length != nsec.length)
            throw new IllegalArgumentException("length of bytes and nsec not identical");
        this.bytes = bytes;
        this.nsec = nsec;
    }

	@Override
	public String toString() {
		return "Results [bytes=" + Arrays.toString(bytes) + ", nsec="
				+ Arrays.toString(nsec) + "]";
	}
}
