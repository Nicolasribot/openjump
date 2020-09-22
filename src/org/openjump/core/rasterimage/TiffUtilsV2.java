package org.openjump.core.rasterimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.imagery.ReferencedImageException;
import com.vividsolutions.jump.workbench.imagery.geoimg.GeoReferencedRaster;

public class TiffUtilsV2 {
  // a File -> RenderedOp cache mapping to prevent recreating inputs for the same file
  private static HashMap<File,GeoReferencedRaster> geoRasterCache = new HashMap<File,GeoReferencedRaster>();

  
  public static RenderedOp getRenderedOp(File tiffFile) throws IOException {
    GeoReferencedRaster geoRaster = getGeoReferencedRaster(tiffFile);
    RenderedOp rop;
    try {
      rop = geoRaster.getImage();
    } catch (ReferencedImageException e) {
      // TODO: handle errors better, wrapping it in IOException here
      //       because that's what's handled up from here
      throw new IOException(e);
    }
    return rop;
  }

  public static Envelope getEnvelope(File tiffFile) throws IOException {
    GeoReferencedRaster geoRaster = getGeoReferencedRaster(tiffFile);
    
    return geoRaster.getOriginalEnvelope();
  }

  private static GeoReferencedRaster getGeoReferencedRaster(File tiffFile) throws IOException {
    // prevent recreating inputs by reusing cached RenderedOp
    if (geoRasterCache.containsKey(tiffFile))
      return geoRasterCache.get(tiffFile);

    GeoReferencedRaster geoRaster;
    try {
      geoRaster = new GeoReferencedRaster(tiffFile.toString(),
          new com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi());
    } catch (ReferencedImageException e) {
      // TODO: handle errors better, wrapping it in IOException here
      //       because that's what's handled up from here
      throw new IOException(e);
    }

    // save in cache
    geoRasterCache.put(tiffFile, geoRaster);

    return geoRaster;
  }

  public static RenderedOp readSubsampled(File tiffFile, float xScale, float yScale) throws IOException {
    RenderedOp renderedOp = getRenderedOp(tiffFile);
    ParameterBlock parameterBlock = new ParameterBlock();
    parameterBlock.addSource(renderedOp);
    parameterBlock.add(xScale);
    parameterBlock.add(yScale);
    renderedOp = JAI.create("scale", parameterBlock);
    return JAI.create("scale", parameterBlock);
  }
  
  /**
   * Method to buid an ImageAndMetadata file
   * An ImageAndMetadata groups the Envelope, the Image, the Statistics and 
   * NoData value of a TIF file
   * @param tiffFile
   * @param viewportEnvelope
   * @param requestedRes
   * @param overviews
   * @param stats
   * @return ImageAndMetadata
   * @throws NoninvertibleTransformException
   * @throws IOException
   * @throws FileNotFoundException
   * @throws TiffTags.TiffReadingException
   * @throws Exception
   */
  public static ImageAndMetadata readImage(File tiffFile, Envelope viewportEnvelope, Resolution requestedRes,
	      Overviews overviews, Stats stats) throws NoninvertibleTransformException, IOException, FileNotFoundException,
	      TiffTags.TiffReadingException, Exception {

	    // Try to read geotiff tags
	    TiffTags.TiffMetadata tiffMetadata = TiffTags.readMetadata(tiffFile);
	    int originalImageWidth = tiffMetadata.getColsCount();
	    int originalImageHeight = tiffMetadata.getRowsCount();
	    Resolution cellSize = tiffMetadata.getResolution();
	    Double noData = tiffMetadata.getNoData();

	    // Now try with tfw
	 /*   if (cellSize == null) {
	      WorldFileHandler worldFileHandler = new WorldFileHandler(tiffFile.getAbsolutePath(), true);
	      Envelope envelope = worldFileHandler.readWorldFile(originalImageWidth, originalImageHeight);
	      cellSize = new Resolution(envelope.getWidth() / originalImageWidth, envelope.getHeight() / originalImageHeight);
	    }*/

	    Envelope wholeImageEnvelope = getEnvelope(tiffFile);
	    		
	    		//RasterImageIO.getGeoReferencing(tiffFile.getAbsolutePath(), true,
	       // new Point(originalImageWidth, originalImageHeight));

	    if (requestedRes == null) {
	      requestedRes = cellSize;
	    }

	    int overviewLevel = overviews.pickOverviewLevel(requestedRes);

	    if (stats == null) {
	      // Statistics on all pixels
	      stats = calculateStats(tiffFile, noData, tiffFile);
	    }

	    if (overviews.getOverviewsCount() == 1) {

	      // No overviews, decimation (subsampling)

	      float xScale = (float) (cellSize.getX() / requestedRes.getX());
	      float yScale = (float) (cellSize.getY() / requestedRes.getY());
	      xScale = Math.min(xScale, 1);
	      yScale = Math.min(yScale, 1);

	      RenderedOp renderedOp = readSubsampled(tiffFile, xScale, yScale);

	      // For better looking results, but slower:
	      // rop = JAI.create("SubsampleAverage", pb);

	      Resolution subsetResolution = new Resolution(wholeImageEnvelope.getWidth() / renderedOp.getWidth(),
	          wholeImageEnvelope.getHeight() / renderedOp.getHeight());

	      Rectangle imageSubset = RasterImageIO.getDrawingRectangle(renderedOp.getWidth(), renderedOp.getHeight(),
	          wholeImageEnvelope, viewportEnvelope, subsetResolution);

	      BufferedImage bufferedImage;
	      Envelope imagePartEnvelope;
	      int actualImageWidth;
	      int actualImageHeight;
	      if (imageSubset == null) {
	        bufferedImage = null;
	        imagePartEnvelope = null;
	        actualImageWidth = 0;
	        actualImageHeight = 0;
	      } else {
	        bufferedImage = renderedOp.getAsBufferedImage(imageSubset, null);
	        imagePartEnvelope =  getImageSubsetEnvelope(wholeImageEnvelope, imageSubset, subsetResolution);
	        actualImageWidth = bufferedImage.getWidth();
	        actualImageHeight = bufferedImage.getHeight();
	      }

	      Metadata metadata = new Metadata(wholeImageEnvelope, imagePartEnvelope,
	          new Point(originalImageWidth, originalImageHeight), new Point(actualImageWidth, actualImageHeight),
	          (cellSize.getX() + cellSize.getY()) / 2, (subsetResolution.getX() + subsetResolution.getY()) / 2, noData,
	          stats);
	      return new ImageAndMetadata(bufferedImage, metadata);

	    } else {

	      // Read from internal overviews
	      ImageAndMetadata imageAndMetadata = readImage(tiffFile, overviewLevel, 0,
	          new Point(originalImageWidth, originalImageHeight), cellSize, wholeImageEnvelope, viewportEnvelope, noData,
	          stats);

	      // Read from external overviews
	      if (imageAndMetadata == null) {
	        File ovrFile = new File(tiffFile.getParent(), tiffFile.getName() + ".ovr");
	        imageAndMetadata = readImage(ovrFile, overviewLevel, overviews.getInternalOverviewsCount(),
	            new Point(originalImageWidth, originalImageHeight), cellSize, wholeImageEnvelope, viewportEnvelope, noData,
	            stats);
	      }
	      return imageAndMetadata;

	    }

	  }

  /**
   * Method to read overviews of a TIF
   * @param tiffFile
   * @param overviewIndex
   * @param indexStart
   * @param originalSize
   * @param originalCellSize
   * @param wholeImageEnvelope
   * @param viewportEnvelope
   * @param noDataValue
   * @param stats
   * @return
   * @throws IOException
   * @throws NoninvertibleTransformException
   */
	  private static ImageAndMetadata readImage(File tiffFile, int overviewIndex, int indexStart, Point originalSize,
	      Resolution originalCellSize, Envelope wholeImageEnvelope, Envelope viewportEnvelope, double noDataValue,
	      Stats stats) throws IOException, NoninvertibleTransformException {

	    ImageInputStream imageInputStream = ImageIO.createImageInputStream(tiffFile);
	    Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInputStream);

	    if (iterator != null && iterator.hasNext()) {

	      ImageReader imageReader = iterator.next();
	      imageReader.setInput(imageInputStream);
	      for (int i = 0; i < imageReader.getNumImages(true); i++) {
	        if (i + indexStart == overviewIndex) {

	          Resolution subsetResolution = new Resolution(wholeImageEnvelope.getWidth() / imageReader.getWidth(i),
	              wholeImageEnvelope.getHeight() / imageReader.getHeight(i));

	          Rectangle imageSubset = RasterImageIO.getDrawingRectangle(imageReader.getWidth(i), imageReader.getHeight(i),
	              wholeImageEnvelope, viewportEnvelope, subsetResolution);

	          BufferedImage bufferedImage;
	          Envelope imagePartEnvelope;
	          int imageWidth;
	          int imageHeight;
	          if (imageSubset == null) {
	            bufferedImage = null;
	            imagePartEnvelope = null;
	            imageWidth = 0;
	            imageHeight = 0;
	          } else {
	            ImageReadParam imageReadParam = new ImageReadParam();
	            imageReadParam.setSourceRegion(imageSubset);
	            bufferedImage = imageReader.read(i, imageReadParam);
	            imagePartEnvelope =  getImageSubsetEnvelope(wholeImageEnvelope, imageSubset, subsetResolution);
	            imageWidth = bufferedImage.getWidth();
	            imageHeight = bufferedImage.getHeight();
	          }

//	                    double originalCellSize = subsetResolution.getX();
//	                    int cellsCount = imageReader.getWidth(i) * imageReader.getHeight(i);
//	                    int sampleSize = 10000;
//	                    double lowResCellSize = Math.sqrt(Math.max(1, cellsCount/(double)sampleSize)) * originalCellSize;

//	                    if(stats == null) {
//	                        BufferedImage statsBufferedImage = imageReader.read(imageReader.getNumImages(true) - 1, null);
//	                        stats = calculateStats(statsBufferedImage, noDataValue);
//	                    }

	          Metadata metadata = new Metadata(wholeImageEnvelope, imagePartEnvelope, originalSize,
	              new Point(imageWidth, imageHeight), (originalCellSize.getX() + originalCellSize.getY()) / 2,
	              (subsetResolution.getX() + subsetResolution.getY()) / 2, noDataValue, stats);

	          return new ImageAndMetadata(bufferedImage, metadata);

	        }
	      }

	    }

	    return null;

	  }

	  /**
	   * Method to read Statistics of TIF file (if available) on the file itself or
	   * from an external aux.xml file
	   * @param tiffFile
	   * @param noDataValue
	   * @param imageFile
	   * @return
	   * @throws ParserConfigurationException
	   * @throws TransformerException
	   * @throws ImageReadException
	   * @throws IOException
	   * @throws SAXException
	   */
	  private static Stats calculateStats(File tiffFile, double noDataValue, File imageFile)
	      throws ParserConfigurationException, TransformerException, ImageReadException, IOException, SAXException {

	    Stats stats = null;

	    // Look for internal stats tag
	    try {
	      TiffImageParser parser = new TiffImageParser();
	      TiffImageMetadata metadata = (TiffImageMetadata) parser.getMetadata(tiffFile);
	      List<TiffField> tiffFields = metadata.getAllFields();
	      for (TiffField tiffField : tiffFields) {
	        if (tiffField.getTag() == TiffTags.TIFFTAG_GDAL_METADATA) {
	          GDALInternalMetadata gdalParser = new GDALInternalMetadata();
	          stats = gdalParser.readStatistics(tiffField.getStringValue());
	          break;
	        }
	      }
	    } catch (Exception ex) {
	      stats = null;
	    }

	    if (stats != null) {
	      return stats;
	    }

	    // Look for aux.xml file
	    File auxXmlFile = new File(imageFile.getParent(), imageFile.getName() + ".aux.xml");
	    if (auxXmlFile.exists()) {
	      GDALPamDataset gdalPamDataset = new GDALPamDataset();
	      try {
	        stats = gdalPamDataset.readStatistics(auxXmlFile);
	        return stats;
	      } catch (Exception ex) {
	        Logger.error("Failed to read statistics.", ex);
	        return createStatsXml(tiffFile, noDataValue, auxXmlFile);
	      }
	    }
	    return createStatsXml(tiffFile, noDataValue, auxXmlFile);

	  }
      /**
       * Method to compute statistic of a TIF file and write as aux.xml file
       * @param tiffFile
       * @param noDataValue
       * @param auxXmlFile
       * @return
       * @throws ParserConfigurationException
       * @throws TransformerException
       * @throws TransformerConfigurationException
       * @throws SAXException
       * @throws IOException
       */
	  private static Stats createStatsXml(File tiffFile, double noDataValue, File auxXmlFile)
	      throws ParserConfigurationException, TransformerException, TransformerConfigurationException, SAXException,
	      IOException {

	    BufferedImage bufferedImage = readSubsampled(tiffFile, 1, 1).getAsBufferedImage();
	    int bandCount = bufferedImage.getRaster().getNumBands();

	    double minValue[] = new double[bandCount];
	    double maxValue[] = new double[bandCount];
	    double sum[] = new double[bandCount];
	    double sumSquare[] = new double[bandCount];
	    long cellsCount[] = new long[bandCount];

	    for (int b = 0; b < bandCount; b++) {
	      minValue[b] = Double.MAX_VALUE;
	      maxValue[b] = -Double.MAX_VALUE;
	    }

	    for (int r = 0; r < bufferedImage.getHeight(); r++) {
	      Raster raster = bufferedImage.getData(new Rectangle(0, r, bufferedImage.getWidth(), 1));
	      for (int c = 0; c < bufferedImage.getWidth(); c++) {

	        for (int b = 0; b < bandCount; b++) {

	          double value = raster.getSampleDouble(c, r, b);
	          if (value != noDataValue && (float) value != (float) noDataValue && !Double.isNaN(value)
	              && !Double.isInfinite(value)) {
	            if (value < minValue[b])
	              minValue[b] = value;
	            if (value > maxValue[b])
	              maxValue[b] = value;
	            cellsCount[b]++;
	            sum[b] += value;
	            sumSquare[b] += value * value;
	          }

	        }

	      }
	    }

	    Stats stats = new Stats(bandCount);
	    for (int b = 0; b < bandCount; b++) {
	      double meanValue = sum[b] / cellsCount[b];
	      double stdDevValue = Math.sqrt(sumSquare[b] / cellsCount[b] - meanValue * meanValue);
	      stats.setStatsForBand(b, minValue[b], maxValue[b], meanValue, stdDevValue);
	    }

	    // Write aux.xml
	    GDALPamDataset gdalPamDataset = new GDALPamDataset();
	    gdalPamDataset.writeStatistics(auxXmlFile, stats);

	    return stats;

	  }

	  private static Envelope getImageSubsetEnvelope(Envelope wholeImageEnvelope, Rectangle imageSubset,
	      Resolution subsetResolution) {

	    double ulX = Math.max(wholeImageEnvelope.getMinX(),
	        wholeImageEnvelope.getMinX() + imageSubset.getX() * subsetResolution.getX());
	    double ulY = Math.min(wholeImageEnvelope.getMaxY(),
	        wholeImageEnvelope.getMaxY() - imageSubset.getY() * subsetResolution.getY());
	    double lrX = Math.min(wholeImageEnvelope.getMaxX(), wholeImageEnvelope.getMinX()
	        + imageSubset.getX() * subsetResolution.getX() + imageSubset.getWidth() * subsetResolution.getX());
	    double lrY = Math.max(wholeImageEnvelope.getMinY(), wholeImageEnvelope.getMaxY()
	        - imageSubset.getY() * subsetResolution.getY() - imageSubset.getHeight() * subsetResolution.getY());
	    Coordinate ulCoord = new Coordinate(ulX, ulY);
	    Coordinate lrCoord = new Coordinate(lrX, lrY);

	    Envelope imagePartEnvelope = new Envelope(ulCoord, lrCoord);

	    return imagePartEnvelope;

	  }

	  /**
	   * Method to read the value of a cell at defined position on the raster
	   * @param tiffFile
	   * @param col
	   * @param row
	   * @param band
	   * @return
	   * @throws Exception
	   */
	  public static Double readCellValue(File tiffFile, int col, int row, int band)  throws Exception {
	    Rectangle rectangle = new Rectangle(col, row, 1, 1);
	    return getRenderedOp(tiffFile).getData(rectangle).getSampleDouble(col, row, band);
	  }

}
