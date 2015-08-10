#!/usr/bin/env scala

import scala.collection.JavaConversions._
import scala.collection.mutable._
import _root_.org.geoscript.layer._
// import org.geoscript.style.combinators._
// import org.geoscript.style._
import org.geoscript.render._
// import org.geoscript.io.Sink
import org.geoscript.viewer.Viewer._
// import org.geoscript.projection._
import java.awt.Rectangle
import _root_.java.io.File
import org.geoscript.geometry.io._
import com.vividsolutions._
// import org.geoscript.workspace._
import org.geotools.data.store._
import org.geoscript.feature._
// import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.simple.SimpleFeatureTypeImpl
import org.geotools.data.directory.DirectoryFeatureLocking
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
// import org.geotools.data.simple.SimpleFeatureSource
// import org.geotools.data.Transaction
import org.geotools.data.DefaultTransaction
// import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.data.simple.SimpleFeatureStore
// import org.geotools.feature.FeatureCollections
import org.geotools.feature.DefaultFeatureCollection



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
                  transaction.rollback()
                  transaction.close()
             }
        } finally {
            transaction.close()
        }
    }
}


/*
 * function: visualize_layer
 *
 * Visualize a layer. (TODO: This function is raising a run-time exception,
 * so this function needs to be worked on further)
 *
 * @param layer the layer to visualize
 */

def visualize_layer(layer: Layer) {

    /*
     * About styles in general in GeoScript Scala:
     *
     *  https://github.com/dwins/geoscript.scala/blob/master/geoscript/src/main/sphinx/style.rst
     */

    // val style = Stroke(Color("#0000FF"))

    val casings =
      Seq(("#FF0000", 1), ("#DD0000", 1), ("#AA0000", 1), ("#770000", 1))

    // A Styled Layer Descriptor (SLD)

    val sldXML =
      <UserStyle xmlns="http://www.opengis.net/sld">
        <FeatureTypeStyle>
          <Rule>
            { for ((color, width) <- casings) yield
                <LineSymbolizer>
                  <Stroke>
                    <CssParameter name="stroke">{color}</CssParameter>
                    <CssParameter name="stroke-width">{width}</CssParameter>
                  </Stroke>
                </LineSymbolizer>
            }
          </Rule>
        </FeatureTypeStyle>
      </UserStyle>

    // convert the Styled Layer Descriptor XML to a GeoScript style

    val myStyle = org.geoscript.style.io.SLD.read(
                       org.geoscript.io.Source.string(sldXML.mkString(" "))
                  )

    val frame = (1024, 1024)
    val rectangle = new Rectangle(0, 0, 1024, 1024)

    /* LatLon is precisely LatLon = lookupEPSG("EPSG:4326"),
     *    and "EPSG:4326" is "WGS 84", that is the coord-system we converted
     *    so this shapefile is in this coordinate system
     */
    // val viewport = Viewport.pad(reference(layer.envelope,
    //                                       LatLon),
    //                                       frame)

    val viewport = Viewport.pad(layer.getBounds(), frame)

    /*
     * // Save this layer of the shapefile as an image
     * render(
     *          viewport,
     *          Seq(MapLayer(layer, myStyle))
     *       ) on PNG(Sink.file("image.png"), frame)
     *   // ) on JPEG(Sink.file("image.jpg"), rectangle)
     */

    // Try to display this layer

    display(Seq(MapLayer(layer, myStyle)))
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
    }

    return   // the visualization below is raising a run-time exception

    /* While the dump of the NYC LION shapefile is ok (above), the drawing
     * code below is failing by raising an exception, and needs to be fixed.
     * plot_NYC_LION_Geodb.py in this repository is able to render it
     * nevertheless.
     */

    // visualize_layer(lion_shp)
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

