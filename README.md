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

