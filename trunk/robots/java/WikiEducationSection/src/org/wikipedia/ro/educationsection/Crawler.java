package org.wikipedia.ro.educationsection;

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.remove;
import static org.apache.commons.lang3.StringUtils.splitByWholeSeparator;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stringtemplate.v4.ST;

public class Crawler {

    private List<String> countyList = null;
    private ST urlTemplate = null;

    public Crawler(final List<String> countyList) {
        this.countyList = countyList;
    }

    public static void main(final String[] args) {
        final List<String> countyList = Arrays.asList("BZ", "IF");
        final Crawler crawler = new Crawler(countyList);
        crawler.crawl();
    }

    public void crawl() {
        for (final String county : countyList) {
            int pageNo = 1;
            FileObject httpFile = null;
            try {
                final FileSystemManager vfs = VFS.getManager();
                do {
                    urlTemplate = new ST("http://static.admitere.edu.ro/2012/rapoarte/<county>/sc/page_<pageno>.html");
                    urlTemplate.add("county", county);
                    urlTemplate.add("pageno", pageNo);
                    final String url = urlTemplate.render();
                    InputStream httpData = null;
                    try {
                        httpFile = vfs.resolveFile(url);
                        if (httpFile.exists()) {
                            httpData = httpFile.getContent().getInputStream();
                            final String pageContent = IOUtils.toString(httpData, httpFile.getContent().getContentInfo()
                                .getContentEncoding());
                            final Document doc = Jsoup.parse(pageContent);
                            System.out.println("Successfully read url " + url);
                            final Elements trs = doc.select("table#mainTable tr[class]");
                            for (final Element tr : trs) {
                                final String schoolName = tr.children().get(2).ownText();
                                final String schoolAddress = remove(tr.children().get(4).ownText(), "&nbsp;");
                                // System.out.println("Found school " + schoolName + " from " + schoolAddress);
                                final String[] addressParts = splitByWholeSeparator(schoolAddress, ";");
                                String place;
                                String commune = null;
                                if (addressParts.length < 2) {
                                    place = trim(substringBefore(addressParts[0], ","));
                                } else {
                                    final Map<String, String> addressProps = new HashMap<String, String>();
                                    for (final String addressPart : addressParts) {
                                        addressProps.put(lowerCase(trim(substringBefore(addressPart, ":"))),
                                            trim(substringAfter(addressPart, ":")));
                                    }

                                    place = addressProps.get("sat");
                                    if (null == place) {
                                        place = addressProps.get("localitate");
                                    } else {
                                        commune = addressProps.get("localitate");
                                    }
                                }

                                System.out.println(place + ", " + commune + " school " + schoolName);
                                //enter school and school type in db
                                //lookup town in db and link to town
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(httpData);
                    }
                    pageNo++;
                } while (null != httpFile && httpFile.exists());
            } catch (final FileSystemException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}