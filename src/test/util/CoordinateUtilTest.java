package test.util;

import img.common.SolarCenter;
import img.dto.Pixel;
import img.utils.CoordinateUtil;

public class CoordinateUtilTest {

    public static void main(String[] args){
        Pixel pixel = new Pixel();
        SolarCenter.solarCenterX = 66;
        SolarCenter.solarCenterY = 66;

        pixel.setX(75);
        pixel.setY(70);

        CoordinateUtil.updatePolarCoordinates(pixel, 1);

        System.out.println("X Offset: " + pixel.getxOffset());
        System.out.println("Y Offset: " + pixel.getyOffset());
        System.out.println("Radius: " + pixel.getRadius());
        System.out.println("Angle: " + pixel.getAngle());
        System.out.println("Quadrant: " + pixel.getQuadrant());
    }
}
