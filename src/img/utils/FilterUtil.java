package img.utils;

import img.common.SolarCenter;
import img.dto.NeighbourPixel;
import img.dto.Pixel;

import java.util.ArrayList;
import java.util.List;

public class FilterUtil {
    public static double calculateKernel(Pixel neighbourPixel, Pixel pixel, double sd){
        double a = Math.pow((pixel.getRadius() - neighbourPixel.getRadius()), 2);
        double b = Math.pow(pixel.getRadius() * CoordinateUtil.angleDifference(pixel.getAngleCooefficient(), neighbourPixel.getAngleCooefficient()), 2);

        double numerator =  a + b;
        double powerFactor = numerator / (2 * Math.pow(sd, 2));

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
//                normalizedValue = Double.valueOf((newMax / range) * (filteredPixelValues.get(i).get(j).intValue() - minValue)).intValue();
//                normalizedValue = Double.valueOf(((filteredPixelValues.get(i).get(j).intValue() - minValue) * ((newMax-newMin)/(maxValue-minValue)))+newMin).intValue();
/*                if ( i < SolarCenter.solarCenterX && j < SolarCenter.solarCenterY){
                    rowPixels.add(60000);
                } else {
                    rowPixels.add(normalizedValue);
                }*/
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
