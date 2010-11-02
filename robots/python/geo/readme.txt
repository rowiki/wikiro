This folder contains python robots related to handling geographic date from Wikimedia projects and data transfers between Wiki and OSM

osm2wiki_coord.py - insert OSM coordinates in Wikipedia articles marked in the nodes.
osm_siruta_postcodes.py - import postcode data from SIRUTA to OSM; from there, it can be imported easier in Wikipedia
extract_coodinates.py - extract coordinates and article names from articles about villages from ro.wp; it will also 
			work for other articles, but the performance will be lower.
geowiki_sql2csv.sh - download the data dumps containing coordinates from Wikipedia articles from 
                     http://toolserver.org/~dispenser/dumps/ extract them, extract the coordinates 
                     and article names from the sql, then save them in a csv file


