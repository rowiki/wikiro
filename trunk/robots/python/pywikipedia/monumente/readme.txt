This folder contains the files used for building and updating the list of historic
monuments on the Romanian Wikipedia. While designed for historic monuments, the
scripts have been programmed so they can be extended to any list that has the same
structure (templates for database and an unique id to identify each entry).

Below you can find a high-level overview of what each script does. For detailed
information, read each script's help file (call the script with the -help param)

Main scripts scripts:
monumente.sh - a shell script that can do most of the work by running the main
			scripts below

update_database.py - parse the monument lists and extract the data in the format: 
			list[dict{Arhitect, Denumire, Adresã, Datare, source, Cod, Coordonate, Imagine, Lat, Localitate, Lon}].
		   - accepts the parameter "db" which indicates which config file 
			to use (and implicitely which database to read)

parse_monument_article.py - parse the articles and images of monuments and log 
				then in the following format: 
				dict{code, list[dict{name, namespace, project, lat, lon}, ...]}; 
				also log errors where needed
                          - parameters "lang"/"family" allows choose the wiki to 
				parse (and the config to use)
                          - parameter "parse" with values quick/normal/full parses 
				the pages that are not in the database/were updated 
				since the last parse/all of them.

corroborate_monument_data.py - parse all the databases, log errors and warnings and 
				updates the database where possible. Some changes 
				are automatic, others require manual verification
                             - parameter "import" lets the user choose a json or csv 
				file to parse. The keys in json or column names in 
				csv must correspond to field names in the script 
				configuration. The filed "Cod" is compulsory.
			     - THIS SCRIPT NEEDS USER SUPERVISION! Each entry must
				be validated manually.

Other maintained scripts:

stats.py - generates some statistics in wikitext format.

error_remove.py - removes some recurring errors from the lists using regular expressions.

add_template_to_images.py - adds the {{Monument istoric}} template to images used 
				in the list but which don't have the template

error_remove.py - removes a set of errors from the lists using regular expressions. 
			DO NOT USE unless you are familiar with regexps.

lmi_shortcuts.py - create pages in the Cod:LMI namespace that redirect to the 
			monument lists

cleanup_code.py - remove spaces and potentially other detectable errors from the
			codes, either in image pages or monument lists
json2txt.py - Convert the list of files parsed, either from the local wiki or from
			commons, to a text file; this can be used for downloading
			the file.


Deprecated scripts (using the prefix deprecated_ ):
add_ran_to_lmi.py - added the RAN codes in the LMI database; the functionality 
			was integrated in the corroborate_monument_data.py 
			script

europeana_image_list.py - generates a csv file that was used by CIMEC to upload 
				the photos to Europeana; this was throw-away 
				code from the beginning
