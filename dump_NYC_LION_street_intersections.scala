#!/usr/bin/env scala

import scala.collection.JavaConversions._
import _root_.org.geoscript.layer._
import org.geoscript.style.combinators._
import org.geoscript.style._
import org.geoscript.render._
import org.geoscript.io.Sink
import org.geoscript.viewer.Viewer._
import org.geoscript.projection._
import java.awt.Rectangle



def dump_nyc_lion_street_db(location_LION_shapefile: String) {

    // Open the filename as a ESRI Shapefile

    val lion_shp = Shapefile(location_LION_shapefile)

    println("Number of items in LION shapefile: " + lion_shp.count)

    println("Schema: " + lion_shp.schema)

    println("Bounds rectangle: " + lion_shp.features.getBounds())

    /* Dump the fields imported into the Shapefile from the NYC intersections
     * in the LION Street db, like:
     *    the_geom = MULTILINESTRING ((-73.90478740690548 40.87892363753028, -73.90442743514292 40.87943261859585))
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

    for (f <- lion_shp.features.toArray()) {

          val feature =
               f.asInstanceOf[org.geotools.feature.simple.SimpleFeatureImpl]

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
    }

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

    val myStyle = io.SLD.read(org.geoscript.io.Source.string(sldXML.mkString(" ")))

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

def main() {

     // Dump the New York City's Department of City Planning
     // LION GeoDB Single Line Street Base. The LION Street database should
     // have been converted first into Shapefile, at this file-location:

     val nyc_lion_street_db = "etl_dest_shp/nyc_data_exploration.shp"


     dump_nyc_lion_street_db(nyc_lion_street_db)
}


/*
 * Entry point
 */

main

