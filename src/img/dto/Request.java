package img.dto;

public class Request {
    private double standardDeviation;
    private String filePath;
    private double squareSd;

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
        updateSquareSd();
    }

    private void updateSquareSd(){
        this.squareSd = this.standardDeviation * this.standardDeviation;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public double sqSd() {
        return this.squareSd;
    }
}
