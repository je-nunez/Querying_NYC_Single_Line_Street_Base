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

m = Basemap(llcrnrlon=LOW_LEFT_CORNR_LONGITUDE,
            llcrnrlat=LOW_LEFT_CORNER_LATITUDE,
            urcrnrlon=UP_RIGHT_CORNER_LONGITUDE,
            urcrnrlat=UP_RIGHT_CORNER_LATITUDE,
            ellps='WGS84',
            resolution='h', area_thresh=1000)

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

