#!/bin//bash

python pwb.py monumente/parse_monument_article.py &> /dev/null &
python pwb.py monumente/parse_monument_article.py -lang:commons -family:commons
python pwb.py monumente/update_database.py
python pwb.py monumente/corroborate_monument_data.py
