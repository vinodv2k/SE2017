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
    private double sqSd;
    private List<List<Pixel>> imgInfo;
    public void processCorona(Request request){
        ImageHelper imageHelper = new ImageHelper();
        Image image = new Image();
        this.imgInfo = imageHelper.getPixelsArrayFromImage(request, image);

        CoordinateUtil.updateSolarCenter(image);
        this.standardDeviation = request.getStandardDeviation();
        this.sqSd = request.sqSd();

        TreeMap<Double, List<Double>> radiansToAngleMap = new TreeMap<>();
        TreeMap<Double, TreeMap<Double, Pixel>> radAnglePixelMap = new TreeMap<>();

//        Pixel currentPixel = null;

        this.imgInfo.stream().forEach(row -> {
            row.stream().forEach( pixel -> {
                CoordinateUtil.updatePolarCoordinates(pixel, this.standardDeviation);

                if (radiansToAngleMap.get(pixel.getRoundedRadius()) != null && radiansToAngleMap.get(pixel.getRoundedRadius()).size() > 0){
                    radiansToAngleMap.get(pixel.getRoundedRadius()).add(pixel.getRoundedAngle());
                } else {
                    final List<Double> angles = new ArrayList<>();
                    angles.add(pixel.getRoundedAngle());
                    radiansToAngleMap.put(pixel.getRoundedRadius(), angles);
                }

                if(radAnglePixelMap.get(pixel.getRoundedRadius()) == null || radAnglePixelMap.get(pixel.getRoundedRadius()).size() == 0){
                    // add new treemap
                    TreeMap<Double, Pixel> anglePixelMap = new TreeMap<Double, Pixel>();
                    anglePixelMap.put(pixel.getRoundedAngle(), pixel);
                    radAnglePixelMap.put(pixel.getRoundedRadius(), anglePixelMap);
                } else {
                    radAnglePixelMap.get(pixel.getRoundedRadius()).put(pixel.getRoundedAngle(), pixel);
                }
            });
        });


        /*for(int i = 0; i < this.imgInfo.size(); i++){
            for(int j = 0; j < this.imgInfo.get(i).size(); j++){
                currentPixel = this.imgInfo.get(i).get(j);
                CoordinateUtil.updatePolarCoordinates(currentPixel);

                if (radiansToAngleMap.get(currentPixel.getRoundedRadius()) != null && radiansToAngleMap.get(currentPixel.getRoundedRadius()).size() > 0){
                    radiansToAngleMap.get(currentPixel.getRoundedRadius()).add(currentPixel.getRoundedAngle());
                } else {
                    angles[0] = new ArrayList<>();
                    angles[0].add(currentPixel.getRoundedAngle());
                    radiansToAngleMap.put(currentPixel.getRoundedRadius(), angles[0]);
                }

                if(radAnglePixelMap.get(currentPixel.getRoundedRadius()) == null || radAnglePixelMap.get(currentPixel.getRoundedRadius()).size() == 0){
                    // add new treemap
                    anglePixelMap[0] = new TreeMap<>();
                    anglePixelMap[0].put(currentPixel.getRoundedAngle(), currentPixel);
                    radAnglePixelMap.put(currentPixel.getRoundedRadius(), anglePixelMap[0]);
                } else {
                    radAnglePixelMap.get(currentPixel.getRoundedRadius()).put(currentPixel.getRoundedAngle(), currentPixel);
                }
            }
        }*/

        System.out.println("Polar Coordinates updated and mapped with every pixel value");
        List<List<Integer>> filteredPixelValues = this.imgInfo.stream().map(colPixels -> {
             return colPixels.stream()
                     .map(pixel -> {
//                 System.out.println("Working on ("+pixel.getX()+", "+pixel.getY()+")");
                int filteredValue = findNeighbouringPixels(pixel, radAnglePixelMap);
//                return filteredValue < 0 ? 0 : filteredValue;
                return filteredValue;
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());

        System.out.println("ACHF complete");

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
        final double[] sumA = {0};
        final double[] sumB = {0};
       SortedMap<Double,TreeMap<Double, Pixel>> subMapEntry = radAnglePixelMap.subMap(currentPixel.getLowerRadius(), currentPixel.getUpperRadius());
         subMapEntry.values().stream().forEach(angleMap -> {
            angleMap.subMap(currentPixel.getLowerAngle(), currentPixel.getUpperAngle()).values().stream().forEach(np -> {
                double kernelValue = FilterUtil.calculateKernel(np, currentPixel, this.sqSd);
                sumA[0] += (kernelValue * np.getPixelValue());
                sumB[0] += kernelValue;
            });
        });
/*        for (Map.Entry<Double, TreeMap<Double, Pixel>> angleMap : subMapEntry.entrySet()) {
//            double radius = angleMap.getKey();
            SortedMap<Double, Pixel> angleSubMap = angleMap.getValue().subMap(currentPixel.getLowerAngle(), currentPixel.getUpperAngle());
            for (Map.Entry<Double, Pixel> angleMapEntry : angleSubMap.entrySet()) {
                double kernelValue = FilterUtil.calculateKernel(angleMapEntry.getValue(), currentPixel, this.sqSd);
                sumA[0] += (angleMapEntry.getValue().getPixelValue() * kernelValue);
                sumB[0] += kernelValue;
            }
        }*/

        int subtract = sumB[0] == 0 ? 0 : Long.valueOf(Math.round(sumA[0] / sumB[0])).intValue();
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