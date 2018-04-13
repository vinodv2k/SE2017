package img.utils;


import img.common.DegreeConstants;
import img.common.SolarCenter;
import img.dto.Image;
import img.dto.Pixel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CoordinateUtil {

    public static void updatePolarCoordinates(Pixel pixel) {
        pixel.setQuadrant(findQuadrant(pixel.getX(), pixel.getY()));
        updatePixelOffsetFromSolarCenter(pixel);
        updateRadius(pixel);
        updateDegrees(pixel);
    }

    public static void updateSolarCenter(Image image){
        SolarCenter.setSolarCenterX(image.getWidth() / 2);
        SolarCenter.setSolarCenterY(image.getHeight() / 2);
    }

    public static void updateCartessianCoordinates(Pixel pixel) {
        pixel.setxOffset(Double.valueOf(pixel.getRadius() * Math.cos(Math.abs(pixel.getAngle()))).intValue());
        pixel.setyOffset(Double.valueOf(pixel.getRadius() * Math.sin(Math.abs(pixel.getAngle()))).intValue());
    }

    public static void adjustOffset(Pixel pixel){
        pixel.setX(SolarCenter.solarCenterX + pixel.getxOffset());
        pixel.setY(SolarCenter.solarCenterY - pixel.getyOffset());
    }

    private static int findQuadrant(int x, int y) {
        int x0 = SolarCenter.getSolarCenterX();
        int y0 = SolarCenter.getSolarCenterY();
        if( x == x0 || y == y0 ){
            return -1;
        }

        if(x > x0 && y > y0) {
//            System.out.println("IV Quadrant");
            return 4;
        } else if(x > x0 && y < y0){
//            System.out.println("I Quadrant");
            return 1;
        }
        else if(x < x0 && y > y0) {
//            System.out.println("III Quadrant");
            return 3;
        } else if(x < x0 && y < y0){
//            System.out.println("II Quadrant");
            return 2;
        }

        return 0;
    }

    private static Pixel updatePixelOffsetFromSolarCenter(Pixel pixel){
        if (pixel.getQuadrant() == 0) {
            return pixel;
        }
        pixel.setxOffset(pixel.getX() - SolarCenter.getSolarCenterX());
        pixel.setyOffset(SolarCenter.getSolarCenterY() - pixel.getY());
        return pixel;
    }

    private static void updateRadius(Pixel pixel) {
        pixel.setRadius(Math.sqrt(Math.pow(pixel.getxOffset(), 2) + Math.pow(pixel.getyOffset(), 2)));
        pixel.setRoundedRadius(round(pixel.getRadius(), 2));
//        System.out.println("Radius: " + pixel.getRadius());
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        if (Double.isNaN(value)){
            return 0;
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    public static void updateDegrees(Pixel pixel){
        float x = pixel.getxOffset();
        float y = pixel.getyOffset();
        int q = pixel.getQuadrant();
        double degrees = 0;
        if(x == 0 && y == 0){
            degrees = 0;
            pixel.setAngleCooefficient(0);
        }

        if (x == 0){
            if (y < 0){
                degrees = DegreeConstants.RADIANS_270;
            } else {
                degrees =  DegreeConstants.RADIANS_90;
            }
        }

        if (y == 0){
            if (x > 0) {
                degrees =  0;
            } else {
                degrees =  DegreeConstants.RADIANS_180;
            }
        }

        int signFactor = 1;
        if (x == 0 || y == 0){
            pixel.setAngleCooefficient(0);
            degrees =  Math.atan(0);
        } else if((y / x) < 0) {
            pixel.setAngleCooefficient(y/x);
            signFactor = -1;
        }

        switch (q){
                case 1: degrees =  Math.atan(Math.abs(y) / Math.abs(x));
                        break;
                case 2: degrees =  Math.atan(y/x) + (DegreeConstants.RADIANS_180);
                        break;
                case 3: degrees =  ((q-1) * DegreeConstants.RADIANS_90) + ((-1) * Math.atan(y/x));
                        degrees = (-1) * degrees;
                        break;
                default:degrees =  Math.atan(y/x);
                        break;
            }
            /*switch (q){
                case 1: degrees =  Math.atan(y / x);
                case 2: degrees =  signFactor * (Math.atan(x/y) + DegreeConstants.RADIANS_90);
                case 3: degrees =  Math.atan(x/y) + ((q-1) * DegreeConstants.RADIANS_90);
                default:degrees =  Math.atan(y/x) + ((q-1) * DegreeConstants.RADIANS_90);
            }*/

        pixel.setAngle(degrees);
        pixel.setRoundedAngle(round(pixel.getAngle(), 2));
    }

    private double angleAddition(float a, float b){
        return Math.atan((a + b) / (1 - (a * b)));
    }

    public static double angleDifference(double a, double b){
        if (a*b == 1) {
            return 0;
        }
        return Math.atan((a - b) / (1 + (a * b)));
    }

    public static int[][] convertBackToIntArray(List<List<Integer>> pixels){
        int[][] pixelValues = new int[pixels.size()][pixels.get(0).size()];

        for (int i = 0; i < pixels.size(); i++){
            for (int j = 0; j < pixels.get(i).size(); j++){
                pixelValues[i][j] = pixels.get(i).get(j).intValue();
            }
        }

        return pixelValues;
    }
}