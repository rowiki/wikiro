#!/usr/bin/python
# -*- coding: utf-8  -*-

"""
This script tries to obtain images from Mapillary based on how a set of
coordinates and other information.

TODO: Pagination in responses
"""

import json
import requests


def get_images_looking(api_key, lat, lon, radius=100, limit=1000):
    """
    Function that obtain a list of images that show a certain location

    :param api_key: The Mapillary API key
    :type api_key: string

    :param lat: The latitude of the point we want to search
    :type lat: float

    :param lon: The longitude of the point we want to search
    :type lon: float

    :return: The json returned by the Mapillary API
    :return type: json or None
    """
    if limit > 1000:
        limit = 1000
    url = "https://a.mapillary.com/v3/images?per_page={limit}&client_id={api_key}&lookat={lon},{lat}&closeto={lon},{lat}&radius={radius}"
    url = url.format(limit=limit, api_key=api_key, lat=lat, lon=lon, radius=radius)

    r = requests.get(url)
    if r and r.text:
        return json.loads(r.text)
    else:
        return None


def get_image_url(image_id, size):
    """
    Function that returns the appropriate thumb URL for the image with the
    image_id.

    If size is not one of the 5 available sizes (320, 640, 1024 or
    2048), the next bigger one is returned

    :param image_id: The Mapillary image key
    :type image_id: string

    :param size: The requested size of the image.
    :type size: int
    """
    url = "https://d1cuyjsrcm0gby.cloudfront.net/{id}/thumb-{size}.jpg"
    if size <= 320:
        size = 320
    elif size <= 640:
        size = 640
    elif size <= 1024:
        size = 1024
    else:
        size = 2048
    return url.format(id=image_id, size=size)
