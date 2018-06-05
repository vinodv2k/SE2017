package img.utils;

import img.common.Lunar;
import img.common.SolarCenter;
import img.dto.NeighbourPixel;
import img.dto.Pixel;

import java.util.ArrayList;
import java.util.List;

public class FilterUtil {
    public static double calculateKernel(Pixel neighbourPixel, Pixel pixel, double sd){
        double a = Math.pow((pixel.getRadius()-neighbourPixel.getRadius()), 2);
        double b = Math.pow(pixel.getRadius() * (pixel.getAngle() - neighbourPixel
            .getAngle()), 2);

        double numerator =  a + b;
        double powerFactor = numerator / Math.pow((sd * 2), 2);

        return Math.exp( (-1) * powerFactor);
     }

    public static List<List<Integer>> normalize(List<List<Integer>> filteredPixelValues) {

        int[] extremes = findMaxValue(filteredPixelValues);
        double minValue = extremes[0];
        double maxValue = extremes[1];

        double range = maxValue - minValue;

        final int newMin = 0;
        final int newMax = 65535;

        List<List<Integer>> normalizedPixelValues = new ArrayList<>();
        List<Integer> rowPixels = new ArrayList<>();

//        maxRange ( (element.value - minValue) / (maxValue - minValue))

        int normalizedValue = 0;
        for(int i = 0; i < filteredPixelValues.size(); i++){
            for(int j = 0; j < filteredPixelValues.get(i).size(); j++){
                normalizedValue = Double.valueOf(newMax * ((filteredPixelValues.get(i).get(j).intValue() - minValue) / range)).intValue();
                rowPixels.add(normalizedValue);
            }
            normalizedPixelValues.add(rowPixels);
            rowPixels = new ArrayList<>();
        }

        /*
        * pixel value - minimumFromOriginalImage * 65536 / range
        * */

        return normalizedPixelValues;
    }


    public static void  normalizeFilteredPixels(List<List<Pixel>> imageAsPixels){
        int[] extremes = findExtremeValues(imageAsPixels);
        double minValue = extremes[0];
        double maxValue = extremes[1];
        System.out.println(minValue + " to " + maxValue);
        double range = maxValue - minValue;

        final int newMin = 32768;
        final int newMax = 65535;
        final int newRange = newMax - newMin;

        imageAsPixels.stream().forEach(colPixelList -> {
            colPixelList.stream().forEach(pixel -> {
                if(pixel.getRadius() <= Lunar.radius){
                    pixel.setNormalizedValue(0);
                } else {
//                    int normalizedValue = Double.valueOf((range)*(pixel.getFilteredValue() - minValue)).intValue();
                    int normalizedValue = Double.valueOf((newMax) + (((pixel.getFilteredValue() - minValue) *
                        newMax) / range)).intValue();
//                    int normalizedValue = Double.valueOf(newRange * (pixel.getFilteredValue() - minValue) / range).intValue();
                    pixel.setNormalizedValue(normalizedValue);
                }
            });
        });
    }

    private static int[] findExtremeValues(List<List<Pixel>> pixelValues) {
        int maxValue = 0;
        int minValue = 0;
        for (List<Pixel> columnPixels: pixelValues) {
            for(Pixel pixel: columnPixels){
                /*if(pixel.getRadius() <= Lunar.radius){
                    continue;
                }*/
                int pixelValue = pixel.getFilteredValue();
                if(pixelValue > maxValue){
                    maxValue = pixelValue;
                }
                if(pixelValue < minValue){
                    minValue = pixelValue;
                }
            }
        }
        return new int[]{minValue, maxValue};
    }

    private static int[] findMaxValue(List<List<Integer>> pixelValues) {
        int maxValue = 0;
        int minValue = 0;
        for (List<Integer> columnPixels: pixelValues) {
            for(Integer pixelValue: columnPixels){
                if(pixelValue > maxValue){
                    maxValue = pixelValue;
                }
                if(pixelValue < minValue){
                    minValue = pixelValue;
                }
            }
        }
        return new int[]{minValue, maxValue};
    }
}
