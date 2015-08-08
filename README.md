# Querying NYC Single Line Street Base

Querying the New York City Department of City Planning's Open Data LION
Single Line Street Base. The script:

     etl_NYC_Lion_Geodb_to_Shapefile.sh

downloads and does an ETL of this File GeoDatabase to a custom 'shapefile'.

# Description

NYC Open Data initiative offers this GeoDatabase that is very helpful in
an explorarion of the speed of the traffic, the volume counts, etc. It is
downloaded from the `BYTES of the Big APPLE` web-site:

    http://www.nyc.gov/html/dcp/html/bytes/dwnlion.shtml

This is the explanation of the fields of the main table, and their
value-domains:

    http://www.nyc.gov/html/dcp/pdf/bytes/lion_metadata.pdf?v=15b

# Required Libraries

We need programs in the `gdal` yum package (RedHat) or `gdal-bin` (Debian)
or `gdal` (`brew` in Mac OS/X).

     yum install gdal
     
     apt-get install gdal
     
     brew install gdal

(These belong to the [Geospatial Data Abstraction Library](http://www.gdal.org/))

We also need the `dbfpy` Python package to verify the dBase DBF file. It
is available here:

     https://pypi.python.org/pypi/dbfpy

but is not available through `pip install dbfpy`. Its installation, after
downloading its source code (follow url above), can be minimal like:

     cd <extracted-dbfpy>/dbfpy-2.3.0/
     python  setup.py  build
     python  setup.py  install

For the last step of the ETL, doing a test plot of its result on the
New York City LION GeoDB, `matplotlib` and `Basemap` in Python are
necessary:

     http://matplotlib.org/users/installing.html

     http://matplotlib.org/basemap/users/installing.html

# Files

We need to do an `ETL` (Extraction-Transform-Load) phase on the geographical
database provided by the `BYTES of the Big APPLE` web-site, because it is in
the propietary `ESRI File Geodatabase` format, which need to be transformed
into the GIS `Shapefile` format.

The script `etl_NYC_Lion_Geodb_to_Shapefile.sh` does this.

The ESRI 'Shapefile' format is composed by several files, and one of the
mandatory files is in dBase IV DBF format: here there is an example of a
'Shapefile':

     http://mapserver.org/input/vector/shapefiles.html

Sometimes it is necessary to dump this dBase IV file to see what is inside.
This project includes the script `view_dbf.py`:

     view_dbf.py    a-dbf-filename.dbf

to verify the DBF file resulting from the ETL of the NYC LION FGDB file.

More details on the GIS 'Shapefile' format and its included DBF file is here:

     https://en.wikipedia.org/wiki/Shapefile

The ESRI File Geodatabase:

     https://en.wikipedia.org/wiki/ArcGIS#Geodatabase

This is a driver of GDAL (Geospatial Data Abstraction Library) to open ESRI
FGDB files:

     http://www.gdal.org/drv_filegdb.html

# A sample result

The ETL gives several results. First, from the download of the NYC LION
Single Line Street Base, it obtains the `shapefile` according to SQL
`select` statement simplifying that input database.

Second, it verifies the associated dBase IV dbf file of the shapefile,
and plots this resulting LION using `matplotlib` and `basemap` in Python,
obtaining an image like:

![sample New York City LION plot using matplotlib and basemap](/a_result_NYC_LION_matplotlib_basemap.png?raw=true "sample New York City LION plot using matplotlib and basemap")

There is another script, `filter_NYC_LION_street_intersections.scala`
(Scala using the `GeoScript`, `GeoTools`, `JTS`, `OpenGIS` libraries),
which filters and dumps the fields imported from the LION db into a
new `shapefile` according to the intersection of a filtering `WKT
geometry` with the LION street segments `the_geom` `MULTILINESTRING`
position, and this might be useful for the Data Mining on the
real-time speed of the traffic:

     ...
     the_geom = MULTILINESTRING ((-73.90478740690548 40.87892363753028,
                                  -73.90442743514292 40.87943261859585))
     segmentid = 0079707
     segmenttyp = U
     segcount = 1
     XFrom = 1010580
     YFrom = 259508
     XTo = 1010679
     YTo = 259693
     ArcCenterX = 0
     ArcCenterY = 0
     street = BROADWAY

So `filter_NYC_LION_street_intersections.scala` can be called this way:

    filter_NYC_LION_street_intersections.scala  [WKT-geometry  [resulting-shapefile]]

to do a geometrical filtering of the LION street segments according to
the argument `WKT-geometry` if present (either if the street segments are
contained in this geometry or intersect it), and optionally, saving such
filtered street segments to a newly created `resulting-shapefile`. For
example:

    filter_NYC_LION_street_intersections.scala  \
          'MULTILINESTRING ((-73.90 40.85, -73.903 40.8518))'  \
          resultshp1.shp

    filter_NYC_LION_street_intersections.scala \
          'POLYGON((-74.022498 40.738589, -73.976334 40.738589, -73.976334 40.699383, -74.022498 40.699383, -74.022498 40.738589))' \
          resultshp2.shp

and in either case, it will create those resulting shapefiles and write
to them only those LION street segments in the given WKT geometries.

(The `WKT-geometry` argument, if given, must be in the `WGS 84`
-`EPSG:4326`- coordinates-system, and the resulting shapefile with
the filtered segments, if requested, will be also in this coordinate
system -and same schema of feature-types as the original ETL of the
NYC LION single-line street segments db.)

(`filter_NYC_LION_street_intersections.scala` could also apply a
regular-expression filter on the `street` name of the LION db, but
this doesn't seem necessary for these purposes yet.)

