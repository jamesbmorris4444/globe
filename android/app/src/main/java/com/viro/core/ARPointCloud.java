/*
 * Copyright (c) 2017-present, ViroMedia, Inc.
 * All rights reserved.
 */

package com.viro.core;

/**
 * ARPointCloud contains a collection of points that the AR subsystem has detected in the
 * user's real world.
 *
 */
public class ARPointCloud {
    private float[] mPoints;

    ARPointCloud(float[] points) {
        // The reason why ARPointCloud simply contains a float[] is because each frame can contain
        // hundreds of points which, if converted to Vectors would result in hundreds of Java objects
        // created each frame (I tried this and would overflow local memory space in JNI).
        mPoints = points;
    }

    /**
     * Returns a float array containing the (x,y,z) position of each point in the point cloud
     * plus a confidence value (from 0 to 1). Each point therefore takes up 4 floats in the array
     * and are arranged in no particular order.
     *
     * @return A single float array containing the position and confidence of each point.
     */
    public float[] getPoints() {
        return mPoints;
    }

    /**
     * Returns the number of points in the ARPointCloud.
     *
     * @return the number of points in the ARPointCloud.
     */
    public int size() {
        return mPoints.length / 4;
    }

    /**
     * Helper method that when given an index i, returns a 4 element float array representing a cloud point.
     *
     * @param i Index of the point to return. Can't be greater than total points returned by {@link #size()}.
     * @return 4 element float array consisting of x,y,z cloud point position and confidence level.
     */
    public float[] getPoint(int i) {

        if(i > size()) {
            throw new IndexOutOfBoundsException();
        }

        float []point = new float[4];
        point[0] = mPoints[i*4+0];
        point[1] = mPoints[i*4+1];
        point[2] = mPoints[i*4+2];
        point[3] = mPoints[i*4+3];
        return point;
    }
}

