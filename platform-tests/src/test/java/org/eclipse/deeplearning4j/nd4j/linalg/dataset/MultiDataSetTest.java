/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.eclipse.deeplearning4j.nd4j.linalg.dataset;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.BaseNd4jTestWithBackends;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.random.impl.BernoulliDistribution;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.interval;

@Slf4j
@Tag(TagNames.NDARRAY_ETL)
@NativeTag
@Tag(TagNames.FILE_IO)
public class MultiDataSetTest extends BaseNd4jTestWithBackends {

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMerging2d(Nd4jBackend backend) {
        //Simple test: single input/output arrays; 5 MultiDataSets to merge
        int nCols = 3;
        int nRows = 5;
        INDArray expIn = Nd4j.linspace(0, nCols * nRows - 1, nCols * nRows, DataType.DOUBLE).reshape(nRows, nCols);
        INDArray expOut = Nd4j.linspace(100, 100 + nCols * nRows - 1, nCols * nRows, DataType.DOUBLE).reshape(nRows, nCols);

        INDArray[] in = new INDArray[nRows];
        INDArray[] out = new INDArray[nRows];
        for (int i = 0; i < nRows; i++) {
            INDArray rowView = expIn.getRow(i, true);
            in[i] = rowView.dup();
        }
        for (int i = 0; i < nRows; i++) {
            INDArray rowView = expOut.getRow(i, true);
            out[i] = rowView.dup();
        }
        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            list.add(new MultiDataSet(in[i], out[i]));
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        assertEquals(1, merged.getFeatures().length);
        assertEquals(1, merged.getLabels().length);

        assertEquals(expIn, merged.getFeatures(0));
        assertEquals(expOut, merged.getLabels(0));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMerging2dMultipleInOut(Nd4jBackend backend) {
        //Test merging: Multiple input/output arrays; 5 MultiDataSets to merge

        int nRows = 5;
        int nColsIn0 = 3;
        int nColsIn1 = 4;
        int nColsOut0 = 5;
        int nColsOut1 = 6;

        INDArray expIn0 = Nd4j.linspace(0, nRows * nColsIn0 - 1, nRows * nColsIn0, DataType.DOUBLE).reshape(nRows, nColsIn0);
        INDArray expIn1 = Nd4j.linspace(0, nRows * nColsIn1 - 1, nRows * nColsIn1, DataType.DOUBLE).reshape(nRows, nColsIn1);
        INDArray expOut0 = Nd4j.linspace(0, nRows * nColsOut0 - 1, nRows * nColsOut0, DataType.DOUBLE).reshape(nRows, nColsOut0);
        INDArray expOut1 = Nd4j.linspace(0, nRows * nColsOut1 - 1, nRows * nColsOut1, DataType.DOUBLE).reshape(nRows, nColsOut1);

        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            if (i == 0) {
                //For first MultiDataSet: have 2 rows, not just 1
                INDArray in0 = expIn0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray in1 = expIn1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray out0 = expOut0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray out1 = expOut1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1}));
                i++;
            } else {
                INDArray in0 = expIn0.getRow(i, true).dup();
                INDArray in1 = expIn1.getRow(i, true).dup();
                INDArray out0 = expOut0.getRow(i, true).dup();
                INDArray out1 = expOut1.getRow(i, true).dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1}));
            }
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        assertEquals(2, merged.getFeatures().length);
        assertEquals(2, merged.getLabels().length);

        assertEquals(expIn0, merged.getFeatures(0));
        assertEquals(expIn1, merged.getFeatures(1));
        assertEquals(expOut0, merged.getLabels(0));
        assertEquals(expOut1, merged.getLabels(1));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMerging2dMultipleInOut2(Nd4jBackend backend) {
        //Test merging: Multiple input/output arrays; 5 MultiDataSets to merge

        int nRows = 10;
        int nColsIn0 = 3;
        int nColsIn1 = 4;
        int nColsIn2 = 5;
        int nColsOut0 = 6;
        int nColsOut1 = 7;
        int nColsOut2 = 8;

        INDArray expIn0 = Nd4j.linspace(0, nRows * nColsIn0 - 1, nRows * nColsIn0, DataType.DOUBLE).reshape(nRows, nColsIn0);
        INDArray expIn1 = Nd4j.linspace(0, nRows * nColsIn1 - 1, nRows * nColsIn1, DataType.DOUBLE).reshape(nRows, nColsIn1);
        INDArray expIn2 = Nd4j.linspace(0, nRows * nColsIn2 - 1, nRows * nColsIn2, DataType.DOUBLE).reshape(nRows, nColsIn2);
        INDArray expOut0 = Nd4j.linspace(0, nRows * nColsOut0 - 1, nRows * nColsOut0, DataType.DOUBLE).reshape(nRows, nColsOut0);
        INDArray expOut1 = Nd4j.linspace(0, nRows * nColsOut1 - 1, nRows * nColsOut1, DataType.DOUBLE).reshape(nRows, nColsOut1);
        INDArray expOut2 = Nd4j.linspace(0, nRows * nColsOut2 - 1, nRows * nColsOut2, DataType.DOUBLE).reshape(nRows, nColsOut2);

        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            if (i == 0) {
                //For first MultiDataSet: have 2 rows, not just 1
                INDArray in0 = expIn0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray in1 = expIn1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray in2 = expIn2.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray out0 = expOut0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray out1 = expOut1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray out2 = expOut2.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1, in2}, new INDArray[] {out0, out1, out2}));
                i++;
            } else {
                INDArray in0 = expIn0.getRow(i, true).dup();
                INDArray in1 = expIn1.getRow(i, true).dup();
                INDArray in2 = expIn2.getRow(i, true).dup();
                INDArray out0 = expOut0.getRow(i, true).dup();
                INDArray out1 = expOut1.getRow(i, true).dup();
                INDArray out2 = expOut2.getRow(i, true).dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1, in2}, new INDArray[] {out0, out1, out2}));
            }
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        assertEquals(3, merged.getFeatures().length);
        assertEquals(3, merged.getLabels().length);

        assertEquals(expIn0, merged.getFeatures(0));
        assertEquals(expIn1, merged.getFeatures(1));
        assertEquals(expIn2, merged.getFeatures(2));
        assertEquals(expOut0, merged.getLabels(0));
        assertEquals(expOut1, merged.getLabels(1));
        assertEquals(expOut2, merged.getLabels(2));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMerging2dMultipleInOut3(Nd4jBackend backend) {
        //Test merging: fewer rows than output arrays...

        int nRows = 2;
        int nColsIn0 = 3;
        int nColsIn1 = 4;
        int nColsIn2 = 5;
        int nColsOut0 = 6;
        int nColsOut1 = 7;
        int nColsOut2 = 8;

        INDArray expIn0 = Nd4j.linspace(0, nRows * nColsIn0 - 1, nRows * nColsIn0, DataType.DOUBLE).reshape(nRows, nColsIn0);
        INDArray expIn1 = Nd4j.linspace(0, nRows * nColsIn1 - 1, nRows * nColsIn1, DataType.DOUBLE).reshape(nRows, nColsIn1);
        INDArray expIn2 = Nd4j.linspace(0, nRows * nColsIn2 - 1, nRows * nColsIn2, DataType.DOUBLE).reshape(nRows, nColsIn2);
        INDArray expOut0 = Nd4j.linspace(0, nRows * nColsOut0 - 1, nRows * nColsOut0, DataType.DOUBLE).reshape(nRows, nColsOut0);
        INDArray expOut1 = Nd4j.linspace(0, nRows * nColsOut1 - 1, nRows * nColsOut1, DataType.DOUBLE).reshape(nRows, nColsOut1);
        INDArray expOut2 = Nd4j.linspace(0, nRows * nColsOut2 - 1, nRows * nColsOut2, DataType.DOUBLE).reshape(nRows, nColsOut2);

        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            INDArray in0 = expIn0.getRow(i, true).dup();
            INDArray in1 = expIn1.getRow(i, true).dup();
            INDArray in2 = expIn2.getRow(i, true).dup();
            INDArray out0 = expOut0.getRow(i, true).dup();
            INDArray out1 = expOut1.getRow(i, true).dup();
            INDArray out2 = expOut2.getRow(i, true).dup();
            list.add(new MultiDataSet(new INDArray[] {in0, in1, in2}, new INDArray[] {out0, out1, out2}));
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        assertEquals(3, merged.getFeatures().length);
        assertEquals(3, merged.getLabels().length);

        assertEquals(expIn0, merged.getFeatures(0));
        assertEquals(expIn1, merged.getFeatures(1));
        assertEquals(expIn2, merged.getFeatures(2));
        assertEquals(expOut0, merged.getLabels(0));
        assertEquals(expOut1, merged.getLabels(1));
        assertEquals(expOut2, merged.getLabels(2));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMerging4dMultipleInOut(Nd4jBackend backend) {
        int nRows = 5;
        int depthIn0 = 3;
        int widthIn0 = 4;
        int heightIn0 = 5;

        int depthIn1 = 4;
        int widthIn1 = 5;
        int heightIn1 = 6;

        int nColsOut0 = 5;
        int nColsOut1 = 6;

        int lengthIn0 = nRows * depthIn0 * widthIn0 * heightIn0;
        int lengthIn1 = nRows * depthIn1 * widthIn1 * heightIn1;
        INDArray expIn0 = Nd4j.linspace(0, lengthIn0 - 1, lengthIn0, DataType.DOUBLE).reshape(nRows, depthIn0, widthIn0, heightIn0);
        INDArray expIn1 = Nd4j.linspace(0, lengthIn1 - 1, lengthIn1, DataType.DOUBLE).reshape(nRows, depthIn1, widthIn1, heightIn1);
        INDArray expOut0 = Nd4j.linspace(0, nRows * nColsOut0 - 1, nRows * nColsOut0, DataType.DOUBLE).reshape(nRows, nColsOut0);
        INDArray expOut1 = Nd4j.linspace(0, nRows * nColsOut1 - 1, nRows * nColsOut1, DataType.DOUBLE).reshape(nRows, nColsOut1);

        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            if (i == 0) {
                //For first MultiDataSet: have 2 rows, not just 1
                INDArray in0 = expIn0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all(), NDArrayIndex.all(),
                        NDArrayIndex.all()).dup();
                INDArray in1 = expIn1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all(), NDArrayIndex.all(),
                        NDArrayIndex.all()).dup();
                INDArray out0 = expOut0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                INDArray out1 = expOut1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all()).dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1}));
                i++;
            } else {
                INDArray in0 = expIn0.get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all(),
                        NDArrayIndex.all()).dup();
                INDArray in1 = expIn1.get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all(),
                        NDArrayIndex.all()).dup();
                INDArray out0 = expOut0.getRow(i, true).dup();
                INDArray out1 = expOut1.getRow(i, true).dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1}));
            }
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        assertEquals(2, merged.getFeatures().length);
        assertEquals(2, merged.getLabels().length);

        assertEquals(expIn0, merged.getFeatures(0));
        assertEquals(expIn1, merged.getFeatures(1));
        assertEquals(expOut0, merged.getLabels(0));
        assertEquals(expOut1, merged.getLabels(1));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMergingTimeSeriesEqualLength(Nd4jBackend backend) {
        int tsLength = 8;
        int nRows = 5;
        int nColsIn0 = 3;
        int nColsIn1 = 4;
        int nColsOut0 = 5;
        int nColsOut1 = 6;

        int n0 = nRows * nColsIn0 * tsLength;
        int n1 = nRows * nColsIn1 * tsLength;
        int nOut0 = nRows * nColsOut0 * tsLength;
        int nOut1 = nRows * nColsOut1 * tsLength;
        INDArray expIn0 = Nd4j.linspace(0, n0 - 1, n0, DataType.DOUBLE).reshape(nRows, nColsIn0, tsLength);
        INDArray expIn1 = Nd4j.linspace(0, n1 - 1, n1, DataType.DOUBLE).reshape(nRows, nColsIn1, tsLength);
        INDArray expOut0 = Nd4j.linspace(0, nOut0 - 1, nOut0, DataType.DOUBLE).reshape(nRows, nColsOut0, tsLength);
        INDArray expOut1 = Nd4j.linspace(0, nOut1 - 1, nOut1, DataType.DOUBLE).reshape(nRows, nColsOut1, tsLength);

        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            if (i == 0) {
                //For first MultiDataSet: have 2 rows, not just 1
                INDArray in0 = expIn0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                INDArray in1 = expIn1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                INDArray out0 = expOut0.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                INDArray out1 = expOut1.get(NDArrayIndex.interval(0, 1, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1}));
                i++;
            } else {
                INDArray in0 = expIn0.get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                INDArray in1 = expIn1.get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                INDArray out0 = expOut0.get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                INDArray out1 = expOut1.get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all())
                        .dup();
                list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1}));
            }
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        assertEquals(2, merged.getFeatures().length);
        assertEquals(2, merged.getLabels().length);

        assertEquals(expIn0, merged.getFeatures(0));
        assertEquals(expIn1, merged.getFeatures(1));
        assertEquals(expOut0, merged.getLabels(0));
        assertEquals(expOut1, merged.getLabels(1));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMergingTimeSeriesWithMasking(Nd4jBackend backend) {
        //Mask arrays, and different lengths

        int tsLengthIn0 = 8;
        int tsLengthIn1 = 9;
        int tsLengthOut0 = 10;
        int tsLengthOut1 = 11;

        int nRows = 5;
        int nColsIn0 = 3;
        int nColsIn1 = 4;
        int nColsOut0 = 5;
        int nColsOut1 = 6;

        INDArray expectedIn0 = Nd4j.create(DataType.DOUBLE, nRows, nColsIn0, tsLengthIn0);
        INDArray expectedIn1 = Nd4j.create(DataType.DOUBLE, nRows, nColsIn1, tsLengthIn1);
        INDArray expectedOut0 = Nd4j.create(DataType.DOUBLE, nRows, nColsOut0, tsLengthOut0);
        INDArray expectedOut1 = Nd4j.create(DataType.DOUBLE, nRows, nColsOut1, tsLengthOut1);

        INDArray expectedMaskIn0 = Nd4j.create(DataType.DOUBLE, nRows, tsLengthIn0);
        INDArray expectedMaskIn1 = Nd4j.create(DataType.DOUBLE, nRows, tsLengthIn1);
        INDArray expectedMaskOut0 = Nd4j.create(DataType.DOUBLE, nRows, tsLengthOut0);
        INDArray expectedMaskOut1 = Nd4j.create(DataType.DOUBLE, nRows, tsLengthOut1);


        Random r = new Random(12345);
        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            int thisRowIn0Length = tsLengthIn0 - i;
            int thisRowIn1Length = tsLengthIn1 - i;
            int thisRowOut0Length = tsLengthOut0 - i;
            int thisRowOut1Length = tsLengthOut1 - i;

            int in0NumElem = thisRowIn0Length * nColsIn0;
            INDArray in0 = Nd4j.linspace(0, in0NumElem - 1, in0NumElem, DataType.DOUBLE).reshape(1, nColsIn0, thisRowIn0Length);

            int in1NumElem = thisRowIn1Length * nColsIn1;
            INDArray in1 = Nd4j.linspace(0, in1NumElem - 1, in1NumElem, DataType.DOUBLE).reshape(1, nColsIn1, thisRowIn1Length);

            int out0NumElem = thisRowOut0Length * nColsOut0;
            INDArray out0 = Nd4j.linspace(0, out0NumElem - 1, out0NumElem, DataType.DOUBLE).reshape(1, nColsOut0, thisRowOut0Length);

            int out1NumElem = thisRowOut1Length * nColsOut1;
            INDArray out1 = Nd4j.linspace(0, out1NumElem - 1, out1NumElem, DataType.DOUBLE).reshape(1, nColsOut1, thisRowOut1Length);

            INDArray maskIn0 = null;
            INDArray maskIn1 = Nd4j.zeros(1, thisRowIn1Length);
            for (int j = 0; j < thisRowIn1Length; j++) {
                if (r.nextBoolean())
                    maskIn1.putScalar(j, 1.0);
            }
            INDArray maskOut0 = null;
            INDArray maskOut1 = Nd4j.zeros(1, thisRowOut1Length);
            for (int j = 0; j < thisRowOut1Length; j++) {
                if (r.nextBoolean())
                    maskOut1.putScalar(j, 1.0);
            }

            expectedIn0.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowIn0Length)}, in0);
            expectedIn1.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowIn1Length)}, in1);
            expectedOut0.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowOut0Length)}, out0);
            expectedOut1.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowOut1Length)}, out1);

            expectedMaskIn0.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowIn0Length)},
                    Nd4j.ones(1, thisRowIn0Length));
            expectedMaskIn1.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowIn1Length)},
                    maskIn1);
            expectedMaskOut0.put(
                    new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowOut0Length)},
                    Nd4j.ones(1, thisRowOut0Length));
            expectedMaskOut1.put(
                    new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowOut1Length)},
                    maskOut1);

            list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1},
                    new INDArray[] {maskIn0, maskIn1}, new INDArray[] {maskOut0, maskOut1}));
        }

        MultiDataSet merged = MultiDataSet.merge(list);

        assertEquals(2, merged.getFeatures().length);
        assertEquals(2, merged.getLabels().length);
        assertEquals(2, merged.getFeaturesMaskArrays().length);
        assertEquals(2, merged.getLabelsMaskArrays().length);

        assertEquals(expectedIn0, merged.getFeatures(0));
        assertEquals(expectedIn1, merged.getFeatures(1));
        assertEquals(expectedOut0, merged.getLabels(0));
        assertEquals(expectedOut1, merged.getLabels(1));

        assertEquals(expectedMaskIn0, merged.getFeaturesMaskArray(0));
        assertEquals(expectedMaskIn1, merged.getFeaturesMaskArray(1));
        assertEquals(expectedMaskOut0, merged.getLabelsMaskArray(0));
        assertEquals(expectedMaskOut1, merged.getLabelsMaskArray(1));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMergingWithPerOutputMasking(Nd4jBackend backend) {

        //Test 2d mask merging, 2d data
        //features
        INDArray f2d1 = Nd4j.create(new double[] {1, 2, 3}).reshape(1, -1);
        INDArray f2d2 = Nd4j.create(new double[][] {{4, 5, 6}, {7, 8, 9}});
        //labels
        INDArray l2d1 = Nd4j.create(new double[] {1.5, 2.5, 3.5}).reshape(1, -1);
        INDArray l2d2 = Nd4j.create(new double[][] {{4.5, 5.5, 6.5}, {7.5, 8.5, 9.5}});
        //feature masks
        INDArray fm2d1 = Nd4j.create(new double[] {0, 1, 1}).reshape(1, -1);
        INDArray fm2d2 = Nd4j.create(new double[][] {{1, 0, 1}, {0, 1, 0}});
        //label masks
        INDArray lm2d1 = Nd4j.create(new double[] {1, 1, 0}).reshape(1, -1);
        INDArray lm2d2 = Nd4j.create(new double[][] {{1, 0, 0}, {0, 1, 1}});

        MultiDataSet mds2d1 = new MultiDataSet(f2d1, l2d1, fm2d1, lm2d1);
        MultiDataSet mds2d2 = new MultiDataSet(f2d2, l2d2, fm2d2, lm2d2);
        MultiDataSet merged = MultiDataSet.merge(Arrays.asList(mds2d1, mds2d2));

        INDArray expFeatures2d = Nd4j.create(new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        INDArray expLabels2d = Nd4j.create(new double[][] {{1.5, 2.5, 3.5}, {4.5, 5.5, 6.5}, {7.5, 8.5, 9.5}});
        INDArray expFM2d = Nd4j.create(new double[][] {{0, 1, 1}, {1, 0, 1}, {0, 1, 0}});
        INDArray expLM2d = Nd4j.create(new double[][] {{1, 1, 0}, {1, 0, 0}, {0, 1, 1}});

        MultiDataSet mdsExp2d = new MultiDataSet(expFeatures2d, expLabels2d, expFM2d, expLM2d);
        assertEquals(mdsExp2d, merged);

        //Test 4d features, 2d labels, 2d masks
        INDArray f4d1 = Nd4j.create(1, 3, 5, 5);
        INDArray f4d2 = Nd4j.create(2, 3, 5, 5);
        MultiDataSet mds4d1 = new MultiDataSet(f4d1, l2d1, null, lm2d1);
        MultiDataSet mds4d2 = new MultiDataSet(f4d2, l2d2, null, lm2d2);
        MultiDataSet merged4d = MultiDataSet.merge(Arrays.asList(mds4d1, mds4d2));
        assertEquals(expLabels2d, merged4d.getLabels(0));
        assertEquals(expLM2d, merged4d.getLabelsMaskArray(0));

        //Test 3d mask merging, 3d data
        INDArray f3d1 = Nd4j.create(1, 3, 4);
        INDArray f3d2 = Nd4j.create(1, 3, 3);
        INDArray l3d1 = Nd4j.getExecutioner().exec(new BernoulliDistribution(Nd4j.create(1, 3, 4), 0.5));
        INDArray l3d2 = Nd4j.getExecutioner().exec(new BernoulliDistribution(Nd4j.create(2, 3, 3), 0.5));
        INDArray lm3d1 = Nd4j.getExecutioner().exec(new BernoulliDistribution(Nd4j.create(1, 3, 4), 0.5));
        INDArray lm3d2 = Nd4j.getExecutioner().exec(new BernoulliDistribution(Nd4j.create(2, 3, 3), 0.5));
        MultiDataSet mds3d1 = new MultiDataSet(f3d1, l3d1, null, lm3d1);
        MultiDataSet mds3d2 = new MultiDataSet(f3d2, l3d2, null, lm3d2);

        INDArray expLabels3d = Nd4j.create(3, 3, 4);
        expLabels3d.put(new INDArrayIndex[] {NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.interval(0, 4)},
                l3d1);
        expLabels3d.put(new INDArrayIndex[] {NDArrayIndex.interval(1, 2, true), NDArrayIndex.all(),
                NDArrayIndex.interval(0, 3)}, l3d2);
        INDArray expLM3d = Nd4j.create(3, 3, 4);
        expLM3d.put(new INDArrayIndex[] {NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.interval(0, 4)},
                lm3d1);
        expLM3d.put(new INDArrayIndex[] {NDArrayIndex.interval(1, 2, true), NDArrayIndex.all(),
                NDArrayIndex.interval(0, 3)}, lm3d2);


        MultiDataSet merged3d = MultiDataSet.merge(Arrays.asList(mds3d1, mds3d2));
        assertEquals(expLabels3d, merged3d.getLabels(0));
        assertEquals(expLM3d, merged3d.getLabelsMaskArray(0));

        //Test 3d features, 2d masks, 2d output (for example: RNN -> global pooling w/ per-output masking)
        MultiDataSet mds3d2d1 = new MultiDataSet(f3d1, l2d1, null, lm2d1);
        MultiDataSet mds3d2d2 = new MultiDataSet(f3d2, l2d2, null, lm2d2);
        MultiDataSet merged3d2d = MultiDataSet.merge(Arrays.asList(mds3d2d1, mds3d2d2));

        assertEquals(expLabels2d, merged3d2d.getLabels(0));
        assertEquals(expLM2d, merged3d2d.getLabelsMaskArray(0));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testSplit(Nd4jBackend backend) {

        INDArray[] features = new INDArray[3];
        features[0] = Nd4j.linspace(1, 30, 30, DataType.DOUBLE).reshape('c', 3, 10);
        features[1] = Nd4j.linspace(1, 300, 300, DataType.DOUBLE).reshape('c', 3, 10, 10);
        features[2] = Nd4j.linspace(1, 3 * 5 * 10 * 10, 3 * 5 * 10 * 10, DataType.DOUBLE).reshape('c', 3, 5, 10, 10);

        INDArray[] labels = new INDArray[3];
        labels[0] = Nd4j.linspace(1, 30, 30, DataType.DOUBLE).reshape('c', 3, 10).addi(0.5);
        labels[1] = Nd4j.linspace(1, 300, 300, DataType.DOUBLE).reshape('c', 3, 10, 10).addi(0.3);
        labels[2] = Nd4j.linspace(1, 3 * 5 * 10 * 10, 3 * 5 * 10 * 10, DataType.DOUBLE).reshape('c', 3, 5, 10, 10).addi(0.1);

        INDArray[] fMask = new INDArray[3];
        fMask[1] = Nd4j.linspace(1, 30, 30, DataType.DOUBLE).reshape('f', 3, 10);

        INDArray[] lMask = new INDArray[3];
        lMask[1] = Nd4j.linspace(1, 30, 30, DataType.DOUBLE).reshape('f', 3, 10).addi(0.5);

        MultiDataSet mds = new MultiDataSet(features, labels, fMask, lMask);

        List<org.nd4j.linalg.dataset.api.MultiDataSet> list = mds.asList();

        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            MultiDataSet m = (MultiDataSet) list.get(i);
            assertEquals(2, m.getFeatures(0).rank());
            assertEquals(3, m.getFeatures(1).rank());
            assertEquals(4, m.getFeatures(2).rank());

            assertArrayEquals(new long[] {1, 10}, m.getFeatures(0).shape());
            assertArrayEquals(new long[] {1, 10, 10}, m.getFeatures(1).shape());
            assertArrayEquals(new long[] {1, 5, 10, 10}, m.getFeatures(2).shape());

            assertEquals(features[0].get(NDArrayIndex.interval(i,i,true), NDArrayIndex.all()), m.getFeatures(0));
            assertEquals(features[1].get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all()),
                    m.getFeatures(1));
            assertEquals(features[2].get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all(),
                    NDArrayIndex.all()), m.getFeatures(2));

            assertEquals(2, m.getLabels(0).rank());
            assertEquals(3, m.getLabels(1).rank());
            assertEquals(4, m.getLabels(2).rank());

            assertArrayEquals(new long[] {1, 10}, m.getLabels(0).shape());
            assertArrayEquals(new long[] {1, 10, 10}, m.getLabels(1).shape());
            assertArrayEquals(new long[] {1, 5, 10, 10}, m.getLabels(2).shape());

            assertEquals(labels[0].get(NDArrayIndex.interval(i,i,true), NDArrayIndex.all()), m.getLabels(0));
            assertEquals(labels[1].get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all()),
                    m.getLabels(1));
            assertEquals(labels[2].get(NDArrayIndex.interval(i, i, true), NDArrayIndex.all(), NDArrayIndex.all(),
                    NDArrayIndex.all()), m.getLabels(2));

            assertNull(m.getFeaturesMaskArray(0));
            assertEquals(fMask[1].get(NDArrayIndex.interval(i,i,true), NDArrayIndex.all()), m.getFeaturesMaskArray(1));

            assertNull(m.getLabelsMaskArray(0));
            assertEquals(lMask[1].get(NDArrayIndex.interval(i,i,true), NDArrayIndex.all()), m.getLabelsMaskArray(1));
        }
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testToString(Nd4jBackend backend) {
        //Mask arrays, and different lengths

        int tsLengthIn0 = 8;
        int tsLengthIn1 = 9;
        int tsLengthOut0 = 10;
        int tsLengthOut1 = 11;

        int nRows = 5;
        int nColsIn0 = 3;
        int nColsIn1 = 4;
        int nColsOut0 = 5;
        int nColsOut1 = 6;

        INDArray expectedIn0 = Nd4j.zeros(nRows, nColsIn0, tsLengthIn0);
        INDArray expectedIn1 = Nd4j.zeros(nRows, nColsIn1, tsLengthIn1);
        INDArray expectedOut0 = Nd4j.zeros(nRows, nColsOut0, tsLengthOut0);
        INDArray expectedOut1 = Nd4j.zeros(nRows, nColsOut1, tsLengthOut1);

        INDArray expectedMaskIn0 = Nd4j.zeros(nRows, tsLengthIn0);
        INDArray expectedMaskIn1 = Nd4j.zeros(nRows, tsLengthIn1);
        INDArray expectedMaskOut0 = Nd4j.zeros(nRows, tsLengthOut0);
        INDArray expectedMaskOut1 = Nd4j.zeros(nRows, tsLengthOut1);


        Random r = new Random(12345);
        List<MultiDataSet> list = new ArrayList<>(nRows);
        for (int i = 0; i < nRows; i++) {
            int thisRowIn0Length = tsLengthIn0 - i;
            int thisRowIn1Length = tsLengthIn1 - i;
            int thisRowOut0Length = tsLengthOut0 - i;
            int thisRowOut1Length = tsLengthOut1 - i;

            int in0NumElem = thisRowIn0Length * nColsIn0;
            INDArray in0 = Nd4j.linspace(0, in0NumElem - 1, in0NumElem, DataType.DOUBLE).reshape(1, nColsIn0, thisRowIn0Length);

            int in1NumElem = thisRowIn1Length * nColsIn1;
            INDArray in1 = Nd4j.linspace(0, in1NumElem - 1, in1NumElem, DataType.DOUBLE).reshape(1, nColsIn1, thisRowIn1Length);

            int out0NumElem = thisRowOut0Length * nColsOut0;
            INDArray out0 = Nd4j.linspace(0, out0NumElem - 1, out0NumElem, DataType.DOUBLE).reshape(1, nColsOut0, thisRowOut0Length);

            int out1NumElem = thisRowOut1Length * nColsOut1;
            INDArray out1 = Nd4j.linspace(0, out1NumElem - 1, out1NumElem, DataType.DOUBLE).reshape(1, nColsOut1, thisRowOut1Length);

            INDArray maskIn0 = null;
            INDArray maskIn1 = Nd4j.zeros(1, thisRowIn1Length);
            for (int j = 0; j < thisRowIn1Length; j++) {
                if (r.nextBoolean())
                    maskIn1.putScalar(j, 1.0);
            }
            INDArray maskOut0 = null;
            INDArray maskOut1 = Nd4j.zeros(1, thisRowOut1Length);
            for (int j = 0; j < thisRowOut1Length; j++) {
                if (r.nextBoolean())
                    maskOut1.putScalar(j, 1.0);
            }

            expectedIn0.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowIn0Length)}, in0);
            expectedIn1.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowIn1Length)}, in1);
            expectedOut0.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowOut0Length)}, out0);
            expectedOut1.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.all(),
                    NDArrayIndex.interval(0, thisRowOut1Length)}, out1);

            expectedMaskIn0.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowIn0Length)},
                    Nd4j.ones(1, thisRowIn0Length));
            expectedMaskIn1.put(new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowIn1Length)},
                    maskIn1);
            expectedMaskOut0.put(
                    new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowOut0Length)},
                    Nd4j.ones(1, thisRowOut0Length));
            expectedMaskOut1.put(
                    new INDArrayIndex[] {NDArrayIndex.point(i), NDArrayIndex.interval(0, thisRowOut1Length)},
                    maskOut1);

            list.add(new MultiDataSet(new INDArray[] {in0, in1}, new INDArray[] {out0, out1},
                    new INDArray[] {maskIn0, maskIn1}, new INDArray[] {maskOut0, maskOut1}));
        }

        MultiDataSet merged = MultiDataSet.merge(list);
        System.out.println(merged);
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void multiDataSetSaveLoadTest() throws IOException {

        int max = 3;

        Nd4j.getRandom().setSeed(12345);

        for (int numF = 0; numF <= max; numF++) {
            for (int numL = 0; numL <= max; numL++) {
                INDArray[] f = (numF > 0 ? new INDArray[numF] : null);
                INDArray[] l = (numL > 0 ? new INDArray[numL] : null);
                INDArray[] fm = (numF > 0 ? new INDArray[numF] : null);
                INDArray[] lm = (numL > 0 ? new INDArray[numL] : null);

                if (numF > 0) {
                    for (int i = 0; i < f.length; i++) {
                        f[i] = Nd4j.rand(new int[] {3, 4, 5});
                    }
                }
                if (numL > 0) {
                    for (int i = 0; i < l.length; i++) {
                        l[i] = Nd4j.rand(new int[] {2, 3, 4});
                    }
                }
                if (numF > 0) {
                    for (int i = 0; i < Math.min(fm.length, 2); i++) {
                        fm[i] = Nd4j.rand(new int[] {3, 5});
                    }
                }
                if (numL > 0) {
                    for (int i = 0; i < Math.min(lm.length, 2); i++) {
                        lm[i] = Nd4j.rand(new int[] {2, 4});
                    }
                }

                MultiDataSet mds = new MultiDataSet(f, l, fm, lm);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                mds.save(dos);

                byte[] asBytes = baos.toByteArray();

                ByteArrayInputStream bais = new ByteArrayInputStream(asBytes);
                DataInputStream dis = new DataInputStream(bais);

                MultiDataSet mds2 = new MultiDataSet();
                mds2.load(dis);

                assertEquals(mds, mds2,"Failed at [" + numF + "]/[" + numL + "]");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testCnnMergeFeatureMasks(Nd4jBackend backend) {
        //Tests merging of different CNN masks: [mb,1,h,1], [mb,1,1,w], [mb,1,h,w]

        for( int t=0; t<3; t++) {
            log.info("Starting test: {}", t);
            int nOut = 3;
            int width = 5;
            int height = 4;
            int depth = 3;
            int nExamples1 = 2;
            int nExamples2 = 1;

            int length1 = width * height * depth * nExamples1;
            int length2 = width * height * depth * nExamples2;

            INDArray first = Nd4j.linspace(1, length1, length1, DataType.DOUBLE).reshape('c', nExamples1, depth, height, width);
            INDArray second = Nd4j.linspace(1, length2, length2, DataType.DOUBLE).reshape('c', nExamples2, depth, height, width).addi(0.1);
            INDArray third = Nd4j.linspace(1, length2, length2, DataType.DOUBLE).reshape('c', nExamples2, depth, height, width).addi(0.2);

            INDArray fm1 = null;
            INDArray fm2;
            INDArray fm3;
            switch (t){
                case 0:
                    fm2 = Nd4j.ones(1,1,height,1);
                    fm3 = Nd4j.zeros(1,1,height,1);
                    fm3.get(all(), all(), interval(0,2), all()).assign(1.0);
                    break;
                case 1:
                    fm2 = Nd4j.ones(1,1,1,width);
                    fm3 = Nd4j.zeros(1,1,1,width);
                    fm3.get(all(), all(), all(), interval(0,3)).assign(1.0);
                    break;
                case 2:
                    fm2 = Nd4j.ones(1,1,height,width);
                    fm3 = Nd4j.zeros(1,1,height,width);
                    fm3.get(all(), all(), interval(0,2), interval(0,3)).assign(1.0);
                    break;
                default:
                    throw new RuntimeException();
            }

            INDArray fmExpected = Nd4j.concat(0, Nd4j.ones(2, 1, (t == 1 ? 1 : height), (t == 0 ? 1 : width)), fm2, fm3);

            INDArray labels1 = Nd4j.linspace(1, nExamples1 * nOut, nExamples1 * nOut, DataType.DOUBLE).reshape('c', nExamples1, nOut);
            INDArray labels2 = Nd4j.linspace(1, nExamples2 * nOut, nExamples2 * nOut, DataType.DOUBLE).reshape('c', nExamples2, nOut).addi(0.1);
            INDArray labels3 = Nd4j.linspace(1, nExamples2 * nOut, nExamples2 * nOut, DataType.DOUBLE).reshape('c', nExamples2, nOut).addi(0.2);

            MultiDataSet ds1 = new MultiDataSet(first, labels1, fm1, null);
            MultiDataSet ds2 = new MultiDataSet(second, labels2, fm2, null);
            MultiDataSet ds3 = new MultiDataSet(third, labels3, fm3, null);

            MultiDataSet merged = MultiDataSet.merge(Arrays.asList(ds1, ds2, ds3));

            INDArray fMerged = merged.getFeatures(0);
            INDArray lMerged = merged.getLabels(0);
            INDArray fmMerged = merged.getFeaturesMaskArray(0);

            assertArrayEquals(new long[]{nExamples1 + 2*nExamples2, depth, height, width}, fMerged.shape());
            assertArrayEquals(new long[]{nExamples1 + 2*nExamples2, nOut}, lMerged.shape());
            assertArrayEquals(new long[]{nExamples1 + 2*nExamples2, 1, (t == 1 ? 1 : height), (t == 0 ? 1 : width)}, fmMerged.shape());


            assertEquals(first, fMerged.get(interval(0, nExamples1), all(), all(), all()));
            INDArray secondExp = fMerged.get(interval(nExamples1, nExamples1 + nExamples2), all(), all(), all());
            assertEquals(second, secondExp);
            assertEquals(third, fMerged.get(interval(nExamples1 + nExamples2, nExamples1 + 2*nExamples2), all(), all(), all()));
            assertEquals(labels1, lMerged.get(interval(0, nExamples1), all()));
            assertEquals(labels2, lMerged.get(interval(nExamples1, nExamples1 + nExamples2), all()));
            assertEquals(labels3, lMerged.get(interval(nExamples1 + nExamples2, nExamples1 + 2*nExamples2), all()));

            assertEquals(fmExpected, fmMerged);

            //Test merging with an empty DataSet (this should be ignored)
            MultiDataSet merged2 = MultiDataSet.merge(Arrays.asList(ds1, new MultiDataSet(), ds2, ds3));
            assertEquals(merged, merged2);

            //Test merging with no features in one of the DataSets
            INDArray temp = ds1.getFeatures(0);
            ds1.setFeatures(0,null);
            try {
                MultiDataSet.merge(Arrays.asList(ds1, ds2));
                fail("Expected exception");
            } catch (NullPointerException e) {
                //OK
                assertTrue(e.getMessage().contains("null feature array"));
            }

            try {
                MultiDataSet.merge(Arrays.asList(ds2, ds1));
                fail("Expected exception");
            } catch (NullPointerException e) {
                //OK
                assertTrue(e.getMessage().contains("merging"));
            }

            ds1.setFeatures(0, temp);
            ds2.setLabels(0, null);
            try {
                MultiDataSet.merge(Arrays.asList(ds1, ds2));
                fail("Expected exception");
            } catch (NullPointerException e) {
                //OK
                assertTrue(e.getMessage().contains("merging"));
            }

            try {
                MultiDataSet.merge(Arrays.asList(ds2, ds1));
                fail("Expected exception");
            } catch (NullPointerException e) {
                //OK
                assertTrue(e.getMessage().contains("merge"));
            }
        }
    }

    @Override
    public char ordering() {
        return 'c';
    }
}
