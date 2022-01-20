#!/usr/bin/python
# -*- coding: utf-8  -*-
#import numpy as np
#import cv2
#from matplotlib import pyplot as plt

#MIN_MATCH_COUNT = 5

#img1 = cv2.imread('/tmp/Monument_istoric.svg.png',0)          # queryImage
#img2 = cv2.imread('/tmp/Casa_Filstich-Plecker_-_placă.JPG',0) # trainImage

import cv2
import numpy as np
from matplotlib import pyplot as plt
import os
import pywikibot
from pywikibot import config as user

#img = cv2.imread('/tmp/Casa_Filstich-Plecker_-_placă.JPG',0) # trainImage
#img2 = img.copy()
template = cv2.imread('/tmp/Monument_istoric.svg.png',cv2.IMREAD_GRAYSCALE)          # queryImage
#template = cv2.Canny(template, 50, 200)
w, h = template.shape[::-1]
#dirname = '/mnt/jacob/filme/monumentimages/'
#dirname = '/mnt/jacob/filme/dir_rofiles.txt/'
#dirname = '/mnt/files/monumenterowp/'
dirname = '/mnt/files/monumentimages/'
user.family = 'commons'
user.mylang = 'commons'
files = [os.path.join(dirname, f) for f in os.listdir(dirname)]
	#if os.path.isfile(os.path.join(dirname, f))]

methods = ['cv2.TM_CCOEFF', 'cv2.TM_CCOEFF_NORMED']
method = eval('cv2.TM_CCOEFF_NORMED')

max_match = 0.45

for path in files:
    if path[-4:] == ".txt":
        continue
    print path
    try:
        #img = cv2.imread('/mnt/files/monumenterowp/Fișier:CiocadiaGJ (5).JPG', 0)
        img = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
        print len(img)
	print len(img[0])

        for scale in np.linspace(0.1, 1.0, 10)[::-1]:
            #print scale
            resized = cv2.resize(img, None, fx=scale, fy=scale)
            if resized.shape[0] < h or resized.shape[1] < w:
                continue

            # Apply template Matching
            res = cv2.matchTemplate(resized,template,method)
            min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(res)

            if max_val > max_match:
                #max_match = max_val
                print str(max_val) + u" @ " + str(scale)
		filename = os.path.split(path)[-1]
		page = pywikibot.Page(pywikibot.Site(), unicode(filename, 'utf8'))
		if page.exists():
			pywikibot.output(u"Updating " + page.title())
			txt = page.get()
			if txt.find("Category:Historic monument signs in Romania") == -1:
				txt += u"\n[[Category:Historic monument signs in Romania]]"
				#print txt
				page.put(txt, u"Adding category based on computer vision detection of monument signs")
			break
		else:
			pywikibot.output(u"Page %s was deleted" % filename)

                #top_left = max_loc
                #bottom_right = (top_left[0] + w, top_left[1] + h)

                #cv2.rectangle(img,top_left, bottom_right, 255, 2)

                #plt.subplot(121),plt.imshow(res,cmap = 'gray')
                #plt.title('Matching Result'), plt.xticks([]), plt.yticks([])
                ##plt.subplot(132),plt.imshow(template,cmap = 'gray')
                ##plt.title('Template'), plt.xticks([]), plt.yticks([])
                #plt.subplot(122),plt.imshow(resized,cmap = 'gray')
                #plt.title('Detected Point'), plt.xticks([]), plt.yticks([])
                #plt.show()
    except Exception as e:
        print e
	import traceback
	traceback.print_exc()
        continue
