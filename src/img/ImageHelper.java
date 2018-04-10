package img;

import img.dto.Image;
import img.dto.Pixel;
import img.dto.Request;

import javax.media.jai.*;
import java.awt.image.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ImageHelper {

    public List<List<Pixel>> getPixelsArrayFromImage(Request request, Image image){
        RenderedOp op = JAI.create("fileload", request.getFilePath());
        image.setWidth(op.getWidth());
        image.setHeight(op.getHeight());

        final short[] pixels = ((DataBufferUShort) op.getData().getDataBuffer()).getData();

        int[][] data = new int[op.getWidth()][op.getHeight()];

        List<List<Pixel>> pixelsArray = new ArrayList<>();
        List<Pixel> columnPixels = new ArrayList<>();

        int col = 0;
        int row = 0;
        int blockSize = op.getWidth();
        Pixel pixel = null;

        for (int i = 0; i < pixels.length; i=i+1) {
            pixel = new Pixel();
            pixel.setPixelValue(pixels[i]);
            pixel.setX(row);
            pixel.setY(col);
            data[row][col] = pixels[i];
            columnPixels.add(pixel);

            System.out.println("("+row+","+col+") : " + pixels[i]);
            row++;
            if (row == blockSize) {
                col++;
                pixelsArray.add(columnPixels);
                columnPixels = new ArrayList<>();
                row = 0;
            }
        }
        System.out.println(data);

        return pixelsArray;
    }

    public void writeToTiff(int[][] pixels, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster wr = bi.getRaster();

        for (int h = 0; h < width; h++){

                wr.setPixels(0, h, width, 1, pixels[h]); // writing one row at a time. Hence h is 1
        }

        SampleModel sampleModel =
                RasterFactory.createBandedSampleModel(DataBuffer.TYPE_USHORT,  width,height,1);
        // Create a compatible ColorModel.
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        TiledImage tiledImage = new TiledImage(0,0,width,height,0,0,
                sampleModel,colorModel);
        // Set the data of the tiled image to be the raster.
        tiledImage.setData(wr);
        // Save the image on a file.
        JAI.create("filestore",tiledImage,"floatpattern.tif","TIFF");

        System.out.println(wr);
    }
}
