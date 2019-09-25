//
//  Copyright (c) 2017-present, ViroMedia, Inc.
//  All rights reserved.
//
//  Permission is hereby granted, free of charge, to any person obtaining
//  a copy of this software and associated documentation files (the
//  "Software"), to deal in the Software without restriction, including
//  without limitation the rights to use, copy, modify, merge, publish,
//  distribute, sublicense, and/or sell copies of the Software, and to
//  permit persons to whom the Software is furnished to do so, subject to
//  the following conditions:
//
//  The above copyright notice and this permission notice shall be included
//  in all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.viro.core.internal.keys;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.viro.renderer.BuildConfig;

import java.lang.ref.WeakReference;

/**
 * Utility class to validate api keys passed in SceneNavigator
 */

public class KeyValidator {

    private static final String TAG = "Viro";
    private WeakReference<Context> mContextWeakRef;
    private DynamoDBMapper mDynamoDBMapper;
    private AmazonDynamoDBClient mDynamoClient;

    private static int MAX_WAIT_INTERVAL_MILLIS = 64000;

    // If we compute exponential falloff with a number greater than 53, we end up overflowing
    // We can set to a lower number since we max out the wait after only a few tries anyway
    private static int MAX_FALLOFF_EXP = 10;

    public KeyValidator(Context context) {
        mContextWeakRef = new WeakReference<Context>(context);
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                BuildConfig.COGNITO_IDENTITY_POOL_ID,
                Regions.US_WEST_2
        );

        mDynamoClient = new AmazonDynamoDBClient(credentialsProvider);
        mDynamoClient.setRegion(Region.getRegion(Regions.US_WEST_2));
        mDynamoDBMapper = new DynamoDBMapper(mDynamoClient);

    }

    /**
     * Validates the apiKey from Dynamo tables. Calls {@link KeyValidationListener#onResponse(boolean)}
     * based on response from Dynamo.
     * @param apiKey
     * @param listener
     */
    public void validateKey(final String apiKey, final String viewType, final String platform, final KeyValidationListener listener) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int retries = 0;
                boolean retry;
                do {
                    retry = false;
                    try {
                        ApiKey keyFromDynamo = mDynamoDBMapper.load(ApiKey.class, apiKey);
                        // Our dynamodb table stores valid as String true or false. As a result, objectmapper loads it as String
                        if (keyFromDynamo != null && keyFromDynamo.getValid().equalsIgnoreCase("true")) {
                            listener.onResponse(true);
                            Context context = mContextWeakRef.get();
                            if(context != null) {
                                // Create KeyMetricsRecorder with Application Context so we don't hold onto activity as Keen Metrics client is a singleton.
                                KeyMetricsRecorder recorder = new KeyMetricsRecorder(mDynamoClient, context.getApplicationContext());
                                recorder.record(apiKey, viewType, platform);
                            }
                        } else {
                            listener.onResponse(false);
                            Log.i(TAG, "The given API Key is either missing or invalid! If you " +
                                    "have not signed up for accessing Viro Media platform, please" +
                                    " do so at www.viromedia.com. Otherwise, contact " +
                                    "info@viromedia.com if you have a valid key and are " +
                                    "encountering this error.");
                        }
                    } catch (Exception e) {
                        retry = true;
                    }
                    if (retry == true) {
                        retries++;
                        long waitTime = getWaitTimeExp(retries);
                        Log.i(TAG, "Attempt #" + retries + " to fetch api keys failed." +
                                " Retrying in " + waitTime + " milliseconds");
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } while (retry);
            }
        };
        Thread dynamoThread = new Thread(runnable);
        dynamoThread.start();
    }

    /**
     * Returns the next wait interval, in milliseconds, using an exponential
     * backoff algorithm.
     */
    private long getWaitTimeExp(int retryCount) {
        int falloffExp = Math.min(retryCount, MAX_FALLOFF_EXP);
        long waitTime = ((long) Math.pow(2, falloffExp) * 1000L);
        return Math.min(waitTime, MAX_WAIT_INTERVAL_MILLIS);
    }
}