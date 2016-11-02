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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Scanner;

//import maril.client.helper.Config;

public abstract class AbstractTCPbasedTest {
    	
    protected final marilClientParam client;
    protected final testParameters params;
    protected final int threadId;

    protected InputStreamCounter in;
    protected BufferedReader reader;
    protected OutputStreamCounter out;
    
    protected long totalDown;
    protected long totalUp;
    protected int framesize;
    protected byte[] buf;

    
    /**
     * 
     * @param client
     * @param params
     * @param threadId
     */
    public AbstractTCPbasedTest(marilClientParam client, testParameters params, int threadId) {
    	this.threadId = threadId;
    	this.client = client;
    	this.params = params;
    }

    protected Socket getSocket(final String host, final int port, final int timeOut) throws UnknownHostException, IOException
    {
    	InetSocketAddress sockAddr = new InetSocketAddress(host, port);
    	
    	final Socket socket;
    	        
        socket = new Socket();
                
        if (socket != null) {
        	System.out.println("Connecting to " + sockAddr + " with timeout: " + timeOut + "ms");
        	//socket.connect(sockAddr, timeOut);
                socket.connect(sockAddr);
        }
        
    	return socket;
    }

    protected Socket connect(final TestResult testResult, final InetAddress host, final int port, final String response, final int connTimeOut) throws IOException {
        log(String.format(Locale.US, "thread %d: connecting...", threadId));
        
        final InetAddress inetAddress = host;
        
        final Socket s = getSocket(inetAddress.getHostAddress(), port, connTimeOut);
        s.setSoTimeout(12000);
        
        if (testResult != null) {
        	testResult.ip_local = s.getLocalAddress();
        	testResult.ip_server = s.getInetAddress();
        	testResult.port_remote = s.getPort();
        }
        
                
        log(String.format(Locale.US, "thread %d: ReceiveBufferSize: '%s'.", threadId, s.getReceiveBufferSize()));
        log(String.format(Locale.US, "thread %d: SendBufferSize: '%s'.", threadId, s.getSendBufferSize()));
        
        if (in != null)
            totalDown += in.getCount();
        if (out != null)
            totalUp += out.getCount();
        
        in = new InputStreamCounter(s.getInputStream());
        reader = new BufferedReader(new InputStreamReader(in, "US-ASCII"), 4096);
        out = new OutputStreamCounter(s.getOutputStream());
                
        String line = reader.readLine();
        if (!line.startsWith("ACCEPT "))
        {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'ACCEPT'", threadId, line));
            return null;
        }
        
        final String send = String.format(Locale.US, "FINGERPRINT %s\n", params.getToken());
        
        out.write(send.getBytes("US-ASCII"));
        
        line = reader.readLine();
        
        if (line == null)
        {
            log(String.format(Locale.US, "thread %d: got no answer expected 'OK'", threadId, line));
            return null;
        }
        else if (!line.equals("OK"))
        {
            log(String.format(Locale.US, "thread %d: got '%s' expected 'OK'", threadId, line));
            return null;
        }
        
        line = reader.readLine();
        final Scanner scanner = new Scanner(line);
        try
        {
        	if (response.equals("FRAMESIZE")) {
                if (!response.equals(scanner.next()))
                {
                    log(String.format(Locale.US, "thread %d: got '%s' expected 'FRAMESIZE'", threadId, line));
                    return null;
                }
                try
                {
                    framesize = scanner.nextInt();
                    log(String.format(Locale.US, "thread %d: FRAMESIZE is %d", threadId, framesize));
                }
                catch (final Exception e)
                {
                    log(String.format(Locale.US, "thread %d: invalid FRAMESIZE: '%s'", threadId, line));
                    return null;
                }
                if (buf == null || buf != null && buf.length != framesize)
                    buf = new byte[framesize];
                
                s.setSoTimeout(0);
                return s;        		
        	}
        	else {
        		log(String.format(Locale.US, "thread %d: got '%s'", threadId, line));
        		s.setSoTimeout(0);
        		return s;
        	}

        }
        finally
        {
            scanner.close();
        }    	
    }
    
    protected Socket connect(final TestResult testResult) throws IOException
    {
    	return connect(testResult, InetAddress.getByName(params.getHost()), params.getPort(), "FRAMESIZE", 20000);
    }
    
    /**
     * 
     * @param message
     * @return
     * @throws IOException 
     * @throws UnsupportedEncodingException 
     */
    protected void sendMessage(final String message) throws UnsupportedEncodingException, IOException {
        String send;
        send = String.format(Locale.US, message);        	

        System.out.println("sending command (thread " + Thread.currentThread().getId() + "): " + send);
		out.write(send.getBytes("US-ASCII"));
        out.flush();
    }
    
    protected void log(final CharSequence text)
    {
        client.log(text);
    }

}
