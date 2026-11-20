# wikiro-java-copy-categories

Creates a missing category on ro.wikipedia, modelled on an existing en.wikipedia category.

```
mvn install:install-file -Dfile=wikiro-java-utils-1_10.jar -DgroupId=org.wikipedia.ro.java \
    -DartifactId=wikiro-java-utils -Dversion=1.10 -Dpackaging=jar -DpomFile=<pom extracted from the jar>
mvn package
java -cp target/classes:<deps> org.wikipedia.ro.java.CopyCategories "Categorie:Nume nou" Q12345
```

Arguments: (1) name of the not-yet-existing ro category (the `Categorie:` prefix is optional),
(2) the Wikidata item ID whose enwiki sitelink is the source category.

Credentials are read the same way as in the other tools based on `AbstractExecutable`.

**Writes are disabled**: `CopyCategories.DRY_RUN = true` only logs the page creation and the Wikidata sitelink (the real calls, `WikipediaPageCache.savePage` and `Wikibase.setSitelink`, are already in place behind that flag).
