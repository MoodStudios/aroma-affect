package com.ovrtechnology.menu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MenuViewportTest {
    @ParameterizedTest
    @CsvSource({"160,120", "256,144", "320,240", "426,240", "640,360", "854,480",
            "1280,720", "1920,1080", "3840,2160", "240,640", "1920,360", "1,1"})
    void layoutsStayVisibleAndMouseCoordinatesRoundTrip(int width, int height) {
        int[][] layouts = {{400,280}, {760,360}, {440,420}, {500,300}, {480,300}, {640,360}, {460,380}, {192,182}};
        for (int[] minimum : layouts) {
            var view = MenuViewport.fit(width, height, minimum[0], minimum[1]);
            assertThat(view.width()).isGreaterThanOrEqualTo(minimum[0]);
            assertThat(view.height()).isGreaterThanOrEqualTo(minimum[1]);
            assertThat(view.scale()).isPositive().isLessThanOrEqualTo(1);
            assertThat(view.offsetX()).isGreaterThanOrEqualTo(-0.0001);
            assertThat(view.offsetY()).isGreaterThanOrEqualTo(-0.0001);
            assertThat(view.offsetX() + view.width() * view.scale()).isLessThanOrEqualTo(width + 0.0001);
            assertThat(view.offsetY() + view.height() * view.scale()).isLessThanOrEqualTo(height + 0.0001);
            // Screen pixels map back to the exact local hit region, even after downscaling.
            for (double fraction : new double[]{0, 0.25, 0.5, 0.75, 1}) {
                double x = width * fraction, y = height * fraction;
                assertThat(view.screenX(view.localX(x))).isCloseTo((int) Math.round(x), within(1));
                assertThat(view.screenY(view.localY(y))).isCloseTo((int) Math.round(y), within(1));
            }
        }
    }

    @Test void ampleSpacePreservesThePlayersGuiScale() {
        var view = MenuViewport.fit(1280, 720, 760, 360);
        assertThat(view.scale()).isEqualTo(1);
        assertThat(view.width()).isEqualTo(1280);
        assertThat(view.height()).isEqualTo(720);
        assertThat(view.localX(123)).isEqualTo(123);
        assertThat(view.localY(456)).isEqualTo(456);
    }

    @Test void zeroSizedWindowDuringMinimizationDoesNotProduceInvalidCoordinates() {
        var view = MenuViewport.fit(0, 0, 760, 360);
        assertThat(Double.isFinite(view.localX(0))).isTrue();
        assertThat(Double.isFinite(view.localY(0))).isTrue();
    }

    @Test void resizingBackRestoresOriginalScaleWithoutAccumulatingTransforms() {
        var original = MenuViewport.fit(854, 480, 500, 300);
        MenuViewport.fit(320, 240, 500, 300);
        MenuViewport.fit(240, 640, 500, 300);
        assertThat(MenuViewport.fit(854, 480, 500, 300)).isEqualTo(original);
    }
    @ParameterizedTest
    @CsvSource({"640,480,2", "800,600,2", "854,480,2", "1024,768,3", "1280,720,3", "1920,1080,4", "1921,1081,3"})
    void lowResolutionsKeepTextPixelsWholeAndClicksAligned(int width, int height, int guiScale) {
        var view = MenuViewport.fitPixels(width, height, guiScale, 640, 360);
        assertThat(view.scale() * guiScale).isEqualTo(Math.floor(view.scale() * guiScale));
        assertThat(view.scale() * guiScale).isGreaterThanOrEqualTo(1);
        assertThat(view.offsetX() * guiScale).isEqualTo(Math.floor(view.offsetX() * guiScale));
        assertThat(view.offsetY() * guiScale).isEqualTo(Math.floor(view.offsetY() * guiScale));
        assertThat(view.width()).isGreaterThanOrEqualTo(640);
        assertThat(view.height()).isGreaterThanOrEqualTo(360);
        assertThat((view.offsetX() + view.width() * view.scale()) * guiScale).isLessThanOrEqualTo(width);
        assertThat((view.offsetY() + view.height() * view.scale()) * guiScale).isLessThanOrEqualTo(height);
        assertThat(view.screenX(view.localX(123))).isEqualTo(123);
        assertThat(view.screenY(view.localY(87))).isEqualTo(87);
    }

    @Test void physicalPixelsUseTheFramebufferInsteadOfRoundedGuiDimensions() {
        var view = MenuViewport.fitPixels(853, 479, 2, 640, 360);
        assertThat(view.width()).isEqualTo(853);
        assertThat(view.height()).isEqualTo(479);
        assertThat(view.scale()).isEqualTo(0.5);
        assertThat(view.offsetX()).isZero();
        assertThat(view.offsetY()).isZero();
    }

    @Test void tinyFramebufferStillHasFiniteCoordinatesAndFits() {
        var view = MenuViewport.fitPixels(320, 240, 1, 640, 360);
        assertThat(view.width() * view.scale()).isLessThanOrEqualTo(320);
        assertThat(view.height() * view.scale()).isLessThanOrEqualTo(240);
        assertThat(view.localX(100)).isFinite();
        assertThat(view.localY(100)).isFinite();
    }
}
