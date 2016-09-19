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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import maril.client.helper.TestStatus;

public class marilClientLaunch
{
    
/**
 * @param args
 * @throws IOException
 * @throws NoSuchAlgorithmException
 * @throws KeyManagementException
 */
public static void main(final String[] args) throws IOException, InterruptedException, KeyManagementException,
        NoSuchAlgorithmException
{
    final OptionParser parser = new OptionParser()
    {
        {                                             
            acceptsAll(Arrays.asList("h", "host"), "server/hostname").withRequiredArg()
                    .ofType(String.class);

            acceptsAll(Arrays.asList("p", "port"), "server port").withRequiredArg().ofType(
                    Integer.class);

            acceptsAll(Arrays.asList("t", "threads"), "number of threads")
                    .withRequiredArg().ofType(Integer.class);

            acceptsAll(Arrays.asList("d", "duration"), "test duration in seconds")
                    .withRequiredArg().ofType(Integer.class);
            
            acceptsAll(Arrays.asList("pre", "pre-test"), "pre-test yes/no")
                    .withRequiredArg().ofType(Boolean.class);

        }
    };
    System.out.println("STARTING POINT\n");

    OptionSet options;
    try
    {
        options = parser.parse(args);
    }
    catch (final OptionException e)
    {
        System.out.println(String.format("error while parsing command line options: %s", e.getLocalizedMessage()));
        System.exit(1);
        return;
    }

    final String[] requiredArgs = { "h", "p" };

    boolean reqArgMissing = false;
    if (!options.has("?"))
        for (final String arg : requiredArgs)
            if (!options.has(arg))
            {
                reqArgMissing = true;
                System.out.println(String.format("ERROR: required argument '%s' is missing", arg));
            }

    if (reqArgMissing)
    {
        System.out.println();
        parser.printHelpOn(System.out);
        System.exit(1);
        return;
    }   

    final marilClientParam client;

    //final String host = (String) options.valueOf("h");
    //final int port = (Integer) options.valueOf("p");
    String host="158.227.68.19";
    //String host = "85.17.254.6";
    int port = 3446;
    //int port = 5231;
    
    boolean pre_test=true;

    final ArrayList<String> geoInfo = null;

    //final String uuid = "1cc2d6bb-2f07-4cb8-8fd6-fb5ffcf10cb0";
    //String uuid = "1cc2d6bb-2f07-4cb8-8fd6-fb5ffcf10cb0";
    String uuid = "158f-43cb-912-191-fdf2-5b76-bca-4ef0";

    int numThreads = 1;
    int duration = 7;
    if (options.has("t"))
        numThreads = (Integer) options.valueOf("t");
    if (options.has("h"))
        host = (String) options.valueOf("h");
    if (options.has("p"))
        port = (Integer) options.valueOf("p");
    if (options.has("d"))
        duration = (Integer) options.valueOf("d");
    if (options.has("pre"))
        pre_test = (boolean) options.valueOf("pre");

    int numPings = 10;
    
    testParameters finalParameters = new testParameters(host, port, uuid, duration, numThreads, numPings, pre_test);


    // standard mode with contact to control server
    client = marilClientParam.getInstance(host, null, port, uuid,
        "DESKTOP", finalParameters);  	                    	
       
    
    if (client != null)
        {
            final TestResult result = client.runTest();
            
            if (result != null)
            {
                client.sendResult();
            }
            
            client.shutdown();
            client.setStatus(TestStatus.END);
                        
        }
    
    }

}
    

