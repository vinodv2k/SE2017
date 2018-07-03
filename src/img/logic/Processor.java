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

        Pixel currentPixel = null;

        this.imagePixels = new ArrayList<>();
        for (int i = 0; i < this.imgInfo.size(); i++) {
            for (int j = 0; j < this.imgInfo.get(i).size(); j++) {
                currentPixel = this.imgInfo.get(i).get(j);
                CoordinateUtil.updatePolarCoordinates(currentPixel);
                this.imagePixels.add(currentPixel);
            }
        }

        List<List<Integer>> filteredPixelValues = this.imgInfo.stream().map(colPixels ->
            colPixels.stream()
                .map(pixel -> applyAchf(pixel))
                .collect(Collectors.toList()))
            .collect(Collectors.toList());

//        printFilteredValues(filteredPixelValues);
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

    private void printFilteredValues(List<List<Integer>> filteredPixelValues){
        filteredPixelValues.stream().forEach(row -> {
            row.stream().forEach(col -> {
                System.out.printf(" " + col.toString());
            });
            System.out.println();
        });
    }

    private int applyAchf(Pixel currentPixel) {
/*        if (currentPixel.getRadius() <= 30){
            return 0;
        }*/
        double doubleSd = 2 * this.standardDeviation;
        double radiusRangeLower = (currentPixel.getRadius()) - 1;
        double radiusRangeUpper = (currentPixel.getRadius()) + 1;

        double lowerAngleRange = currentPixel.getAngle() - (doubleSd / currentPixel
            .getRadius());
        double upperAngleRange = currentPixel.getAngle() + (doubleSd / currentPixel
            .getRadius());

        double sumA = 0;
        double sumB = 0;

        /*System.out.println(lowerAngleRange+","+upperAngleRange+" for "+currentPixel.getxOffset()+"," +
            ""+currentPixel.getyOffset());*/

        if (lowerAngleRange < DegreeConstants.RADIANS_0){
            double adjustmentOffset = DegreeConstants.RADIANS_360 + lowerAngleRange;
            List<Pixel> neighbourPixelsForAxis = this.imagePixels.stream()
                .filter(pix -> pix.getRadius() > radiusRangeLower && pix.getRadius() < radiusRangeUpper)
                .filter( pix ->
                pix.getAngle() > adjustmentOffset && pix.getAngle() < DegreeConstants.RADIANS_360)
                .collect(Collectors.toList());
            for (Pixel pix: neighbourPixelsForAxis) {
                double kernelValue = FilterUtil.calculateKernel(pix, currentPixel, this.standardDeviation);
                sumA += (pix.getPixelValue() * kernelValue);
                sumB += kernelValue;
            }
            lowerAngleRange = DegreeConstants.RADIANS_0;
        }

        if (upperAngleRange > DegreeConstants.RADIANS_360){
            double adjustmentOffset = upperAngleRange - DegreeConstants.RADIANS_360;
            List<Pixel> neighbourPixelsForAxis = this.imagePixels.stream()
                .filter(pix -> pix.getRadius() > radiusRangeLower && pix.getRadius() < radiusRangeUpper)
                .filter( pix -> pix.getAngle() < adjustmentOffset && pix.getAngle() > DegreeConstants.RADIANS_0)
                .collect(Collectors.toList());
            for (Pixel nPix: neighbourPixelsForAxis) {
                double kernelValue = FilterUtil.calculateKernel(nPix, currentPixel, this.standardDeviation);
                sumA += (nPix.getPixelValue() * kernelValue);
                sumB += kernelValue;
            }
            upperAngleRange = DegreeConstants.RADIANS_360;
        }

        double finalUpperAngleRange = upperAngleRange;
        double finalLowerAngleRange = lowerAngleRange;
        List<Pixel> neighbourPixelsForAxis = this.imagePixels.stream()
            .filter(pix -> pix.getRadius() > radiusRangeLower && pix.getRadius() < radiusRangeUpper)
            .filter( pix ->
                pix.getAngle() < finalUpperAngleRange && pix.getAngle() > finalLowerAngleRange
            ).collect(Collectors.toList());

        for (Pixel pix: neighbourPixelsForAxis) {
            if(pix.getRadius() < 30){
                return 0;
            }
            double kernelValue = FilterUtil.calculateKernel(pix, currentPixel, this.standardDeviation);
            sumA += (pix.getPixelValue() * kernelValue);
            sumB += kernelValue;
        }

        int subtract = sumB == 0 ? 0 : Long.valueOf(Math.round(sumA / sumB)).intValue();
        int filteredPixelValue = currentPixel.getPixelValue() - subtract;
        return filteredPixelValue;
    }
}