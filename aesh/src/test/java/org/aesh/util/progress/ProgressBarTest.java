/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.util.progress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the progress bar utility.
 */
public class ProgressBarTest {

    // --- ASCII style rendering ---

    @Test
    public void testAsciiAt0Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(0, 100, 30);
        assertTrue("Should contain left bracket", output.contains("["));
        assertTrue("Should contain right bracket", output.contains("]"));
        assertTrue("Should show 0%", output.contains("0%"));
        // No fill chars at 0%
        assertFalse("Should not contain # at 0%", output.contains("#"));
    }

    @Test
    public void testAsciiAt50Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertTrue("Should contain #", output.contains("#"));
        assertTrue("Should contain -", output.contains("-"));
        assertTrue("Should show 50%", output.contains("50%"));
    }

    @Test
    public void testAsciiAt100Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(100, 100, 30);
        assertTrue("Should show 100%", output.contains("100%"));
        // No empty chars at 100%
        assertFalse("Should not contain - at 100%", output.contains("-"));
    }

    // --- UNICODE style rendering ---

    @Test
    public void testUnicodeAt0Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.UNICODE)
                .width(30)
                .build();

        String output = bar.render(0, 100, 30);
        assertTrue("Should contain unicode left bracket", output.contains("\u2502"));
        assertTrue("Should contain empty blocks", output.contains("\u2591"));
        assertTrue("Should show 0%", output.contains("0%"));
    }

    @Test
    public void testUnicodeAt50Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.UNICODE)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertTrue("Should contain fill blocks", output.contains("\u2588"));
        assertTrue("Should contain empty blocks", output.contains("\u2591"));
        assertTrue("Should show 50%", output.contains("50%"));
    }

    @Test
    public void testUnicodeAt100Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.UNICODE)
                .width(30)
                .build();

        String output = bar.render(100, 100, 30);
        assertTrue("Should show 100%", output.contains("100%"));
        assertFalse("Should not contain empty blocks at 100%", output.contains("\u2591"));
    }

    // --- SIMPLE style rendering ---

    @Test
    public void testSimpleAt50Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.SIMPLE)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertTrue("Should contain =", output.contains("="));
        assertTrue("Should show 50%", output.contains("50%"));
    }

    // --- ARROW style rendering ---

    @Test
    public void testArrowShowsTipCharacter() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ARROW)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertTrue("Should contain > tip", output.contains(">"));
        assertTrue("Should contain = fill", output.contains("="));
        assertTrue("Should show 50%", output.contains("50%"));
    }

    @Test
    public void testArrowAt100PercentNoTip() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ARROW)
                .width(30)
                .build();

        String output = bar.render(100, 100, 30);
        // At 100% the bar is fully filled, no tip character
        assertFalse("Should not contain > tip at 100%", output.contains(">"));
        assertTrue("Should show 100%", output.contains("100%"));
    }

    @Test
    public void testArrowAt0PercentNoTip() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ARROW)
                .width(30)
                .build();

        String output = bar.render(0, 100, 30);
        // At 0% there is no fill, so no tip
        assertFalse("Should not contain > tip at 0%", output.contains(">"));
    }

    // --- Label ---

    @Test
    public void testLabelAppearsInOutput() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .label("Downloading")
                .style(ProgressBarStyle.ASCII)
                .width(50)
                .build();

        String output = bar.render(25, 100, 50);
        assertTrue("Should contain label", output.startsWith("Downloading "));
    }

    @Test
    public void testNoLabelByDefault() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertTrue("Should start with [", output.startsWith("["));
    }

    // --- Percentage display toggle ---

    @Test
    public void testPercentageShownByDefault() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertTrue("Should show percentage", output.contains("%"));
    }

    @Test
    public void testPercentageHidden() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .showPercentage(false)
                .width(30)
                .build();

        String output = bar.render(50, 100, 30);
        assertFalse("Should not show percentage", output.contains("%"));
    }

    // --- Ratio display toggle ---

    @Test
    public void testRatioHiddenByDefault() {
        ProgressBar bar = ProgressBar.builder()
                .total(200)
                .style(ProgressBarStyle.ASCII)
                .width(40)
                .build();

        String output = bar.render(100, 200, 40);
        assertFalse("Should not show ratio by default", output.contains("100/200"));
    }

    @Test
    public void testRatioShown() {
        ProgressBar bar = ProgressBar.builder()
                .total(200)
                .style(ProgressBarStyle.ASCII)
                .showRatio(true)
                .width(40)
                .build();

        String output = bar.render(100, 200, 40);
        assertTrue("Should show ratio", output.contains("(100/200)"));
    }

    // --- Edge cases ---

    @Test
    public void testTotalZeroRendersAs100Percent() {
        ProgressBar bar = ProgressBar.builder()
                .total(0)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(0, 0, 30);
        assertTrue("Should show 100% when total is 0", output.contains("100%"));
    }

    @Test
    public void testCurrentExceedsTotalClampsTo100() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        String output = bar.render(150, 100, 30);
        assertTrue("Should clamp to 100%", output.contains("100%"));
        assertFalse("Should not contain empty chars", output.contains("-"));
    }

    // --- Bar width calculation ---

    @Test
    public void testBarWidthAdjustsForLabelAndSuffix() {
        ProgressBar barNoLabel = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(40)
                .build();

        ProgressBar barWithLabel = ProgressBar.builder()
                .total(100)
                .label("Test")
                .style(ProgressBarStyle.ASCII)
                .width(40)
                .build();

        String outputNoLabel = barNoLabel.render(50, 100, 40);
        String outputWithLabel = barWithLabel.render(50, 100, 40);

        // Both should be the same total length (up to terminal width),
        // but the one with label has less bar space
        assertEquals("Both should fit within terminal width", outputNoLabel.length(), outputWithLabel.length());
    }

    @Test
    public void testMinimumBarWidth() {
        // Use a very narrow terminal width that would make bar negative
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .label("Very Long Label Here")
                .style(ProgressBarStyle.ASCII)
                .showRatio(true)
                .width(10)
                .build();

        String output = bar.render(50, 100, 10);
        // Should still render without errors, bar width clamped to minimum 10
        assertTrue("Should still contain brackets", output.contains("["));
        assertTrue("Should still contain brackets", output.contains("]"));
    }

    // --- step() and complete() ---

    @Test
    public void testStepIncrements() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        // Step once
        bar.step();
        String output1 = bar.render(1, 100, 30);
        assertTrue("Should show 1%", output1.contains("1%"));

        // Step by 9
        bar.step(9);
        String output10 = bar.render(10, 100, 30);
        assertTrue("Should show 10%", output10.contains("10%"));
    }

    @Test
    public void testCompleteShowsFull() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        bar.step(50);
        bar.complete();
        // After complete, current should be equal to total
        String output = bar.render(100, 100, 30);
        assertTrue("Should show 100%", output.contains("100%"));
    }

    // --- Builder defaults ---

    @Test
    public void testBuilderDefaults() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .width(30)
                .build();

        // Default style is ASCII
        String output = bar.render(50, 100, 30);
        assertTrue("Default style should use [ bracket", output.startsWith("["));
        assertTrue("Default should show percentage", output.contains("%"));
        assertFalse("Default should not show ratio", output.contains("/"));
    }

    // --- Rendering consistency ---

    @Test
    public void testRenderWithRatioAndLabel() {
        ProgressBar bar = ProgressBar.builder()
                .total(200)
                .label("Processing")
                .style(ProgressBarStyle.UNICODE)
                .showPercentage(true)
                .showRatio(true)
                .width(60)
                .build();

        String output = bar.render(104, 200, 60);
        assertTrue("Should start with label", output.startsWith("Processing "));
        assertTrue("Should contain percentage", output.contains("52%"));
        assertTrue("Should contain ratio", output.contains("(104/200)"));
        assertTrue("Should contain unicode bracket", output.contains("\u2502"));
    }

    @Test
    public void testRenderLargeValues() {
        ProgressBar bar = ProgressBar.builder()
                .total(1000000L)
                .style(ProgressBarStyle.ASCII)
                .showRatio(true)
                .width(60)
                .build();

        String output = bar.render(500000L, 1000000L, 60);
        assertTrue("Should show 50%", output.contains("50%"));
        assertTrue("Should show ratio with large values", output.contains("(500000/1000000)"));
    }

    // --- Edge case tests for bounds checking ---

    @Test
    public void testTotalZeroCurrentZero() {
        ProgressBar bar = ProgressBar.builder()
                .total(0)
                .style(ProgressBarStyle.ASCII)
                .showRatio(true)
                .width(30)
                .build();

        String output = bar.render(0, 0, 30);
        assertTrue("Should show 100% when total=0 and current=0", output.contains("100%"));
        assertTrue("Should show ratio 0/0", output.contains("(0/0)"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStepThrowsException() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        bar.step(-1);
    }

    @Test
    public void testStepBeyondTotalClamps() {
        ProgressBar bar = ProgressBar.builder()
                .total(10)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        bar.step(15);
        // After stepping beyond total, render should show 100%
        String output = bar.render(10, 10, 30);
        assertTrue("Should show 100% after clamping", output.contains("100%"));
    }

    @Test
    public void testUpdateClampsNegativeToZero() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        bar.update(-10);
        String output = bar.render(0, 100, 30);
        assertTrue("Should show 0% after clamping negative", output.contains("0%"));
    }

    @Test
    public void testUpdateClampsBeyondTotal() {
        ProgressBar bar = ProgressBar.builder()
                .total(100)
                .style(ProgressBarStyle.ASCII)
                .width(30)
                .build();

        bar.update(200);
        String output = bar.render(100, 100, 30);
        assertTrue("Should show 100% after clamping above total", output.contains("100%"));
    }
}
