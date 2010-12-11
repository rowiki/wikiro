#!/bin/bash

# download the data dumps containing coordinates from Wikipedia articles
# from http://toolserver.org/~dispenser/dumps/ extract them, extract the
# coordinates and article names from the sql, then save them in a csv file

wget -r -nd -l 1 http://toolserver.org/~dispenser/dumps/
for FILE in `find . -type f -size -1000c -name "*.gz"`
do
	rm -rfv $FILE
done

gunzip *.gz

NAMES=`ls *.sql | sed -r -e "s/coord_(.*)wiki.sql/\\1/g"`

for NAME in $NAMES
do
	cat "coord_${NAME}wiki.sql" | grep "INSERT" | tr "(" "\n" | tr -d ")" | sed -r -e "s/,'([^']*'?[^']*)',/,\1,/g" | awk -v name=${NAME} -F"," '{ printf("%s,%s,%s:%s\n",$2,$3,name,$12); }' | sed -r -e "/,${NAME}:$/d"  > ${NAME}geowiki.csv
	echo "finished parsing coord_${NAME}wiki.sql"
	rm -rfv "coord_${NAME}wiki.sql"
done

rm -rfv *.sql
rm -rfv *.txt
rm -rfv *.html*
