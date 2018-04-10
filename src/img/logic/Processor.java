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

    private int standardDeviation;
    private List<List<Pixel>> imgInfo;

    public void processCorona(Request request){
        ImageHelper imageHelper = new ImageHelper();
        Image image = new Image();
        this.imgInfo = imageHelper.getPixelsArrayFromImage(request, image);

        CoordinateUtil.updateSolarCenter(image);
        this.standardDeviation = request.getStandardDeviation();

//        int filteredValue = 0;
//        for(int i = 0; i < this.imgInfo.size(); i++){
//            for(int j = 0; j < this.imgInfo.get(i).size(); j++){
//                filteredValue = findNeighbouringPixelsAndApplyFilter(this.imgInfo.get(i).get(j));
//                pixelValues.add(filteredValue < 0 ? 0 : filteredValue);
//            }
//            filteredPixelValues.add(pixelValues);
//            pixelValues = new ArrayList<>();
//            System.out.println("End of row #" + i);
//        }


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
                 System.out.println("Working on ("+pixel.getX()+", "+pixel.getY()+")");
                int filteredValue = findNeighbouringPixels(pixel, radAnglePixelMap);
                return filteredValue < 0 ? 0 : filteredValue;
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());


//        filteredValue = findNeighbouringPixelsAndApplyFilter(this.imgInfo.get(20).get(20));
//        pixelValues.add(filteredValue < 0 ? 0 : filteredValue);
//        pixelValues.add(filteredValue);

        int[][] normalizedIntArray = CoordinateUtil.convertBackToIntArray(FilterUtil.normalize(filteredPixelValues));
        imageHelper.writeToTiff(normalizedIntArray, image.getWidth(), image.getHeight());
    }

    public void achf(List<List<Pixel>> filteredPixelValues){
        filteredPixelValues.stream().forEach(pixelColumn -> {
            pixelColumn.stream().forEach(rowPixel -> {
                CoordinateUtil.updatePolarCoordinates(rowPixel);
                int filteredValue = findNeighbouringPixelsAndApplyFilter(rowPixel);
                System.out.println(filteredValue);
            });
        });
    }

    private int filterPixel(Pixel pixel){
        this.standardDeviation = 1;
        CoordinateUtil.updatePolarCoordinates(pixel);

        if(pixel.getQuadrant() == -1)
            return 0;



        double radiusRangeLower = (pixel.getRadius() - 2) * this.standardDeviation;
        double radiusRangeUpper = (pixel.getRadius() + 2) * this.standardDeviation;

        double degreeRangeLower = (pixel.getAngle() - ((2 * this.standardDeviation) / pixel.getRadius()));
        double degreeRangeUpper = (pixel.getAngle() + ((2 * this.standardDeviation) / pixel.getRadius()));

        double pRadius = radiusRangeLower;
        double pAngle = degreeRangeLower;

        double sumA = 0, sumB = 0;
        NeighbourPixel neighbourPixel= null;
        while(pRadius < radiusRangeUpper){
            while(pAngle < degreeRangeUpper){
                neighbourPixel = new NeighbourPixel();
                neighbourPixel.setRadius(pRadius);
                neighbourPixel.setAngle(pAngle);
                CoordinateUtil.updateCartessianCoordinates(neighbourPixel);
                CoordinateUtil.adjustOffset(neighbourPixel);
                neighbourPixel.setPixelValue(getPixelValueOf(neighbourPixel.getX(), neighbourPixel.getY()));

               double kernelValue = FilterUtil.calculateKernel(neighbourPixel, pixel, this.standardDeviation);
                sumA += (neighbourPixel.getPixelValue() * kernelValue);
                sumB += kernelValue;
                pAngle++;
            }
            pRadius++;
        }

        int filteredPixelValue = sumB == 0 ? 0 : Long.valueOf(pixel.getPixelValue() - Math.round(sumA / sumB)).intValue();
        return filteredPixelValue;
    }

    private int findNeighbouringPixelsAndApplyFilter(Pixel currentPixel){
        this.standardDeviation = 1;
        CoordinateUtil.updatePolarCoordinates(currentPixel);

        double radiusRangeLower = (currentPixel.getRadius() - 2) * this.standardDeviation;
        double radiusRangeUpper = (currentPixel.getRadius() + 2) * this.standardDeviation;

        double lowerAngleRange = (currentPixel.getAngle() - ((2 * this.standardDeviation) / currentPixel.getRadius()));
        double upperAngleRange = (currentPixel.getAngle() + ((2 * this.standardDeviation) / currentPixel.getRadius()));

        Pixel neighbourPixel = null;
        double sumA = 0; double sumB = 0;

        int totalNeighbouringPixels = 0;
        for(int i = 0; i < this.imgInfo.size(); i++){
            for(int j = 0; j < this.imgInfo.get(i).size(); j++){
                neighbourPixel = this.imgInfo.get(i).get(j);
                CoordinateUtil.updatePolarCoordinates(neighbourPixel);
                if(neighbourPixel.getRadius() > radiusRangeLower && neighbourPixel.getRadius() < radiusRangeUpper
                        && (neighbourPixel.getAngle() > lowerAngleRange && neighbourPixel.getAngle() < upperAngleRange)){
                    double kernelValue = FilterUtil.calculateKernel(neighbourPixel, currentPixel, this.standardDeviation);
                    sumA += (neighbourPixel.getPixelValue() * kernelValue);
                    sumB += kernelValue;
                    totalNeighbouringPixels++;
                }
            }
        }
//        System.out.println("Found "+ totalNeighbouringPixels +" neighbouring pixels");
        int filteredPixelValue = sumB == 0 ? 0 : Long.valueOf(currentPixel.getPixelValue() - Math.round(sumA / sumB)).intValue();

        return filteredPixelValue;
    }


    private int findNeighbouringPixels(Pixel currentPixel, TreeMap<Double, TreeMap<Double, Pixel>> radAnglePixelMap){
        this.standardDeviation = 1;
//        CoordinateUtil.updatePolarCoordinates(currentPixel);

        double radiusRangeLower = (currentPixel.getRoundedRadius() - 2) * this.standardDeviation;
        double radiusRangeUpper = (currentPixel.getRoundedRadius() + 2) * this.standardDeviation;

        double lowerAngleRange = (currentPixel.getRoundedAngle() - ((2 * this.standardDeviation) / currentPixel.getRoundedRadius()));
        double upperAngleRange = (currentPixel.getRoundedAngle() + ((2 * this.standardDeviation) / currentPixel.getRoundedRadius()));

        Pixel neighbourPixel = null;
        double sumA = 0; double sumB = 0;

        int totalNeighbouringPixels = 0;
        SortedMap<Double,TreeMap<Double, Pixel>> subMapEntry = radAnglePixelMap.subMap(radiusRangeLower, radiusRangeUpper);
        for (Map.Entry<Double, TreeMap<Double, Pixel>> angleMap : subMapEntry.entrySet()) {
//            double radius = angleMap.getKey();
            SortedMap<Double, Pixel> angleSubMap = angleMap.getValue().subMap(lowerAngleRange, upperAngleRange);
            for (Map.Entry<Double, Pixel> angleMapEntry : angleSubMap.entrySet()) {
                double kernelValue = FilterUtil.calculateKernel(angleMapEntry.getValue(), currentPixel, this.standardDeviation);
                sumA += (angleMapEntry.getValue().getPixelValue() * kernelValue);
                sumB += kernelValue;
                totalNeighbouringPixels++;
            }
        }


       /* totalNeighbouringPixels = 0;
        for(int i = 0; i < this.imgInfo.size(); i++){
            for(int j = 0; j < this.imgInfo.get(i).size(); j++){
                neighbourPixel = this.imgInfo.get(i).get(j);
                CoordinateUtil.updatePolarCoordinates(neighbourPixel);
                if(neighbourPixel.getRadius() > radiusRangeLower && neighbourPixel.getRadius() < radiusRangeUpper
                        && (neighbourPixel.getAngle() > lowerAngleRange && neighbourPixel.getAngle() < upperAngleRange)){
                    double kernelValue = FilterUtil.calculateKernel(neighbourPixel, currentPixel, this.standardDeviation);
                    sumA += (neighbourPixel.getPixelValue() * kernelValue);
                    sumB += kernelValue;
                    totalNeighbouringPixels++;
                }
            }
        }*/
//        System.out.println("Found "+ totalNeighbouringPixels +" neighbouring pixels");
        int filteredPixelValue = sumB == 0 ? 0 : Long.valueOf(currentPixel.getPixelValue() - Math.round(sumA / sumB)).intValue();

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