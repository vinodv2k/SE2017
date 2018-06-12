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

        double angleInRadians = findPolarAngle(pixel.getxOffset(), pixel.getyOffset(), pixel.getQuadrant());
        setAngleAndRoundedAngle(pixel, angleInRadians);
//        updateDegrees(pixel);
    }

    public static void updateSolarCenter(Image image){
        SolarCenter.setSolarCenterX(image.getWidth() / 2);
        SolarCenter.setSolarCenterY(image.getHeight() / 2);
        System.out.println("Solar Center is "+ SolarCenter.getSolarCenterX() + "," + SolarCenter.getSolarCenterY());
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
/*        if( x == x0 || y == y0 ){
            return -1;
        }*/

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
        pixel.setxOffset(pixel.getX() - SolarCenter.getSolarCenterX());
        pixel.setyOffset(SolarCenter.getSolarCenterY() - pixel.getY());
        return pixel;
    }

    private static void updateRadius(Pixel pixel) {
        pixel.setRadius(Math.sqrt(Math.pow(pixel.getxOffset(), 2) + Math.pow(pixel.getyOffset(), 2)));
        pixel.setRoundedRadius(round(pixel.getRadius(), 2));
    }

    public static double round(double value, int places) {
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
        double radians = 0;

        double degree = Math.atan(Math.abs(y) / Math.abs(x));

        switch (q){
            case 1: radians = degree;
                break;
            case 2: radians = DegreeConstants.RADIANS_90 + degree;
                break;
            case 3: radians =  DegreeConstants.RADIANS_180 + degree;
                break;
            case 4: radians =  DegreeConstants.RADIANS_270 + degree;
                break;
        }

        if(x == 0 && y == 0){
            radians = 0;
            pixel.setAngleCooefficient(0);
        }

        if (x == 0){
            if (y < 0){
                setAngleAndRoundedAngle(pixel, DegreeConstants.RADIANS_270);
                return;
            } else {
                setAngleAndRoundedAngle(pixel, DegreeConstants.RADIANS_90);
                return;
            }
        }

        if (y == 0){
            if (x > 0) {
                setAngleAndRoundedAngle(pixel, 0);
//                System.out.println(x + "," + y);
                return;
            } else {
                setAngleAndRoundedAngle(pixel, DegreeConstants.RADIANS_180);
//                System.out.println(x + "," + y);
                return;
            }
        }
        setAngleAndRoundedAngle(pixel, radians);
    }

    public static double findPolarAngle(float x, float y, int q) {
        if(x == 0 && y == 0){
            return 0;
        }

        if (x == 0){
            return (y < 0) ? DegreeConstants.RADIANS_270 : DegreeConstants.RADIANS_90;
        }

        if (y == 0){
            return (x < 0) ? DegreeConstants.RADIANS_180 : DegreeConstants.RADIANS_0;
        }

        double angle = Math.atan(Math.abs(y) / Math.abs(x));

        switch (q){
            case 1: return angle;

            case 2: return DegreeConstants.RADIANS_90 + angle;

            case 3: return DegreeConstants.RADIANS_180 + angle;

            case 4: return DegreeConstants.RADIANS_270 + angle;
        }

        System.out.println("Default .. Shouldn't see this line at all");
        return 0;
    }

    public static void setAngleAndRoundedAngle(Pixel pixel, double degrees){
        pixel.setAngle(degrees);
        pixel.setRoundedAngle(round(pixel.getAngle(), 2));
    }

    public static double angleAddition(double a, double b){
        if (a*b == 1) {
            return 0;
        }
        return Math.atan((a + b) / (1 - (a * b)));
    }

    public static double angleDifference(double a, double b){
//        a = Math.abs(a); b = Math.abs(b);
        if (a*b == -1) {
            System.out.println("Returns zero");
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

    public static int[][] convertPixelsToIntArray(List<List<Pixel>> pixels){
        int[][] pixelValues = new int[pixels.size()][pixels.get(0).size()];

        for (int i = 0; i < pixels.size(); i++){
            for (int j = 0; j < pixels.get(i).size(); j++){
                pixelValues[i][j] = pixels.get(i).get(j).getNormalizedValue();
            }
        }

        return pixelValues;
    }
}