package img.logic;

import img.ImageHelper;
import img.dto.Image;
import img.dto.NeighbourPixel;
import img.dto.Pixel;
import img.dto.Request;
import img.utils.CoordinateUtil;
import img.utils.FilterUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.cert.CollectionCertStoreParameters;
import java.util.*;
import java.util.logging.Filter;
import java.util.stream.Collectors;

public class Processor {

    private double standardDeviation;
    private List<List<Pixel>> imgInfo;
    public void processCorona(Request request){
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
        for(int i = 0; i < this.imgInfo.size(); i++){
            for(int j = 0; j < this.imgInfo.get(i).size(); j++){
                currentPixel = this.imgInfo.get(i).get(j);
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

        List<List<Integer>> filteredPixelValues = this.imgInfo.stream().map(colPixels -> {
             return colPixels.stream().map(pixel -> {
//                 System.out.println("Working on ("+pixel.getX()+", "+pixel.getY()+")");
                int filteredValue = findNeighbouringPixels(pixel, radAnglePixelMap);
//                return filteredValue < 0 ? 0 : filteredValue;
                return filteredValue;
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());

//        filteredValue = findNeighbouringPixelsAndApplyFilter(this.imgInfo.get(20).get(20));
//        pixelValues.add(filteredValue < 0 ? 0 : filteredValue);
//        pixelValues.add(filteredValue);

        int[][] normalizedIntArray = CoordinateUtil.convertBackToIntArray(FilterUtil.normalize(filteredPixelValues));
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
//        this.standardDeviation = 1;
//        CoordinateUtil.updatePolarCoordinates(currentPixel);
/*        if (currentPixel.getRoundedRadius() == 22.62){
            return 0;
        }*/

        double radiusRangeLower = (currentPixel.getRadius()) - (2 * this.standardDeviation);
        double radiusRangeUpper = (currentPixel.getRadius()) + (2 * this.standardDeviation);

        double lowerAngleRange = (currentPixel.getAngle() - ((2 * this.standardDeviation) / currentPixel.getRadius()));
        double upperAngleRange = (currentPixel.getAngle() + ((2 * this.standardDeviation) / currentPixel.getRadius()));

        Pixel neighbourPixel = null;
        double sumA = 0; double sumB = 0;
        int totalNeighbouringPixels = 0;
        SortedMap<Double,TreeMap<Double, Pixel>> subMapEntry = radAnglePixelMap.subMap(radiusRangeLower, radiusRangeUpper);
        for (Map.Entry<Double, TreeMap<Double, Pixel>> angleMap : subMapEntry.entrySet()) {
//            double radius = angleMap.getKey();
            SortedMap<Double, Pixel> angleSubMap = angleMap.getValue().subMap(lowerAngleRange, upperAngleRange);
            for (Map.Entry<Double, Pixel> angleMapEntry : angleSubMap.entrySet()) {
                if (angleMapEntry.getValue().getRadius() < 30){
                    return 0;
                }
//                System.out.println(currentPixel.getxOffset()+"\t"+currentPixel.getyOffset()+"\t"+angleMapEntry.getValue().getxOffset()+"\t"+angleMapEntry.getValue().getyOffset()+"\t");
                double kernelValue = FilterUtil.calculateKernel(angleMapEntry.getValue(), currentPixel, this.standardDeviation);
                sumA += (angleMapEntry.getValue().getPixelValue() * kernelValue);
                sumB += kernelValue;
                totalNeighbouringPixels++;
            }
        }

        int subtract = sumB == 0 ? 0 : Long.valueOf(Math.round(sumA / sumB)).intValue();
        int filteredPixelValue = currentPixel.getPixelValue() - subtract;
        return filteredPixelValue;
    }

    private int getPixelValueOf(int x, int y){
        try{
            return this.imgInfo.get(x).get(y).getPixelValue();
        } catch(Exception e){
            System.out.println(e.getStackTrace());
            return 0;
        }
    }
}