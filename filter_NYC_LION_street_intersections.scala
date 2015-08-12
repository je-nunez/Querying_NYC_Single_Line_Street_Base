#!/usr/bin/env scala -deprecation
/* Ask Scala to warn for "-deprecation" because there are methods in
 * GeoTools that are being optimized according to the Open Geospatial
 * Consortium:
 *
 *    http://www.opengeospatial.org/standards/
 *
 * and others whose use is now deprecated, as e.g., constructors in
 *
 *    MapContent
 *
 * http://docs.geotools.org/stable/javadocs/org/geotools/map/MapContent.html
 *
 * and the same therefore holds for GeoScript as well.
 */

import scala.collection.JavaConversions._
import scala.collection.mutable._
import scala.math._
import _root_.java.io.File
import _root_.java.io.IOException
import org.geoscript.geometry.io._
import org.geoscript.feature._
import _root_.org.geoscript.layer._
import org.geoscript.render._
import org.geoscript.viewer.Viewer._
import _root_.java.awt.Color
import _root_.java.awt.Rectangle
import _root_.java.awt.image.BufferedImage
import _root_.javax.imageio._
import com.vividsolutions._
import org.opengis.feature.simple.SimpleFeatureType
import org.geotools.data.directory.DirectoryFeatureLocking
import org.geotools.data.DefaultTransaction
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.data.store._
import org.geotools.factory.CommonFactoryFinder
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.simple.SimpleFeatureTypeImpl
import org.geotools.geometry.jts.ReferencedEnvelope
import org.geotools.map.FeatureLayer
import org.geotools.map.MapContent
// import org.geotools.renderer.shape.ShapefileRenderer
import org.geotools.renderer.lite.StreamingRenderer
import org.geotools.styling.Rule
import org.geotools.styling.Stroke
import org.geotools.styling.Style
import org.geotools.styling.StyleFactory
import org.geotools.util.logging.Logging



/*
 * function: open_a_shapefile
 *
 * Uses GeoScript to open a filename and return the ESRI Shapefile layer.
 *
 * @param shapefile_fname filename of the shapefile (should include ".shp")
 * @return opened shapefile (layer according to GeoScript)
 */

def open_a_shapefile(shapefile_fname: String): Layer =
{
    // Open the filename as a ESRI Shapefile

    val shp = Shapefile(shapefile_fname)

    println("DEBUG: Class of ShpFile: " + shp.getClass)

    println("DEBUG: Number of items in the shapefile: " + shp.count)

    println("DEBUG: Schema: " + shp.schema)

    println("DEBUG: Features: " + shp.features)
    println("DEBUG: Bounds rectangle: " + shp.features.getBounds())

    return shp
}


/*
 * function: create_new_shapefile
 *
 * Uses GeoTools to create a new shapefile given a filename, and returns
 * the ESRI Shapefile layer.
 *
 * @param shp_filename the filename of the new shapefile
 * @param schema the schema to give to the new shapefile
 * @return the new shapefile
 */

def create_new_shapefile(shp_filename: String, schema: SimpleFeatureType)
       : ShapefileDataStore =
{
    val shp_file = new File(shp_filename)
    val factory = new ShapefileDataStoreFactory()

    val params = Map("url" -> shp_file.toURI().toURL())
    // params += (("create spatial index", true))

    val new_ShpFile = factory.createNewDataStore(params).
                                  asInstanceOf[ShapefileDataStore]

    val featureBuilder = new SimpleFeatureBuilder(schema)

    val featureType = featureBuilder.getFeatureType()

    new_ShpFile.createSchema(featureType)

    return new_ShpFile
}


/*
 * function: filter_and_dump_geom_features
 *
 * Uses GeoTools to create a new shapefile given a filename, and returns
 * the ESRI Shapefile layer.
 *
 * @param input_features the input features on which to process
 * @param filtering_geom the filtering geometry to match (null is possible)
 * @param resulting_features the feature collection where to save the
 *                           filtered results (null is possible: don't save)
 */

def filter_and_dump_geom_features(input_features: FeatureCollection,
                                  filtering_geom: jts.geom.Geometry,
                                  resulting_features: DefaultFeatureCollection)
{
    /* TODO: this has to be rewritten to use instead the subCollection(Filter)
     *       method of "input_features: FeatureCollection", instead of the full
     *       for-loop in this initial test version
     */

    for (f <- input_features.toArray()) {

          val feature =
               f.asInstanceOf[org.geotools.feature.simple.SimpleFeatureImpl]

          var print_this_feature: Boolean = true

          // Check if there is a polygonal_constraint (ie., a polygonal filter)
          if (filtering_geom != null) {
              // this feature's geometry intersects this filter?
              val strt_segm: jts.geom.Geometry =
                                    feature.getAttribute("the_geom").
                                               asInstanceOf[jts.geom.Geometry]
              print_this_feature = filtering_geom.intersects(strt_segm)
              if (!print_this_feature) {
                 /*
                  * Try if the constraint is a polygon and if this feature's
                  * geometry is contained in it.
                  * The most general -and optimum- way to handle this is
                  * to use:
                  *      IntersectionMatrix
                  *             filtering_geom.relate(strt_segm)
                  * and see what are the relations between both geometries
                  * http://www.vividsolutions.com/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#relate(com.vividsolutions.jts.geom.Geometry)
                 */
                 if (filtering_geom.isInstanceOf[jts.geom.Polygon]) {
                      print_this_feature =
                                    filtering_geom.contains(strt_segm)
                 }
              }
          }

          if (print_this_feature) {
              // Print all the property names and values of this feature
              for (property <- feature.getProperties) {
                  println(property.getName() + " = " + property.getValue())
                  /*
                   * // Printing this feature property types and constraints
                   * // as well
                   *  println(property.getName() +
                   *          "[" + property.getType() + "] = " +
                   *          property.getValue()
                   *         )
                   */
              }

              // Add this feature to a resulting feature collection (if
              // requested so)
              if (resulting_features != null) {
                  resulting_features.add(feature)
              }
          }
    }

}


/*
 * function: save_features_to_shapefile
 *
 * Saves a collection of features to an ESRI shapefile
 *
 * @param input_features the input features to save
 * @param to_shapefile the shapefile to save those features
 */

def save_features_to_shapefile(input_features: FeatureCollection,
                               to_shapefile: ShapefileDataStore)
{
    val typeName = to_shapefile.getTypeNames()(0)
    val featureSource = to_shapefile.getFeatureSource(typeName)
    if (featureSource.isInstanceOf[SimpleFeatureStore]) {
        val featureStore = featureSource.asInstanceOf[SimpleFeatureStore]
        val transaction = new DefaultTransaction("create")
        featureStore.setTransaction(transaction)
        try {
            featureStore.addFeatures(input_features)
            transaction.commit()
        } catch {
             case e: java.lang.Exception => {
                  System.err.println("Caught an exception: " + e.getMessage)
                  transaction.rollback()
                  transaction.close()
             }
        } finally {
            transaction.close()
        }
    }
}


/*
 * function: createLineStyle
 *
 * Create a simple Style for rendering lines
 *
 * @return the new Style
 */

def createLineStyle(): Style = {

    // See this function here:
    //     http://docs.geotools.org/latest/tutorials/map/style.html

    val styleFactory = CommonFactoryFinder.getStyleFactory(null)
    val filterFactory = CommonFactoryFinder.getFilterFactory(null)

    val stroke = styleFactory.createStroke(
                                filterFactory.literal(Color.BLUE),
                                filterFactory.literal(1)
                       )

    /*
     * Setting the geometryPropertyName arg to null signals that we want to
     * draw the default geomettry of features
     */
    val sym = styleFactory.createLineSymbolizer(stroke, null)

    val rule = styleFactory.createRule()
    rule.symbolizers().add(sym)

    val rules: Array[Rule] = new Array[Rule](1)
    rules(0) = rule

    val fts = styleFactory.createFeatureTypeStyle(rules)
    val style = styleFactory.createStyle()
    style.featureTypeStyles().add(fts)

    return style

    /*
     * To create in GeoScript (not GeoTools directly) a style from a
     * Styled Layer Descriptor (SLD) string:
     *
     * val casings =
     *       Seq(("#FF0000", 1), ("#DD0000", 1), ("#AA0000", 1), ("#770000", 1))
     *
     * // A Styled Layer Descriptor (SLD)
     *
     * val sldXML =
     *     <UserStyle xmlns="http://www.opengis.net/sld">
     *        <FeatureTypeStyle>
     *           <Rule>
     *             { for ((color, width) <- casings) yield
     *                 <LineSymbolizer>
     *                    <Stroke>
     *                       <CssParameter name="stroke">{color}</CssParameter>
     *                 <CssParameter name="stroke-width">{width}</CssParameter>
     *                    </Stroke>
     *                 </LineSymbolizer>
     *             }
     *           </Rule>
     *        </FeatureTypeStyle>
     *     </UserStyle>
     *
     * // convert the Styled Layer Descriptor XML to a GeoScript style
     *
     * val myStyle = org.geoscript.style.io.SLD.read(
     *                  org.geoscript.io.Source.string(sldXML.mkString(" "))
     *             )
     *
     * // About styles in general in GeoScript Scala:
     * // https://github.com/dwins/geoscript.scala/blob/master/geoscript/src/main/sphinx/style.rst
     *
     */

}


/*
 * function: save_map_content_to_jpeg_img
 *
 * Save a MapContent to a JPEG image filename
 * (A version inspired from http://docs.geotools.org/latest/userguide/library/render/gtrenderer.html)
 *
 * @param map the MapContent with the layers
 * @param img_fname the filename where to save the image
 * @param imageWidth the width of the resulting image file
 */

def save_map_content_to_jpeg_img(map: MapContent, img_fname: String,
                                 imageWidth: Integer)
{
    var mapBounds: ReferencedEnvelope = null
    var imageBounds: Rectangle = null

    try {
        mapBounds = map.getMaxBounds()
        val map_heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0)
	val imageHeight = round(imageWidth * map_heightToWidth).toInt
        imageBounds = new Rectangle(0, 0, imageWidth, imageHeight)

    } catch {
         case e: java.lang.Exception => {
              System.err.println("Caught an exception: " + e.getMessage)
              return
         }
    }

    val image = new BufferedImage(imageBounds.width, imageBounds.height,
                                  BufferedImage.TYPE_INT_RGB)

    val gr = image.createGraphics()
    gr.setPaint(Color.WHITE)
    gr.fill(imageBounds)

    val renderer = new StreamingRenderer()
    renderer.setMapContent(map)

    try {
        renderer.paint(gr, imageBounds, mapBounds)
        val dest_img_file = new File(img_fname)
        ImageIO.write(image, "jpeg", dest_img_file)
    } catch {
         case e: IOException => {
              System.err.println("Caught an IO exception: " + e.getMessage)
              return
         }
    }
}


/*
 * function: visualize_shapefile
 *
 * Visualize a shapefile instance.
 *
 * @param shp the ShapefileDataStore instance to visualize
 */

def visualize_shapefile(shp: ShapefileDataStore) {

    val typeName = shp.getTypeNames()(0)
    val featureSource = shp.getFeatureSource(typeName)

    /*
     * We use a "Line" Style as in:
     *
     *     http://docs.geotools.org/latest/tutorials/map/style.html
     *
     * because we know our shapefile is only of MultiLineStrings. This
     * GeoTools URL above has examples how to create styles for other
     * Geometries (Polygons, Points, etc).
     */

    val lnStyle = createLineStyle()
    var envelope = featureSource.getBounds()

    val layer = new FeatureLayer(featureSource, lnStyle)

    val layers: Array[MapLayer] = new Array[MapLayer](1)
    layers(0) = layer

    val map = new MapContent()
    map.addLayer(layer)

    /* This code is throwing a run-time exception -in the ImageIO.write */
    // save_map_content_to_jpeg_img(map, "test.jpg", 1024)

    return;  // The next code in this procedure is being rewritten

    /* LatLon is precisely LatLon = lookupEPSG("EPSG:4326"),
     *    and "EPSG:4326" is "WGS 84", that is the coord-system we converted
     *    so this shapefile is in this coordinate system
     */
    // val viewport = Viewport.pad(reference(layer.envelope,
    //                                       LatLon),
    //                                       frame)

    // val viewport = Viewport.pad(layer.getBounds(), frame)

    /*
     * // Save this layer of the shapefile as an image
     * render(
     *          viewport,
     *          Seq(MapLayer(layer, myStyle))
     *       ) on PNG(Sink.file("image.png"), frame)
     *   // ) on JPEG(Sink.file("image.jpg"), rectangle)
     */

    // Try to display this layer

    // display(Seq(MapLayer(layer, myStyle)))
}


/*
 * function: dump_nyc_lion_street_db
 *
 * Dump the New York City's Department of City Planning LION Single Line
 * Street database (it must have been imported into an ESRI Shapefile
 * first: see other ETL task in this task that downloads the LION URL)
 *
 * @param location_LION_shapefile filename with the NYC LION Street Shapefile
 * @param filter_polygonal_area a WKT geometry string to which to filter the
 *                              LION street segments if matching this WKT geom
 *                              (null is possible: do not filter)
 * @param save_to_shpfile filename of the ESRI Shapefile where to save the
 *                        resulting LION street segments by filter match above
 *                        (null is possible: do not save the filtered streets)
 */

def dump_nyc_lion_street_db(location_LION_shapefile: String,
                            filter_polygonal_area: String,
                            save_to_shpfile: String) {

    val lion_shp = open_a_shapefile(location_LION_shapefile)

    /* Dump the fields imported into the Shapefile from the NYC intersections
     * in the LION Street db, like:
     *    the_geom = MULTILINESTRING ((-73.90478740690548 40.87892363753028,
     *                                 -73.90442743514292 40.87943261859585))
     *    segmentid = 0079707
     *    segmenttyp = U
     *    segcount = 1
     *    XFrom = 1010580
     *    YFrom = 259508
     *    XTo = 1010679
     *    YTo = 259693
     *    ArcCenterX = 0
     *    ArcCenterY = 0
     *    street = BROADWAY
     */

    var polygonal_constraint: jts.geom.Geometry = null
    var result_shapefile: ShapefileDataStore = null
    var result_lion_feat: DefaultFeatureCollection = null

    if (filter_polygonal_area != null) {
        polygonal_constraint = WKT.read(
                         org.geoscript.io.Source.string(filter_polygonal_area)
                                      )
        if (save_to_shpfile != null) {
            // It is requested that that the results be saved to a shapefile
            result_shapefile = create_new_shapefile(save_to_shpfile,
                                                  lion_shp.features.getSchema)
            result_lion_feat = new DefaultFeatureCollection()
        }
    }

    // Filter and dump the LION street segments that match that
    // polygonal_constraint, leaving them in the result_lion_feat if
    // requested so

    filter_and_dump_geom_features(lion_shp.features, polygonal_constraint,
                                  result_lion_feat)

    // Save the resulting filtered features, if requested

    if (result_lion_feat != null && result_lion_feat.size >= 1) {
        result_shapefile.setIndexCreationEnabled(false)
        try {
            save_features_to_shapefile(result_lion_feat, result_shapefile)
        } finally {
            result_shapefile.setIndexCreationEnabled(true)
        }
        visualize_shapefile(result_shapefile)
    }

    return   // the visualization below is raising a run-time exception
}


/*
 * function: main
 *
 * The MAIN function for this program.
 *
 * @param cmdl_args the command-line arguments: if present, first argument is
 *                  a WKT geometry to filter all the NYC LION street segments;
 *                  if present, second argument is the new ESRI Shapefile to
 *                  save the filtered NYC LION street segments by that WKT geom
 */

def main(cmdl_args: Array[String]) {

     // Logging.ALL.setLoggerFactory("org.geotools.util.logging.CommonsLoggerFactory")
     // Logging.ALL.setLoggerFactory("org.geotools.util.logging.Log4JLoggerFactory")

     // Dump the New York City's Department of City Planning
     // LION GeoDB Single Line Street Base. The LION Street database should
     // have been converted first into Shapefile, at this file-location:

     val nyc_lion_street_db = "etl_dest_shp/nyc_data_exploration.shp"

     var polygonal_constraint: String = null
     if (cmdl_args.length >= 1)
         polygonal_constraint = cmdl_args(0)

     var save_to_shpfile: String = null
     if (cmdl_args.length >= 2)
         save_to_shpfile = cmdl_args(1)

     dump_nyc_lion_street_db(nyc_lion_street_db, polygonal_constraint,
                             save_to_shpfile)
}


/*
 * Entry point
 */

main(args)

