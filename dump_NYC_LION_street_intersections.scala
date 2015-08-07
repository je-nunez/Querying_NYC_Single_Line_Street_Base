#!/usr/bin/env scala

import scala.collection.JavaConversions._
import scala.collection.mutable._
import _root_.org.geoscript.layer._
import org.geoscript.style.combinators._
import org.geoscript.style._
import org.geoscript.render._
import org.geoscript.io.Sink
import org.geoscript.viewer.Viewer._
import org.geoscript.projection._
import java.awt.Rectangle
import _root_.java.io.File
import org.geoscript.geometry.io._
import com.vividsolutions._
import org.geoscript.workspace._
import org.geotools.data.store._
import org.geoscript.feature._
import org.opengis.feature.simple.SimpleFeature
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.SimpleFeatureSource
// import org.geotools.data.Transaction
import org.geotools.data.DefaultTransaction
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollections
import org.geotools.feature.DefaultFeatureCollection



def create_resulting_shapefile(shp_filename: String, fields: FeatureCollection)
       : ShapefileDataStore =
{
    val shp_file = new File(shp_filename)
    val factory = new ShapefileDataStoreFactory()

    val params = Map("url" -> shp_file.toURI().toURL())
    // params += (("create spatial index", true))

    val new_ShpFile = factory.createNewDataStore(params).
                                  asInstanceOf[ShapefileDataStore]

    val featureBuilder = new SimpleFeatureBuilder(fields.getSchema)

    val featureType = featureBuilder.getFeatureType()

    new_ShpFile.createSchema(featureType)

    return new_ShpFile
}


def dump_nyc_lion_street_db(location_LION_shapefile: String,
                            filter_polygonal_area: String,
                            save_to_shpfile: String) {

    // Open the filename as a ESRI Shapefile

    val lion_shp = Shapefile(location_LION_shapefile)

    println("DEBUG: Class of ShpFile: " + lion_shp.getClass)

    println("Number of items in LION shapefile: " + lion_shp.count)

    println("Schema: " + lion_shp.schema)

    println("Features: " + lion_shp.features)
    println("Bounds rectangle: " + lion_shp.features.getBounds())

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
          result_shapefile = create_resulting_shapefile(save_to_shpfile,
                                                        lion_shp.features)
          result_shapefile.setIndexCreationEnabled(false)
          println("DEBUG: result_shapefile: " + result_shapefile)
          result_lion_feat = new DefaultFeatureCollection()
          println("DEBUG: result_lion_feat: " + result_lion_feat)
       }
    }

    for (f <- lion_shp.features.toArray()) {

          val feature =
               f.asInstanceOf[org.geotools.feature.simple.SimpleFeatureImpl]

          var print_this_feature: Boolean = true

          // Check if there is a polygonal_constraint (ie., a polygonal filter)
          if (polygonal_constraint != null) {
              // the New York City LION street segment intersects this filter?
              val strt_segm: jts.geom.Geometry =
                                    feature.getAttribute("the_geom").
                                               asInstanceOf[jts.geom.Geometry]
              print_this_feature = polygonal_constraint.intersects(strt_segm)
              if (!print_this_feature) {
                 /*
                  * Try if the constraint is a polygon and the LION segment is
                  * contained in it.
                  * The most general -and optimum- way to handle this is
                  * to use:
                  *      IntersectionMatrix
                  *             polygonal_constraint.relate(strt_segm)
                  * and see what are the relations between both geometries
                  * http://www.vividsolutions.com/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#relate(com.vividsolutions.jts.geom.Geometry)
                 */
                 if (polygonal_constraint.isInstanceOf[jts.geom.Polygon]) {
                      print_this_feature =
                                    polygonal_constraint.contains(strt_segm)
                 }
              }
          }

          if (print_this_feature) {
              // Print all the property names and values of this feature
              for (property <- feature.getProperties) {
                  println(property.getName() + " = " + property.getValue())
                  /*
                   * // Printing LION property types and constraints as well
                   *  println(property.getName() +
                   *          "[" + property.getType() + "] = " +
                   *          property.getValue()
                   *         )
                   */
              }

              // Add this feature to a resulting shapefile (if requested so)
              if (result_lion_feat != null) {
                  result_lion_feat.add(feature)
              }
          }
    }

    if(result_lion_feat != null && result_lion_feat.size >= 1) {

        val typeName = result_shapefile.getTypeNames()(0)
        val featureSource = result_shapefile.getFeatureSource(typeName)
        if (featureSource.isInstanceOf[SimpleFeatureStore]) {
            val featureStore = featureSource.asInstanceOf[SimpleFeatureStore]
            val transaction = new DefaultTransaction("create")
            featureStore.setTransaction(transaction)
            try {
                featureStore.addFeatures(result_lion_feat)
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
        result_shapefile.setIndexCreationEnabled(true)
    }

    return   // the visualization below is raising a run-time exception

    /* While the dump of the NYC LION shapefile is ok (above), the drawing
     * code below is failing by raising an exception, and needs to be fixed.
     * plot_NYC_LION_Geodb.py in this repository is able to render it
     * nevertheless.
     */

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
     *    the NYC LION Street database to.
     */
    // val viewport = Viewport.pad(reference(lion_shp.envelope,
    //                                       LatLon),
    //                                       frame)

    val viewport = Viewport.pad(lion_shp.getBounds(), frame)

    /*
     * // Save the New York City LION Street shapefile as an image
     * render(
     *          viewport,
     *          Seq(MapLayer(lion_shp, myStyle))
     *       ) on PNG(Sink.file("nyc_lion_shapefile.png"), frame)
     *   // ) on JPEG(Sink.file("nyc_lion_shapefile.jpg"), rectangle)
     */

    // Try to display the NYC LION Single Line Street GeoDB

    display(Seq(MapLayer(lion_shp, myStyle)))
}


/*
 *   MAIN PROGRAM
 *
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

