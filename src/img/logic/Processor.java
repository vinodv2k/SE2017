package img.logic;

import img.ImageHelper;
import img.common.DegreeConstants;
import img.dto.Image;
import img.dto.Pixel;
import img.dto.Request;
import img.utils.CoordinateUtil;
import img.utils.FilterUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Processor {

    private double standardDeviation;
    private List<List<Pixel>> imgInfo;
    private List<Pixel> imagePixels;

    public void processCorona(Request request) {
        ImageHelper imageHelper = new ImageHelper();
        Image image = new Image();
        this.imgInfo = imageHelper.getPixelsArrayFromImage(request, image);

        CoordinateUtil.updateSolarCenter(image);
        this.standardDeviation = request.getStandardDeviation();

        TreeMap<Double, List<Double>> radAngle = new TreeMap<>();
        TreeMap<Double, TreeMap<Double, Pixel>> radAnglePixelMap = new TreeMap<>();

        TreeMap<Double, Pixel> anglePixelMap = null;
        Pixel currentPixel = null;
        List<Double> angles = null;
        this.imagePixels = new ArrayList<>();
        for (int i = 0; i < this.imgInfo.size(); i++) {
            for (int j = 0; j < this.imgInfo.get(i).size(); j++) {
                currentPixel = this.imgInfo.get(i).get(j);
                CoordinateUtil.updatePolarCoordinates(currentPixel);
                updateNeighbourSearchRadius(currentPixel);
                updateNeighbourSearchAngle(currentPixel);
                this.imagePixels.add(currentPixel);

                if (radAngle.get(currentPixel.getRoundedRadius()) != null && radAngle.get(currentPixel
                    .getRoundedRadius()).size() > 0) {
                    radAngle.get(currentPixel.getRoundedRadius()).add(currentPixel.getRoundedAngle());
                } else {
                    angles = new ArrayList<>();
                    angles.add(currentPixel.getRoundedAngle());
                    radAngle.put(currentPixel.getRoundedRadius(), angles);
                }

                if (radAnglePixelMap.get(currentPixel.getRoundedRadius()) == null || radAnglePixelMap.get
                    (currentPixel.getRoundedRadius()).size() == 0) {
                    // add new treemap
                    anglePixelMap = new TreeMap<>();
                    anglePixelMap.put(currentPixel.getRoundedAngle(), currentPixel);
                    radAnglePixelMap.put(currentPixel.getRoundedRadius(), anglePixelMap);
                } else {
                    radAnglePixelMap.get(currentPixel.getRoundedRadius()).put(currentPixel.getRoundedAngle
                        (), currentPixel);
                }
            }
        }

        List<List<Integer>> filteredPixelValues = this.imgInfo.stream().map(colPixels -> {
            return colPixels.stream().map(pixel -> {
//                 System.out.println("Working on ("+pixel.getX()+", "+pixel.getY()+")");
                int filteredValue = applyAchf(pixel, radAnglePixelMap);
//                return filteredValue < 0 ? 0 : filteredValue;
                return filteredValue;
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());

//        filteredValue = findNeighbouringPixelsAndApplyFilter(this.imgInfo.get(20).get(20));
//        pixelValues.add(filteredValue < 0 ? 0 : filteredValue);
//        pixelValues.add(filteredValue);

        int[][] normalizedIntArray = CoordinateUtil.convertBackToIntArray(FilterUtil.normalize
            (filteredPixelValues));
//        printAllValues(normalizedIntArray);
        String fileName = request.getFilePath().substring(request.getFilePath().lastIndexOf('/') + 1,
            request.getFilePath().length());
        imageHelper.writeToTiff(normalizedIntArray, image.getWidth(), image.getHeight(), this
            .standardDeviation, fileName);
    }

    private void printAllValues(int[][] values) {
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                System.out.printf("%5d ", values[i][j]);
            }
            System.out.println();
        }
    }

    private int applyAchf(Pixel currentPixel, TreeMap<Double, TreeMap<Double, Pixel>>
        radAnglePixelMap) {
//        this.standardDeviation = 1;
//        CoordinateUtil.updatePolarCoordinates(currentPixel);
/*        if (currentPixel.getRoundedRadius() == 22.62){
            return 0;
        }*/

        if (currentPixel.getRoundedRadius() <= 30){
            return 0;
        }

        double radiusRangeLower = (currentPixel.getRadius()) - (2 * this.standardDeviation);
        double radiusRangeUpper = (currentPixel.getRadius()) + (2 * this.standardDeviation);

        double lowerAngleRange = (currentPixel.getAngle() - ((2 * this.standardDeviation) / currentPixel
            .getRadius()));
        double upperAngleRange = (currentPixel.getAngle() + ((2 * this.standardDeviation) / currentPixel
            .getRadius()));

        double sumA = 0;
        double sumB = 0;

        SortedMap<Double, TreeMap<Double, Pixel>> radiusRangeMap = radAnglePixelMap.subMap(radiusRangeLower,
            radiusRangeUpper);
        for (Map.Entry<Double, TreeMap<Double, Pixel>> angleMap : radiusRangeMap.entrySet()) {

            SortedMap<Double, Pixel> angleSubMap = null;

            Pixel n_pixel = null;
            if(lowerAngleRange < DegreeConstants.RADIANS_0){
                for (double i = lowerAngleRange; i <= DegreeConstants.RADIANS_0; i = i + 0.01) {

                    double adjustedAngle = CoordinateUtil.round(DegreeConstants.RADIANS_0 + i, 2);
                    n_pixel = angleMap.getValue().get(adjustedAngle);
                    if (n_pixel == null){
                        continue;
                    }
//                    System.out.println(adjustedAngle);
                    double kernelValue = FilterUtil.calculateKernel(n_pixel, currentPixel, this.standardDeviation);
                    sumA += (n_pixel.getPixelValue() * kernelValue);
                    sumB += kernelValue;
                }
                lowerAngleRange = DegreeConstants.RADIANS_0;
            }

            if (upperAngleRange > 360){
                angleSubMap = angleMap.getValue().subMap(DegreeConstants.RADIANS_360, upperAngleRange);
                for (Map.Entry<Double, Pixel> angleMapEntry : angleSubMap.entrySet()) {
//                System.out.println(currentPixel.getxOffset()+"\t"+currentPixel.getyOffset()
// +"\t"+angleMapEntry.getValue().getxOffset()+"\t"+angleMapEntry.getValue().getyOffset()+"\t");
                    System.out.println("hello");
                    double kernelValue = FilterUtil.calculateKernel(angleMapEntry.getValue(), currentPixel,
                        this.standardDeviation);
                    sumA += (angleMapEntry.getValue().getPixelValue() * kernelValue);
                    sumB += kernelValue;
                }
                upperAngleRange = DegreeConstants.RADIANS_360;
            }

//            System.out.println(lowerAngleRange+","+upperAngleRange);
            angleSubMap = angleMap.getValue().subMap(lowerAngleRange, upperAngleRange);
            for (Map.Entry<Double, Pixel> angleMapEntry : angleSubMap.entrySet()) {
                if (angleMapEntry.getValue().getRadius() <= 30){
                    return 65536;
                }
                if(currentPixel.getAngle() == DegreeConstants.RADIANS_45){
                    System.out.println(angleSubMap.size());
                }
                double kernelValue = FilterUtil.calculateKernel(angleMapEntry.getValue(), currentPixel,
                    this.standardDeviation);
                sumA += (angleMapEntry.getValue().getPixelValue() * kernelValue);
                sumB += kernelValue;
            }
        }

        int subtract = sumB == 0 ? 0 : Long.valueOf(Math.round(sumA / sumB)).intValue();
        int filteredPixelValue = currentPixel.getPixelValue() - subtract;
        return filteredPixelValue;
    }


    private void temp(){

        this.imagePixels.stream().forEach(pixel -> {
            List<Pixel> neighbouringPixels = this.imagePixels.stream().filter(pixels -> {
                return pixel.getRadius() >= pixels.getNeighbourRadiusLowerLevel() && pixel.getRadius() <=
                    pixels.getNeighbourRadiusUpperLevel();
            }).filter(np -> {
                return pixel.getAngle() >= np.getNeighbourAngleLowerLevel() && pixel.getAngle() <= np
                    .getNeighbourAngleUpperLevel();
            }).collect(Collectors.toList());
        });

    }


    private void updateNeighbourSearchRadius(Pixel pixel){
        pixel.setRadius(pixel.getRadius() - (2 * this.standardDeviation));
        pixel.setRadius(pixel.getRadius() + (2 * this.standardDeviation));
    }
    
    private void updateNeighbourSearchAngle(Pixel pixel){
        pixel.setNeighbourAngleLowerLevel(pixel.getAngle() - ((2 * this.standardDeviation) / pixel
            .getRadius()));
        pixel.setNeighbourAngleUpperLevel(pixel.getAngle() + ((2 * this.standardDeviation) / pixel
            .getRadius()));

    }

    private int getPixelValueOf(int x, int y) {
        try {
            return this.imgInfo.get(x).get(y).getPixelValue();
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            return 0;
        }
    }
}