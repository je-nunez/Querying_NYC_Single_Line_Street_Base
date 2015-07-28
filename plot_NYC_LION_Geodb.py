#!/usr/bin/env python

"""Test ETL results from the New York City LION Single Line Street Base Map,
plotting that GeoDB using Basemap/matplotlib"""

# pylint: disable=no-name-in-module
# pylint: disable=invalid-name

import matplotlib.pyplot as plt
from mpl_toolkits.basemap import Basemap
from matplotlib.patches import Circle

# These are the boundary coordinates given at the bottom of page 2, top of
# page 3:
#   http://www.nyc.gov/html/dcp/pdf/bytes/lion_metadata.pdf?v=15b
#
LOW_LEFT_CORNR_LONGITUDE = -74.260380
LOW_LEFT_CORNER_LATITUDE = 40.485808
UP_RIGHT_CORNER_LONGITUDE = -73.699206
UP_RIGHT_CORNER_LATITUDE = 40.917691

# This parameter refers to the minimum island in New York City to visualize
# in the matplotlib/basemap, e.g, Governors Island or Hart Island. The New
# York City's Department of City Planning LION Single Line Street Base Map
# has segments for many streets, as well as for those in these islands, so
# they appear plotted by default by the readshapefile('LION_geodb') below
# and this parameter below controls the minimum coastline to plot with
# Basemap(). With 0.9, Governors Island is visualized in the Basemap, with
# 0.6, Hart Island does.

MIN_NYC_ISLAND_TO_VISUALIZ = 0.6

# Create the Basemap

m = Basemap(llcrnrlon=LOW_LEFT_CORNR_LONGITUDE,
            llcrnrlat=LOW_LEFT_CORNER_LATITUDE,
            urcrnrlon=UP_RIGHT_CORNER_LONGITUDE,
            urcrnrlat=UP_RIGHT_CORNER_LATITUDE,
            ellps='WGS84',
            resolution='h',
            area_thresh=MIN_NYC_ISLAND_TO_VISUALIZ)

m.drawcoastlines()
m.fillcontinents(color='green')
m.drawcountries(linewidth=3)
m.drawstates()
m.drawrivers()

m.drawmapboundary(fill_color='blue')

# Plot the New York City LION Single Line Street Base in a basemap/matplotlib
s = m.readshapefile(shapefile='etl_dest_shp/nyc_data_exploration',
                    name='segments', color='gray', linewidth=0.4)

# This is the middle point of coordinates, to use it as a sample:
#
SAMPLE_LONGITUDE = (
    LOW_LEFT_CORNR_LONGITUDE + UP_RIGHT_CORNER_LONGITUDE
    ) / 2
SAMPLE_LATITUDE = (
    LOW_LEFT_CORNER_LATITUDE + UP_RIGHT_CORNER_LATITUDE
    ) / 2

TEST_POINT = Circle((SAMPLE_LONGITUDE, SAMPLE_LATITUDE),
                    radius=0.005, facecolor='yellow')
plt.gca().add_patch(TEST_POINT)


# plt.legend()
mng = plt.get_current_fig_manager()
mng.full_screen_toggle()
plt.title('Plotting New York City LION GeoDataBase in basemap/matplotlib')
plt.show()

