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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import maril.client.TCPbasedTest.CurrentSpeed;
import maril.client.helper.IntermediateResult;
import maril.client.helper.TestStatus;
import maril.client.helper.TestMeasurement;
import maril.client.helper.TrafficService;

public class marilClientParam
{
    private static final ExecutorService COMMON_THREAD_POOL = Executors.newCachedThreadPool();
    
    private final testParameters params;
    
    private final long durationInitNano = 2500000000L; // TODO
    private final long durationUpNano;
    private final long durationDownNano;
    
    private final AtomicLong pingNano = new AtomicLong(-1);
    private final AtomicLong downBitPerSec = new AtomicLong(-1);
    private final AtomicLong upBitPerSec = new AtomicLong(-1);
    
    /* ping status */
    private final AtomicLong pingTsStart = new AtomicLong(-1);
    private final AtomicInteger pingNumDome = new AtomicInteger(-1);
    private final AtomicLong pingTsLastPing = new AtomicLong(-1);
    
    private final static long MIN_DIFF_TIME = 10000000; // 10 ms
    
    private final static int KEEP_LAST_ENTRIES = 20;
    private int lastCounter;
    private final long[][] lastTransfer;
    private final long[][] lastTime;
    
    private final ExecutorService testThreadPool;
    
    private final TCPbasedTest[] testTasks;
    
    private TotalTestResult result; 
    
    private final boolean outputToStdout = true;
    
    private final AtomicBoolean aborted = new AtomicBoolean();
    
    private String errorMsg = "";
        
    private final AtomicReference<TestStatus> testStatus = new AtomicReference<TestStatus>(TestStatus.WAIT);
    private final AtomicReference<TestStatus> statusBeforeError = new AtomicReference<TestStatus>(null);
    private final AtomicLong statusChangeTime = new AtomicLong();
    
    private TrafficService trafficService;
    private ConcurrentHashMap<TestStatus, TestMeasurement> measurementMap = new ConcurrentHashMap<TestStatus, TestMeasurement>();
    
    public static ExecutorService getCommonThreadPool()
    {
        return COMMON_THREAD_POOL;
    }
    
    public static marilClientParam getInstance(final String host, final String pathPrefix, final int port,
            final String uuid, final String clientType, final testParameters overrideParams) 
    {
    	return getInstance(host, pathPrefix, port, uuid, clientType, overrideParams, 0);
    }
    
    public static marilClientParam getInstance(final String host, final String pathPrefix, final int port,
            final String uuid, final String clientType, final testParameters overrideParams, final int marc)
    {
        
        final testParameters params = getTestParameter(overrideParams);
        System.out.println("1.- Host " + params.getHost() + " port " + params.getPort() + " uuid " + params.getToken() + " duration " + params.getDuration() + " threads " + params.getNumThreads() + " preTest " + params.getPreTest());
        
        return new marilClientParam(params);
    }
    
    public static marilClientParam getInstance(final testParameters params)
    {
        return new marilClientParam(params);
    }
    
    public static testParameters getTestParameter(testParameters overrideParams)
    {
        String host = "";
        int port = 0;
        int duration = 0;
        int numThreads = 0;
        int numPings = 0;
        long testTime = 0;
        String uuid = "";
        int pre_test = 1;
                
        if (overrideParams != null)
        {
            if (overrideParams.getHost() != null && overrideParams.getPort() > 0)
            {
                host = overrideParams.getHost();
                port = overrideParams.getPort();
            }
            if (overrideParams.getDuration() > 0)
                duration = overrideParams.getDuration();
            if (overrideParams.getNumThreads() > 0)
                numThreads = overrideParams.getNumThreads();
            if (overrideParams.getNumPings() > 0)
                numPings = overrideParams.getNumPings();
            testTime = System.currentTimeMillis();
            if (overrideParams.getPreTest() != 1)
                pre_test = overrideParams.getPreTest();
        }
        System.out.println("2.- Host " + host + " port " + port + " uuid " + uuid + " duration " + duration + " threads " + numThreads + " preTest " + pre_test);
        return new testParameters(host, port, overrideParams.getToken(), duration, numThreads, numPings, testTime, pre_test);
    }
    
    marilClientParam(final testParameters params)
    {
        this.params = params;
                
        if (params.getNumThreads() > 0)
        {
            testThreadPool = Executors.newFixedThreadPool(params.getNumThreads());
            testTasks = new TCPbasedTest[params.getNumThreads()];
        }
        else
        {
            testThreadPool = null;
            testTasks = null;
        }
        
        durationDownNano = params.getDuration() * 1000000000L;
        durationUpNano = params.getDuration() * 1000000000L;
        
        lastTransfer = new long[params.getNumThreads()][KEEP_LAST_ENTRIES];
        lastTime = new long[params.getNumThreads()][KEEP_LAST_ENTRIES];

    }
    
    public void setTrafficService(TrafficService trafficService) {
    	this.trafficService = trafficService;
    }
    
    public TrafficService getTrafficService() {
    	return this.trafficService;
    }
        
    public TestResult runTest() throws InterruptedException
    {
    	System.out.println("starting test...");
    	
    	long txBytes = 0;
    	long rxBytes = 0;
    	final long timeStampStart = System.nanoTime();
    	
        if (testStatus.get() != TestStatus.ERROR && testThreadPool != null)
        {   
            if (trafficService != null) {
                txBytes = trafficService.getTotalTxBytes();
                rxBytes = trafficService.getTotalRxBytes();
            }
            
            resetSpeed();
            downBitPerSec.set(-1);
            upBitPerSec.set(-1);
            pingNano.set(-1);
            
            final long waitTime = params.getStartTime() - System.currentTimeMillis();
            if (waitTime > 0)
            {
                setStatus(TestStatus.WAIT);
                log(String.format(Locale.US, "we have to wait %d ms...", waitTime));
                Thread.sleep(waitTime);
                log(String.format(Locale.US, "...done.", waitTime));
            }
            else
                log(String.format(Locale.US, "luckily we do not have to wait.", waitTime));
            
            setStatus(TestStatus.INIT);
            statusBeforeError.set(null);
            
            if (testThreadPool.isShutdown())
                throw new IllegalStateException("Experiment already shut down");
            log("starting test...");
            
            final int numThreads = params.getNumThreads();
                        
            aborted.set(false);
            
            result = new TotalTestResult();
                                                
            log(String.format(Locale.US, "Host: %s; Port: %d", params.getHost(), params.getPort()));
            log(String.format(Locale.US, "starting %d threads...", numThreads));
            
            final CyclicBarrier barrier = new CyclicBarrier(numThreads);
            
            @SuppressWarnings("unchecked")
            final Future<ThreadTestResult>[] results = new Future[numThreads];
            
            final int storeResults = (int) (params.getDuration() * 1000000000L / MIN_DIFF_TIME);
            
            final AtomicBoolean fallbackToOneThread = new AtomicBoolean();
            fallbackToOneThread.set(false);
            
            for (int i = 0; i < numThreads; i++)
            {
                testTasks[i] = new TCPbasedTest(this, params, i, barrier, storeResults, MIN_DIFF_TIME, fallbackToOneThread);
                results[i] = testThreadPool.submit(testTasks[i]);
            }
            
            
            try
            {
                
                long shortestPing = Long.MAX_VALUE;
                
                // wait for all threads first
                for (int i = 0; i < numThreads; i++)
                    results[i].get();
                
                if (aborted.get())
                    return null;
                
                final long[][] allDownBytes = new long[numThreads][];
                final long[][] allDownNsecs = new long[numThreads][];
                final long[][] allUpBytes = new long[numThreads][];
                final long[][] allUpNsecs = new long[numThreads][];
                
                int realNumThreads = 0;
                log("");
                for (int i = 0; i < numThreads; i++)
                {
                    final ThreadTestResult testResult = results[i].get();
                    
                    if (testResult != null)
                    {
                        realNumThreads++;
                        
                        log(String.format(Locale.US, "Thread %d: Download: bytes: %d time: %.3f s", i,
                                ThreadTestResult.getLastEntry(testResult.down.bytes),
                                ThreadTestResult.getLastEntry(testResult.down.nsec) / 1e9));
                        log(String.format(Locale.US, "Thread %d: Upload:   bytes: %d time: %.3f s", i,
                                ThreadTestResult.getLastEntry(testResult.up.bytes),
                                ThreadTestResult.getLastEntry(testResult.up.nsec) / 1e9));
                        
                        final long ping = testResult.ping_shortest;
                        if (ping < shortestPing)
                            shortestPing = ping;
                        
                        if (!testResult.pings.isEmpty())
                            result.pings.addAll(testResult.pings);
                        
                        allDownBytes[i] = testResult.down.bytes;
                        allDownNsecs[i] = testResult.down.nsec;
                        allUpBytes[i] = testResult.up.bytes;
                        allUpNsecs[i] = testResult.up.nsec;
                        
                        result.totalDownBytes += testResult.totalDownBytes;
                        result.totalUpBytes += testResult.totalUpBytes;
                        
                        // aggregate speedItems
                        result.speedItems.addAll(testResult.speedItems);
                    }
                }
                
                result.calculateDownload(allDownBytes, allDownNsecs);
                result.calculateUpload(allUpBytes, allUpNsecs);
                
                log("");
                log(String.format(Locale.US, "Total calculated bytes down: %d", result.bytes_download));
                log(String.format(Locale.US, "Total calculated time down:  %.3f s", result.nsec_download / 1e9));
                log(String.format(Locale.US, "Total calculated bytes up:   %d", result.bytes_upload));
                log(String.format(Locale.US, "Total calculated time up:    %.3f s", result.nsec_upload / 1e9));
                
                // get Connection Info from thread 1 (one thread must run)
                result.ip_local = results[0].get().ip_local;
                result.ip_server = results[0].get().ip_server;
                result.port_remote = results[0].get().port_remote;
                
                result.num_threads = realNumThreads;
                
                result.ping_shortest = shortestPing;
                
                result.speed_download = result.getDownloadSpeedBitPerSec() / 1e3;
                result.speed_upload = result.getUploadSpeedBitPerSec() / 1e3;
                
                log("");
                log(String.format(Locale.US, "Total Down: %.0f kBit/s", result.getDownloadSpeedBitPerSec() / 1e3));
                log(String.format(Locale.US, "Total UP:   %.0f kBit/s", result.getUploadSpeedBitPerSec() / 1e3));
                log(String.format(Locale.US, "Ping:       %.2f ms", shortestPing / 1e6));
               
                
                downBitPerSec.set(Math.round(result.getDownloadSpeedBitPerSec()));
                upBitPerSec.set(Math.round(result.getUploadSpeedBitPerSec()));
                
                log("end.");
                setStatus(TestStatus.SPEEDTEST_END);
                
                if (trafficService != null) {
                    result.setMeasurementMap(measurementMap);
            	}
                               
                return result;
            }
            catch (final ExecutionException e)
            {
                log(e);
                abortTest(true);
                return null;
            }
            catch (final InterruptedException e)
            {
                log("Test interrupted");
                abortTest(false);
                throw e;
            }
        }
        else {
            setStatus(TestStatus.SPEEDTEST_END);
            
        	return null;
        }
    }
    
    public boolean abortTest(final boolean error)
    {
        System.out.println("Test aborted");
        
        if (error)
            setErrorStatus();
        else
            setStatus(TestStatus.ABORTED);
        aborted.set(true);
        
        if (testThreadPool != null)
            testThreadPool.shutdownNow();
        
        return true;
    }
    
    public void shutdown()
    {
        System.out.println("Shutting down the experiment thread pool...");
        if (testThreadPool != null)
            testThreadPool.shutdownNow();
        
        System.out.println("Shutdown finished.");
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        if (testThreadPool != null)
            testThreadPool.shutdownNow();
    }
        
    private void resetSpeed()
    {
        lastCounter = 0;
    }
    
    private float getTotalSpeed()
    {
        long sumTrans = 0;
        long maxTime = 0;
        
        final CurrentSpeed currentSpeed = new CurrentSpeed();
        
        for (int i = 0; i < params.getNumThreads(); i++)
            if (testTasks[i] != null)
            {
                testTasks[i].getCurrentSpeed(currentSpeed);
                
                if (currentSpeed.time > maxTime)
                    maxTime = currentSpeed.time;
                sumTrans += currentSpeed.trans;
            }
        
        return maxTime == 0f ? 0f : (float) sumTrans / (float) maxTime * 1e9f * 8.0f;
    }
    
    final Map<Integer, List<SpeedItem>> speedMap = new HashMap<Integer, List<SpeedItem>>();
    
    private float getAvgSpeed()
    {
        long sumDiffTrans = 0;
        long maxDiffTime = 0;
        
        final CurrentSpeed currentSpeed = new CurrentSpeed();
        
        final int currentIndex = lastCounter % KEEP_LAST_ENTRIES;
        int diffReferenceIndex = (lastCounter - KEEP_LAST_ENTRIES + 1) % KEEP_LAST_ENTRIES;
        if (diffReferenceIndex < 0)
            diffReferenceIndex = 0;
        
        lastCounter++;
        
        for (int i = 0; i < params.getNumThreads(); i++)
            if (testTasks[i] != null)
            {
                testTasks[i].getCurrentSpeed(currentSpeed);
                
                lastTime[i][currentIndex] = currentSpeed.time;
                lastTransfer[i][currentIndex] = currentSpeed.trans;
                
//                System.out.println("T" + i + ": " + currentSpeed);
                
                List<SpeedItem> speedList = speedMap.get(i);
                if (speedList == null) {
                	speedList = new ArrayList<SpeedItem>();
                	speedMap.put(i, speedList);
                }
                
                speedList.add(new SpeedItem(false, i, currentSpeed.time, currentSpeed.trans));
                
                final long diffTime = currentSpeed.time - lastTime[i][diffReferenceIndex];
                final long diffTrans = currentSpeed.trans - lastTransfer[i][diffReferenceIndex];
                
                if (diffTime > maxDiffTime)
                    maxDiffTime = diffTime;
                sumDiffTrans += diffTrans;
            }     
        
        final float speedAvg = maxDiffTime == 0f ? 0f : (float) sumDiffTrans / (float) maxDiffTime * 1e9f * 8.0f;
                
        return speedAvg;
    }
    
    public IntermediateResult getIntermediateResult(IntermediateResult iResult)
    {
        if (iResult == null)
            iResult = new IntermediateResult();
        iResult.status = testStatus.get();
        iResult.remainingWait = 0;
        final long diffTime = System.nanoTime() - statusChangeTime.get();
        switch (iResult.status)
        {
        case WAIT:
            iResult.progress = 0;
            iResult.remainingWait = params.getStartTime() - System.currentTimeMillis();
            break;
        
        case INIT:
            iResult.progress = (float) diffTime / durationInitNano;
            break;
        
        case PING:
            iResult.progress = getPingProgress();
            break;
        
        case DOWN:
            iResult.progress = (float) diffTime / durationDownNano;
            downBitPerSec.set(Math.round(getAvgSpeed()));
            break;
        
        case INIT_UP:
            iResult.progress = 0;
            break;
        
        case UP:
            iResult.progress = (float) diffTime / durationUpNano;
            upBitPerSec.set(Math.round(getAvgSpeed()));
            break;
        
        case SPEEDTEST_END:
            iResult.progress = 1;
            break;
        
        case ERROR:
        case ABORTED:
            iResult.progress = 0;
            break;
        }
        
        if (iResult.progress > 1)
            iResult.progress = 1;
        
        iResult.pingNano = pingNano.get();
        iResult.downBitPerSec = downBitPerSec.get();
        iResult.upBitPerSec = upBitPerSec.get();
        
        iResult.setLogValues();
        
        return iResult;
    }
    
    public TestStatus getStatus()
    {
        return testStatus.get();
    }
    
    public TestStatus getStatusBeforeError()
    {
        return statusBeforeError.get();
    }
    
    public void setStatus(final TestStatus status)
    {
        testStatus.set(status);
        statusChangeTime.set(System.nanoTime());
        if (status == TestStatus.INIT_UP)
        {
            // DOWN is finished
            downBitPerSec.set(Math.round(getTotalSpeed()));
            resetSpeed();
        }
    }
        
    public String getErrorMsg()
    {
        return errorMsg;
    }
    
    public void sendResult()
    {
        setErrorStatus();
        log("Still pending the result delivery...");
        Iterator it = result.speedItems.iterator();
        Iterator itPing =  result.pings.iterator();
        try{
            PrintWriter writer = new PrintWriter("outputData.txt", "UTF-8");
            while(itPing.hasNext()) {
                Object elementPing = itPing.next();
                writer.println(elementPing.toString());
            }
            while(it.hasNext()) {
                Object element = it.next();
                writer.println(element);
                //System.out.print(element + "\n");
            }
            writer.close();
        }catch(Exception e)
        { 
            log("Problem creating the log file");
        }
        
        log(String.format(Locale.US, "END LOG"));
    }
        
    private void setErrorStatus()
    {
        final TestStatus lastStatus = testStatus.getAndSet(TestStatus.ERROR);
        if (lastStatus != TestStatus.ERROR)
            statusBeforeError.set(lastStatus);
    }
    
    void log(final CharSequence text)
    {
        if (outputToStdout)
            System.out.println(text);
    }
    
    void log(final Exception e)
    {
        if (outputToStdout)
            e.printStackTrace(System.out);
    }
    
    void setPing(final long shortestPing)
    {
        pingNano.set(shortestPing);
    }
    
    void updatePingStatus(final long tsStart, int pingsDone, long tsLastPing)
    {
        pingTsStart.set(tsStart);
        pingNumDome.set(pingsDone);
        pingTsLastPing.set(tsLastPing);
    }
    
    private float getPingProgress()
    {
        final long start = pingTsStart.get();
        
        if (start == -1) // not yet started
            return 0;
        
        final int numDone = pingNumDome.get();
        final long lastPing = pingTsLastPing.get();
        final long now = System.nanoTime();
        
        final int numPings = params.getNumPings();
        
        if (numPings <= 0) // nothing to do
            return 1;
        
        final float factorPerPing = (float)1 / (float)numPings;
        final float base = factorPerPing * numDone;
        
        final long approxTimePerPing;
        if (numDone == 0 || lastPing == -1) // during first ping, assume 100ms
            approxTimePerPing = 100000000;
        else
            approxTimePerPing = (lastPing - start) / numDone;
        
        float factorLastPing = (float)(now - lastPing) / (float)approxTimePerPing;
        if (factorLastPing < 0)
            factorLastPing = 0;
        if (factorLastPing > 1)
            factorLastPing = 1;
        
        final float result = base + factorLastPing * factorPerPing;
        if (result < 0)
            return 0;
        if (result > 1)
            return 1;
        
        return result;
    }
    
    public void startTrafficService(final int threadId, final TestStatus status) {
    	if (trafficService != null) {
    		//a concurrent map is needed in case multiple threads want to start the traffic service
    		//only the first thread should be able to start the service
    		TestMeasurement tm = new TestMeasurement(status.toString(), trafficService);
        	TestMeasurement previousTm = measurementMap.putIfAbsent(status, tm);
        	if (previousTm == null) {
        		tm.start(threadId);
        	}
    	}
    }
    
    public void stopTrafficMeasurement(final int threadId, final TestStatus status) {
    	final TestMeasurement testMeasurement = measurementMap.get(status);
    	if (testMeasurement != null)
    	    testMeasurement.stop(threadId);
    }
    
    public Map<TestStatus, TestMeasurement> getTrafficMeasurementMap() {
    	return measurementMap;
    }
       
    
}
