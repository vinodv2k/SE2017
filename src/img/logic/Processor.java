package img.logic;

import img.ImageHelper;
import img.common.DegreeConstants;
import img.common.Lunar;
import img.dto.Image;
import img.dto.Pixel;
import img.dto.Request;
import img.utils.CoordinateUtil;
import img.utils.FilterUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Processor {

    private double standardDeviation;
    private List<List<Pixel>> imageAsPixels;
    public void processCorona(Request request){
        ImageHelper imageHelper = new ImageHelper();
        Image image = new Image();
        this.imageAsPixels = imageHelper.getPixelsArrayFromImage(request, image);

        CoordinateUtil.updateSolarCenter(image);
        this.standardDeviation = request.getStandardDeviation();

        TreeMap<Double, List<Double>> radAngle = new TreeMap<>();
        TreeMap<Double, TreeMap<Double, Pixel>> radAnglePixelMap = new TreeMap<>();

        TreeMap<Double, Pixel> anglePixelMap = null;
        Pixel currentPixel = null;
        List<Double> angles = null;
        for(int i = 0; i < this.imageAsPixels.size(); i++){
            for(int j = 0; j < this.imageAsPixels.get(i).size(); j++){
                currentPixel = this.imageAsPixels.get(i).get(j);
                CoordinateUtil.updatePolarCoordinates(currentPixel);

                if (radAngle.get(currentPixel.getRoundedRadius()) != null && radAngle.get(currentPixel.getRoundedRadius()).size() > 0){
                    radAngle.get(currentPixel.getRoundedRadius()).add(currentPixel.getRoundedAngle());
                } else {
                    angles = new ArrayList<>();
                    angles.add(currentPixel.getRoundedAngle());
                    radAngle.put(currentPixel.getRoundedRadius(), angles);
                }

                if(radAnglePixelMap.get(currentPixel.getRoundedRadius()) == null || radAnglePixelMap.get(currentPixel.getRoundedRadius()).size() == 0){
                    // add new treemap
                    anglePixelMap = new TreeMap<>();
                    anglePixelMap.put(currentPixel.getRoundedAngle(), currentPixel);
                    radAnglePixelMap.put(currentPixel.getRoundedRadius(), anglePixelMap);
                } else {
                    radAnglePixelMap.get(currentPixel.getRoundedRadius()).put(currentPixel.getRoundedAngle(), currentPixel);
                }
            }
        }

/*        List<List<Integer>> filteredPixelValues = this.imageAsPixels.stream().map(colPixels -> {
            return colPixels.stream().map(pixel -> {
//                 System.out.println("Working on ("+pixel.getX()+", "+pixel.getY()+")");
                int filteredValue = 0;
                if (pixel.getRadius() > 10) {
                    filteredValue = findNeighbouringPixels(pixel, radAnglePixelMap);
                }
                return filteredValue;
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());*/

        this.imageAsPixels.stream().map(colPixels -> {
            return colPixels.stream().map(pixel -> {
//                 System.out.println("Working on ("+pixel.getX()+", "+pixel.getY()+")");
                int filteredValue = 0;
                if (pixel.getRadius() > Lunar.radius) {
                    findNeighbouringPixels(pixel, radAnglePixelMap);
                    filteredValue = pixel.getProcessedValue();
                }
//                pixel.setFilteredValue(filteredValue);
                return filteredValue;
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());
        FilterUtil.normalizeFilteredPixels(this.imageAsPixels);
        int[][] normalizedIntArray = CoordinateUtil.convertPixelsToIntArray(this.imageAsPixels);
//        printAllValues(normalizedIntArray);
        String fileName = request.getFilePath().substring(request.getFilePath().lastIndexOf('/')+1, request.getFilePath().length());
        imageHelper.writeToTiff(normalizedIntArray, image.getWidth(), image.getHeight(), this.standardDeviation, fileName);
    }

    private void printAllValues(int[][] values){
        for(int i = 0; i < values.length; i++)
        {
            for(int j = 0; j < values[i].length; j++)
            {
                System.out.printf("%5d ", values[i][j]);
            }
            System.out.println();
        }
    }

    private int findNeighbouringPixels(Pixel currentPixel, TreeMap<Double, TreeMap<Double, Pixel>> radAnglePixelMap){
        double radiusRangeLower = (currentPixel.getRadius()) - (2 * this.standardDeviation);
        double radiusRangeUpper = (currentPixel.getRadius()) + (2 * this.standardDeviation);

        double lowerAngleRange = (currentPixel.getAngle() - ((2 * this.standardDeviation) / currentPixel.getRadius()));
        double upperAngleRange = (currentPixel.getAngle() + ((2 * this.standardDeviation) / currentPixel.getRadius()));

        double sumA = 0; double sumB = 0;

        Pixel n_pixel = null;
        SortedMap<Double,TreeMap<Double, Pixel>> radToAnglesSubMap = radAnglePixelMap.subMap(radiusRangeLower, radiusRangeUpper);
        for (Map.Entry<Double, TreeMap<Double, Pixel>> anglesMap : radToAnglesSubMap.entrySet()) {
            if (anglesMap.getKey() <= Lunar.radius){
                continue;
            }

            // When lowerAngleRange is out of range with the angles, adjust the angle value to get the offset of the angle
            // from the max angle range which is -3.14. After -3.14, it is considered 0.01 and not -3.15
            // So, start from offset and find all the angles until 0.01. Then, reset the lowerAngleRange to -3.14 to continue with the normal process.
            if(upperAngleRange > DegreeConstants.RADIANS_360){
                for(double i  = upperAngleRange; i > DegreeConstants.RADIANS_360; i = i - 0.01) {
                    double adjustedAngle = i - DegreeConstants.RADIANS_360;
                    n_pixel = anglesMap.getValue().get(adjustedAngle);
                    if (n_pixel == null){
                        continue;
                    }
                    System.out.println("hi");
                    double kernelValue = FilterUtil.calculateKernel(n_pixel, currentPixel, this.standardDeviation);
                    sumA += (n_pixel.getPixelValue() * kernelValue);
                    sumB += kernelValue;
                }
                upperAngleRange = DegreeConstants.RADIANS_360;

/*                double diff = CoordinateUtil.round((-3.14 + Math.abs(lowerAngleRange)), 2);
                for(double i = diff; i > 0.01; i = i - 0.01){
                    n_pixel = anglesMap.getValue().get(i);
                    if (n_pixel == null){
                        continue;
                    }
                    System.out.println("hi");
                    double kernelValue = FilterUtil.calculateKernel(n_pixel, currentPixel, this.standardDeviation);
                    sumA += (n_pixel.getPixelValue() * kernelValue);
                    sumB += kernelValue;
                }
                lowerAngleRange = -3.14;*/
            }

            SortedMap<Double, Pixel> angleSubMap = anglesMap.getValue().subMap(lowerAngleRange, upperAngleRange);

            for (Map.Entry<Double, Pixel> angleMapEntry : angleSubMap.entrySet()) {
//                System.out.println(currentPixel.getxOffset()+"\t"+currentPixel.getyOffset()+"\t"+angleMapEntry.getValue().getxOffset()+"\t"+angleMapEntry.getValue().getyOffset()+"\t");
                /*if (angleMapEntry.getValue().getRadius() <= Lunar.radius){
                    continue;
                }*/

                double kernelValue = FilterUtil.calculateKernel(angleMapEntry.getValue(), currentPixel, this.standardDeviation);
                sumA += (angleMapEntry.getValue().getPixelValue() * kernelValue);
                sumB += kernelValue;
            }
        }

        int subtract = sumB == 0 ? 0 : Long.valueOf(Math.round(sumA / sumB)).intValue();
        int filteredPixelValue = currentPixel.getPixelValue() - subtract;
        currentPixel.setFilteredValue(filteredPixelValue);
        currentPixel.setProcessedValue((2*currentPixel.getPixelValue()) + (2 * currentPixel.getFilteredValue()));
        return filteredPixelValue;
    }

    private int getPixelValueOf(int x, int y){
        try{
            return this.imageAsPixels.get(x).get(y).getPixelValue();
        } catch(Exception e){
            System.out.println(e.getStackTrace());
            return 0;
        }
    }
}