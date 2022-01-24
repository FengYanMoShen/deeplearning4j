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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.BaseNd4jTestWithBackends;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.TestMultiDataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.MultiNormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.ops.transforms.Transforms;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TagNames.NDARRAY_ETL)
@NativeTag
@Tag(TagNames.FILE_IO)
public class MultiNormalizerMinMaxScalerTest extends BaseNd4jTestWithBackends {
    private static final double TOLERANCE_PERC = 0.01; // 0.01% of correct value
    private static final int INPUT1_SCALE = 1, INPUT2_SCALE = 2, OUTPUT1_SCALE = 3, OUTPUT2_SCALE = 4;

    private MultiNormalizerMinMaxScaler SUT;
    private MultiDataSet data;

    private double naturalMin;
    private double naturalMax;

    @BeforeEach
    public void setUp() {
        SUT = new MultiNormalizerMinMaxScaler();
        SUT.fitLabel(true);

        // Prepare test data
        int nSamples = 5120;

        INDArray values = Nd4j.linspace(1, nSamples, nSamples, Nd4j.dataType()).reshape(1, -1).transpose();
        INDArray input1 = values.mul(INPUT1_SCALE);
        INDArray input2 = values.mul(INPUT2_SCALE);
        INDArray output1 = values.mul(OUTPUT1_SCALE);
        INDArray output2 = values.mul(OUTPUT2_SCALE);

        data = new MultiDataSet(new INDArray[] {input1, input2}, new INDArray[] {output1, output2});

        naturalMin = 1;
        naturalMax = nSamples;
    }


    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMultipleInputsAndOutputsWithDataSet(Nd4jBackend backend) {
        SUT.fit(data);
        assertExpectedMinMax();
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testMultipleInputsAndOutputsWithIterator(Nd4jBackend backend) {
        MultiDataSetIterator iter = new TestMultiDataSetIterator(1, data);
        SUT.fit(iter);
        assertExpectedMinMax();
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testRevertFeaturesINDArray(Nd4jBackend backend) {
        SUT.fit(data);

        MultiDataSet transformed = data.copy();
        SUT.preProcess(transformed);

        INDArray reverted = transformed.getFeatures(0).dup();
        SUT.revertFeatures(reverted, null, 0);

        assertNotEquals(reverted, transformed.getFeatures(0));

        SUT.revert(transformed);
        assertEquals(reverted, transformed.getFeatures(0));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testRevertLabelsINDArray(Nd4jBackend backend) {
        SUT.fit(data);

        MultiDataSet transformed = data.copy();
        SUT.preProcess(transformed);

        INDArray reverted = transformed.getLabels(0).dup();
        SUT.revertLabels(reverted, null, 0);

        assertNotEquals(reverted, transformed.getLabels(0));

        SUT.revert(transformed);
        assertEquals(reverted, transformed.getLabels(0));
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testRevertMultiDataSet(Nd4jBackend backend) {
        SUT.fit(data);

        MultiDataSet transformed = data.copy();
        SUT.preProcess(transformed);

        double diffBeforeRevert = getMaxRelativeDifference(data, transformed);
        assertTrue(diffBeforeRevert > TOLERANCE_PERC);

        SUT.revert(transformed);

        double diffAfterRevert = getMaxRelativeDifference(data, transformed);
        assertTrue(diffAfterRevert < TOLERANCE_PERC);
    }

    @ParameterizedTest
    @MethodSource("org.nd4j.linalg.BaseNd4jTestWithBackends#configs")
    public void testFullyMaskedData() {
        MultiDataSetIterator iter = new TestMultiDataSetIterator(1,
                new MultiDataSet(new INDArray[] {Nd4j.create(new float[] {1}).reshape(1, 1, 1)},
                        new INDArray[] {Nd4j.create(new float[] {2}).reshape(1, 1, 1)}),
                new MultiDataSet(new INDArray[] {Nd4j.create(new float[] {2}).reshape(1, 1, 1)},
                        new INDArray[] {Nd4j.create(new float[] {4}).reshape(1, 1, 1)}, null,
                        new INDArray[] {Nd4j.create(new float[] {0}).reshape(1, 1)}));

        SUT.fit(iter);

        // The label min value should be 2, as the second row with 4 is masked.
        assertEquals(2f, SUT.getLabelMin(0).getFloat(0), 1e-6);
    }

    private double getMaxRelativeDifference(MultiDataSet a, MultiDataSet b) {
        double max = 0;
        for (int i = 0; i < a.getFeatures().length; i++) {
            INDArray inputA = a.getFeatures()[i];
            INDArray inputB = b.getFeatures()[i];
            INDArray delta = Transforms.abs(inputA.sub(inputB)).div(inputB);
            double maxdeltaPerc = delta.max(0, 1).mul(100).getDouble(0);
            if (maxdeltaPerc > max) {
                max = maxdeltaPerc;
            }
        }
        return max;
    }

    private void assertExpectedMinMax() {
        assertSmallDifference(naturalMin * INPUT1_SCALE, SUT.getMin(0).getDouble(0));
        assertSmallDifference(naturalMax * INPUT1_SCALE, SUT.getMax(0).getDouble(0));

        assertSmallDifference(naturalMin * INPUT2_SCALE, SUT.getMin(1).getDouble(0));
        assertSmallDifference(naturalMax * INPUT2_SCALE, SUT.getMax(1).getDouble(0));

        assertSmallDifference(naturalMin * OUTPUT1_SCALE, SUT.getLabelMin(0).getDouble(0));
        assertSmallDifference(naturalMax * OUTPUT1_SCALE, SUT.getLabelMax(0).getDouble(0));

        assertSmallDifference(naturalMin * OUTPUT2_SCALE, SUT.getLabelMin(1).getDouble(0));
        assertSmallDifference(naturalMax * OUTPUT2_SCALE, SUT.getLabelMax(1).getDouble(0));
    }

    private void assertSmallDifference(double expected, double actual) {
        double delta = Math.abs(expected - actual);
        double deltaPerc = (delta / expected) * 100;
        assertTrue(deltaPerc < TOLERANCE_PERC);
    }


    @Override
    public char ordering() {
        return 'c';
    }
}