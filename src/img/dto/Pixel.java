package img.dto;

public class Pixel {
    private int x;
    private int y;
    private double radius;
    private double angle;
    private int xOffset;
    private int yOffset;
    private int pixelValue;
    private int quadrant;
    private double roundedRadius;
    private double roundedAngle;
    private double angleCooefficient;
    private double degrees;

    public double getDegrees() {
        return degrees;
    }

    public void setDegrees(double degrees) {
        this.degrees = degrees;
    }

    public double getAngleCooefficient() {
        return angleCooefficient;
    }

    public void setAngleCooefficient(double angleCooefficient) {
        this.angleCooefficient = angleCooefficient;
    }

    public double getRoundedRadius() {
        return roundedRadius;
    }

    public void setRoundedRadius(double roundedRadius) {
        this.roundedRadius = roundedRadius;
    }

    public double getRoundedAngle() {
        return roundedAngle;
    }

    public void setRoundedAngle(double roundedAngle) {
        this.roundedAngle = roundedAngle;
    }

    public int getPixelValue() {
        return pixelValue;
    }

    public void setPixelValue(int pixelValue) {
        this.pixelValue = pixelValue;
    }

    public int getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(int quadrant) {
        this.quadrant = quadrant;
    }

    public int getxOffset() {
        return xOffset;
    }

    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }
}
