package org.wikipedia.ro.populationdb.ua;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isAlpha;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.replaceEach;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.ua.dao.Hibernator;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Language;
import org.wikipedia.ro.populationdb.ua.model.LanguageStructurable;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Region;
import org.wikipedia.ro.populationdb.ua.model.Settlement;
import org.wikipedia.ro.populationdb.util.Transliterator;
import org.wikipedia.ro.populationdb.util.UkrainianTransliterator;

import au.com.bytecode.opencsv.CSVReader;

public class UAPercentagesParser {

    private static File[] files;

    public UAPercentagesParser(final File[] files2) {
        final List<File> fileList = new ArrayList<File>();
        fileList.addAll(Arrays.asList(files2));
        Collections.sort(fileList, new Comparator<File>() {

            public int compare(final File arg0, final File arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        });
        this.files = fileList.toArray(new File[fileList.size()]);
    }

    private static Wiki rowiki = null;

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.out.println("Missing argument to point directory with data xls files");
            // System.exit(1);
        }
        final File inDir = new File(args[0]);
        if (!inDir.isDirectory()) {
            System.out.println("Specified argument is not an existing directory");
            // System.exit(1);
        }
        rowiki = new Wiki("ro.wikipedia.org");
        try {
            final Properties credentials = new Properties();
            credentials.load(UAPercentagesParser.class.getClassLoader().getResourceAsStream("credentials.properties"));
            final String user = credentials.getProperty("Username");
            final String pass = credentials.getProperty("Password");
            rowiki.login(user, pass.toCharArray());

            final File[] files = inDir.listFiles(new FileFilter() {

                public boolean accept(File arg0) {
                    return true;
                    // return arg0.getName().contains("kirovohrad");
                }
            });
            final UAPercentagesParser parser = new UAPercentagesParser(files);

            // parser.parse();

            parser.performCorrection();
            
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final FailedLoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            rowiki.logout();
        }
    }

    public void performCorrection() {
        final Hibernator hib = new Hibernator();
        Session ses = hib.getSession();
        ses.beginTransaction();
        Region kievReg = hib.getRegionByTransliteratedName("Bila Țerkva");
        if (null != kievReg) {
            kievReg.setName(capitalize(lowerCase("КИЇВ")));
            kievReg.setTransliteratedName("Kîiiv");
            kievReg.setRomanianName("Kiev");

            final Commune kievCommune = hib.getCommuneByRomanianName("Kiev");
            kievReg.setCapital(kievCommune);
            ses.saveOrUpdate(kievReg);
        } else {
            kievReg = hib.getRegionByTransliteratedName("Kîiiv");
        }
        Region transcarpatiaRegion = hib.getRegionByTransliteratedName("Ujhorod");
        if (null != transcarpatiaRegion) {
            transcarpatiaRegion.setName(capitalize(lowerCase("ЗАКАРПАТСЬКА")));
            transcarpatiaRegion.setRomanianName("Transcarpatia");
            transcarpatiaRegion.setTransliteratedName("Zakarpatska");
            ses.saveOrUpdate(transcarpatiaRegion);
        } else {
            transcarpatiaRegion = hib.getRegionByTransliteratedName("Zakarpatska");
        }
        Region volynRegion = hib.getRegionByTransliteratedName("Luțk");
        if (null != volynRegion) {
            volynRegion.setName(capitalize(lowerCase("Волин")));
            volynRegion.setRomanianName("Volînia");
            volynRegion.setTransliteratedName("Volîn");
            ses.saveOrUpdate(volynRegion);
        } else {
            volynRegion = hib.getRegionByTransliteratedName("Volîn");
        }

        final Region crimeaRegion = hib.getRegionByTransliteratedName("Krîm");
        final Commune simferopol = hib.getCommuneByTransliteratedName("Simferopol");
        if (null != crimeaRegion && null != simferopol) {
            crimeaRegion.setCapital(simferopol);
            ses.saveOrUpdate(crimeaRegion);
        }

        Commune kiev = hib.getCommuneByTransliteratedName("Kîiiv");

        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Hmilnîk", "Vinnîțea");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Inhuleț", "Dnipropetrovsk");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Mukaceve", "Zakarpatska");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Burștîn", "Ivano-Frankivsk");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Brovarî", "Kîiiv");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Vasîlkiv", "Kîiiv");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Sambir", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Strîi", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Truskaveț", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Novîi Rozdil", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Șostka", "Sumî");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Kaniv", "Cerkasî");
        fixCityWithoutRaionOrRegionBySettingRegion(hib, "Uman", "Vinnîțea");
        ses.getTransaction().commit();

        ses = hib.getSession();
        ses.beginTransaction();

        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Bratska", "Krasnoperekopsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Azovske", "Djankoi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Starîi Krîm", "Kirovske");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Șciolkine", "Lenine");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Novofedorivskîi", "Sakî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Krîm", "Hvardiiske", "Simferopol");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Berezneanska", "Hmilnîk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Braiiliv", "Jmerînka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Brodețke", "Kozeatîn");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Voronovîțea", "Vinnîțea");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Bohdanivska", "Pavlohrad");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Oleksandro-Kalînovska", "Kosteantînivka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "V" + lowerCase("ELÎKOȘÎȘIVSKA"), "Șahtarsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Jîtomîr", "B" + lowerCase("ILKIVSKA"), "Korosten");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zakarpatska", "Batovo", "Berehove");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zakarpatska", "Vîșkovo", "Hust");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zaporijjea", "V" + lowerCase("ELÎKOBILOZERSKA"),
            "Velîka Bilozerka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, kievReg.getTransliteratedName(), "V"
            + lowerCase("ELÎKOOLEKSANDRIVSKA"), "Borîspil");
        fixRaionNameAndCapitalByTransliteratedNames(hib, kievReg.getTransliteratedName(),
            "V" + lowerCase("ELÎKOKARATULSKA"), "Pereiaslav-Hmelnîțkîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "B" + lowerCase("OHDANIVSKA"), "Znameanka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "A" + lowerCase("DJAMSKA"), "Kirovohrad");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "V" + lowerCase("ELÎKOANDRUSIVSKA"), "Svitlovodsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Mîkolaiiv", "Dmîtrivska", "Oceakiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Adamivska", "Bilhorod-Dnistrovskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Oleksiivka", "Kotovsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Bilețkivska", "Kremenciuk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Berezivska", "Lubnî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Abazivska", "Poltava");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Rivne", "Bilașivska", "Ostroh");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Bîșkinska", "Lebedîn");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Anastasivska", "Romnî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Oleksandrivska", "Izium");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Vîșnivska", "Kupeansk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Oleksiivka", "Pervomaiskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Herson", "Vasîlivska", "Kahovka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Hannopilska", "Slavuta");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Bahlaiivska", "Starokosteantîniv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Antîpivska", "Zolotonoșa");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Berzokivska", "Kaniv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Balakliivska", "Smila");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Vendîceanî", "Mohîliv-Podilskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Vinnîțea", "Hnivan", "Tîvriv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Volîn", "Holobî", "Kovel");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Volîn", "Rokîni", "Luțk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Volîn", "Ustîluh", "Volodîmîr-Volînskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Pidhorodne", "Dnipropetrovsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Radușne", "Krîvîi Rih");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Cervonohrîhorivka", "Nikopol");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Pereșcepîne", "Novomoskovsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Dnipropetrovsk", "Ilarionove", "Sînelnîkove");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Siversk", "Artemivsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Sveatohorivka", "Dobropillea");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Hrodivka", "Krasnoarmiisk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Drobîșeve", "Krasnîi Lîman");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Andriivka", "Sloveansk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Komsomolske", "Starobeșeve");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Velîkoșîșivska", "Șahtarsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Donețk", "Verhnotorețke", "Iasînuvata");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Jîtomîr", "Dovbîș", "Baranivka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Jîtomîr", "Hrîșkivți", "Berdîciv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Jîtomîr", "Novohuivînske", "Jîtomîr");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Jîtomîr", "Horodnîțea", "Novohrad-Volînskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zakarpatska", "Kolciîno", "Mukaceve");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zakarpatska", "Ciop", "Ujhorod");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zakarpatska", "Vîjkovo", "Hust");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zaporijjea", "Andriivka", "Berdeansk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zaporijjea", "Balabîne", "Zaporijjea");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zaporijjea", "Mîrne", "Melitopol");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Zaporijjea", "Moloceansk", "Tokmak");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Ivano-Frankivsk", "Voinîliv", "Kaluș");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Ivano-Frankivsk", "Hvizdeț", "Kolomîia");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kîiiv", "Uzîn", "Bila Țerkva");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kîiiv", "Velîkooleksandrivska", "Borîspil");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kîiiv", "Velîka Dîmerka", "Brovarî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kîiiv", "Hlevaha", "Vasîlkiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kîiiv", "Ciornobîl", "Ivankiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kîiiv", "Borova", "Fastiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "Nova Praha", "Oleksandria");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "Pomicina", "Dobrovelîcikivka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "Bohdanivska", "Znameanka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "Adjamska", "Kirovohrad");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Kirovohrad", "Velîkoandrusivska", "Svitlovodsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Luhansk", "Iesaulivka", "Antrațît");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Luhansk", "Velîkîi Loh", "Krasnodon");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Luhansk", "Biriukove", "Sverdlovsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Luhansk", "Zîmohirea", "Sloveanoserbsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Lviv", "Medenîci", "Drohobîci");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Lviv", "Rudkî", "Sambir");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Lviv", "Morșîn", "Strîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Mîkolaiiv", "Oleksandrivka", "Voznesensk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Mîkolaiiv", "Olșanske", "Mîkolaiiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Mîkolaiiv", "Pidhorodna", "Pervomaisk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Adamivska", "Bilhorod-Dnistrovskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Suvorove", "Izmaiil");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Odesa", "Oleksiivska", "Kotovsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Poltava", "Komîșnea", "Mîrhorod");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Rivne", "Smîha", "Dubno");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Rivne", "Kvasîliv", "Rivne");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Ciupahivka", "Ohtîrka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Cervone", "Hluhiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Duboveazivka", "Konotop");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Bîșkinska", "Lebedîn");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Anastasivska", "Romnî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Nîzî", "Sumî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Voronij", "Șostka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Sumî", "Drujba", "Iampil");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Ternopil", "Kopîciînți", "Huseatîn");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Ternopil", "Skalat", "Pidvolociîsk");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Ternopil", "Velîka Berezovîțea", "Ternopil");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Oleksandrivska", "Izium");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Vîșnivska", "Kupean");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Krasnopavlivka", "Lozova");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Merefa", "Harkiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Harkiv", "Vvedenka", "Ciuhuiv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Stara Ușîțea", "Kameaneț-Podilskîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Ciornîi Ostriv", "Hmelnîțkîi");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Hmelnîțkîi", "Hrîțiv", "Șepetivka");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Antîpivska", "Zolotonoșa");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Berkozivska", "Kaniv");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Balakliivska", "Smila");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Babanka", "Uman");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cerkasî", "Irdîn", "Cerkasî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cernihiv", "Oster", "Kozeleț");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cernihiv", "Losînivka", "Nijîn");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cernihiv", "Ladan", "Prîlukî");
        fixRaionNameAndCapitalByTransliteratedNames(hib, "Cernihiv", "Honcearivske", "Cernihiv");

        Raion teceu = hib.getRaionByTransliteratedNameAndRegion("Teaciv", transcarpatiaRegion);
        if (null != teceu) {
            teceu.setRomanianName("Teceu");
            ses.saveOrUpdate(teceu);
        }

        ses.getTransaction().commit();
        ses = hib.getSession();
        ses.beginTransaction();
        Region donetkReg = hib.getRegionByTransliteratedName("Donețk");
        if (null != donetkReg) {
            Raion pershotravnevyi = hib.getRaionByTransliteratedNameAndRegion("Manhuș", donetkReg);
            if (null != pershotravnevyi) {
                pershotravnevyi.setName("Першотравневий");
                pershotravnevyi.setTransliteratedName(new UkrainianTransliterator("Першотравневий").transliterate());
                ses.saveOrUpdate(pershotravnevyi);
            }
        }

        Region mikolaiivReg = hib.getRegionByTransliteratedName("Mîkolaiiv");
        Commune mikolaiivCity = hib.getCommuneByTransliteratedNameAndRegion("Mîkolaiiv", mikolaiivReg);
        if (null != mikolaiivReg && null != mikolaiivCity) {
            Raion raion = hib.getRaionByTransliteratedNameAndRegion("Voskresenske", mikolaiivReg);
            if (null != raion) {
                raion.setCapital(mikolaiivCity);
                raion.setName("Жовтневий");
                raion.setOriginalName("Жовтневий");
                raion.setTransliteratedName(new UkrainianTransliterator(raion.getName()).transliterate());
                ses.saveOrUpdate(raion);
            }
        }

        if (null != kievReg) {
            final Raion raion = hib.getRaionByTransliteratedNameAndRegion("Boiarka", kievReg);
            if (null != raion) {
                raion.setTransliteratedName("Kîiiv-Sveatoșîn");
                raion.setOriginalName("Києво-Святошинський");
                raion.setRomanianName("Kiev-Sveatoșîn");
                if (null != kiev) {
                    raion.setCapital(kiev);
                    ses.saveOrUpdate(raion);
                }
            }
        }

        if (null != kievReg) {
            final Raion raion = hib.getRaionByTransliteratedNameAndRegion("Volodarska", kievReg);
            if (null != raion) {
                final Commune com = hib.getCommuneByTransliteratedNameAndRaion("Kraseatîci", raion);
                if (null != com) {
                    raion.setTransliteratedName("Poliske");
                    raion.setName(capitalize(lowerCase("Поліський")));
                    raion.setRomanianName("");
                    raion.setCapital(com);
                    ses.saveOrUpdate(raion);
                }
            }
        }
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Beresteciko", "Horohiv", "Volîn");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Zelenodolsk", "Apostolove", "Dnipropetrovsk");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Verhivțeve", "Verhnodniprovsk", "Dnipropetrovsk");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Dniprorudne", "Vasîlivka", "Zaporijjea");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Vîșneve", "Kîiiv-Sveatoșîn", "Kîiiv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Ukraiinka", "Obuhiv", "Kîiiv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Șceastea", "Novoaidar", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Artemivsk", "Perevalsk", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Komarno", "Horodok", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Hodoriv", "Jîdaciv", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Hlîneanî", "Zolociv", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Sudova Vîșnea", "Mostîska", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Dubleanî", "Jovkva", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Rava-Ruska", "Jovkva", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Bibrka", "Peremîșleanî", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Belz", "Sokal", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Velîki Mostî", "Sokal", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Uhniv", "Sokal", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Dobromîl", "Starîi Sambir", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Hîriv", "Starîi Sambir", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Novoiavorivsk", "Iavoriv", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Vîlkove", "Kilia", "Odesa");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Cervonozavodske", "Lohvîțea", "Poltava");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Vorojba", "Bilopillea", "Sumî");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Horostkiv", "Huseatîn", "Ternopil");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Poceaiv", "Kremeneț", "Ternopil");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Vașkivți", "Vîjnîțea", "Cernivți");
        fixCityWithoutRaionOrRegionBySettingRaion(hib, "Maslivka", "Nova Kahovka", "Herson");

        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Oleksandrivsk", "Luhansk", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Vahrușeve", "Krasnîi Luci", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Miusînsk", "Krasnîi Luci", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Petrivske", "Krasnîi Luci", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Novodrujesk", "Lîsîceansk", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Prîvillea", "Lîsîceansk", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Almazna", "Stahanov", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Teplohirsk", "Stahanov", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Zorînsk", "Stahanov", "Luhansk");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Vînnîkî", "Lviv", "Lviv");
        fixCityWithoutRaionOrRegionBySettingRegionalTown(hib, "Stebnîk", "Drohobîci", "Lviv");

        Commune teplohirsk = hib.getCommuneByTransliteratedNameAndRaion("Teplohirsk",
            hib.getMiskradaByTransliteratedNameAndRegion("Stahanov", hib.getRegionByTransliteratedName("Luhansk")));
        if (null != teplohirsk) {
            teplohirsk.setName("Ірміно");
            teplohirsk.setTransliteratedName("Irmino");
            teplohirsk.setRomanianName("Irmino");
            ses.saveOrUpdate(teplohirsk);
        }

        Commune inkerman = hib.getUnassignedCommuneByTransliteratedName("Inkerman");
        if (null != inkerman) {
            Raion sevastopol = hib.getMiskradaByTransliteratedNameAndRegion("Sevastopol", null);
            inkerman.setRaion(sevastopol);
            ses.saveOrUpdate(inkerman);
        }

        Raion novaKahovka = hib.getMiskradaByTransliteratedNameAndRegion("Nova Kahovka",
            hib.getRegionByTransliteratedName("Herson"));
        Commune maslivka = hib.getCommuneByTransliteratedNameAndRaion("Maslivka", novaKahovka);
        if (null == maslivka) {
            maslivka = hib.getUnassignedCommuneByTransliteratedName("Maslivka");
        }
        if (null == maslivka) {
            maslivka = hib.getUnassignedCommuneByTransliteratedName("Raiske");
        }
        if (null != maslivka) {
            maslivka.setName("Райське");
            maslivka.setTransliteratedName("Raiske");
            maslivka.setRaion(novaKahovka);
            ses.saveOrUpdate(maslivka);
        }

        ses.getTransaction().commit();

        ses = hib.getSession();
        ses.beginTransaction();

        resetVillageRomanianName(hib, "Suvorove");
        resetCommuneRomanianNameExceptIds(hib, "Suvorove", 6212);
        resetVillageRomanianName(hib, "Berehove");
        resetCommuneRomanianNameExceptIds(hib, "Berehove", 2426);
        resetCommuneRomanianNameExceptIds(hib, "Suvorove", 2426);
        resetVillageRomanianNameExceptIds(hib, "Zaliznîcine", 14211);
        resetCommuneRomanianNameExceptIds(hib, "Zaliznîcine", 6167);
        resetVillageRomanianNameExceptIds(hib, "Nahirne", 14782);
        resetVillageRomanianNameExceptIds(hib, "Krînîcine", 14213);
        resetCommuneRomanianNameExceptIds(hib, "Krînîcine", 6169);
        resetVillageRomanianName(hib, "Berezivka");
        resetCommuneRomanianNameExceptIds(hib, "Berezivka", 0);
        resetVillageRomanianNameExceptIds(hib, "Horodnie", 14207);
        resetVillageRomanianNameExceptIds(hib, "Novoselîțea", 23190);
        resetCommuneRomanianNameExceptIds(hib, "Novoselîțea", 9980, 10014);
        resetVillageRomanianNameExceptIds(hib, "Jovtneve", 14209);
        resetCommuneRomanianNameExceptIds(hib, "Jovtneve", 6166);
        resetVillageRomanianName(hib, "Pidlisne");
        resetCommuneRomanianNameExceptIds(hib, "Pidlisne");
        resetVillageRomanianName(hib, "Luka");
        resetCommuneRomanianNameExceptIds(hib, "Luka");
        resetVillageRomanianName(hib, "Skala");
        resetCommuneRomanianNameExceptIds(hib, "Skala");
        resetVillageRomanianName(hib, "Murafa");
        resetCommuneRomanianNameExceptIds(hib, "Murafa");
        resetVillageRomanianName(hib, "Sokîreanî");
        resetCommuneRomanianNameExceptIds(hib, "Sokîreanî", 10059);
        resetVillageRomanianName(hib, "Serhiivka");
        resetCommuneRomanianNameExceptIds(hib, "Serhiivka", 6020);
        resetVillageRomanianName(hib, "Kutî");
        resetCommuneRomanianNameExceptIds(hib, "Kutî", 3313);
        resetVillageRomanianName(hib, "Krupa");
        resetVillageRomanianNameExceptIds(hib, "Vînohradivka", 14204);
        resetCommuneRomanianNameExceptIds(hib, "Vînohradivka", 6161);
        resetVillageRomanianNameExceptIds(hib, "Komintern");
        resetCommuneRomanianNameExceptIds(hib, "Komintern");
        resetVillageRomanianNameExceptIds(hib, "Kodak");
        resetVillageRomanianNameExceptIds(hib, "Koson");
        resetCommuneRomanianNameExceptIds(hib, "Koson");
        resetVillageRomanianNameExceptIds(hib, "Ciornîi Potik", 23161);
        resetCommuneRomanianNameExceptIds(hib, "Ciornîi Potik", 9956);
        resetVillageRomanianNameExceptIds(hib, "Osii");
        resetCommuneRomanianNameExceptIds(hib, "Osii");
        resetVillageRomanianNameExceptIds(hib, "Torun");
        resetCommuneRomanianNameExceptIds(hib, "Torun");
        resetVillageRomanianNameExceptIds(hib, "Lîpoveț");
        resetCommuneRomanianNameExceptIds(hib, "Lîpoveț", 576);
        resetVillageRomanianNameExceptIds(hib, "Iza");
        resetCommuneRomanianNameExceptIds(hib, "Iza");
        resetVillageRomanianNameExceptIds(hib, "Plavni", 14785);
        resetVillageRomanianNameExceptIds(hib, "Hoverla");
        resetCommuneRomanianNameExceptIds(hib, "Hoverla");
        resetVillageRomanianNameExceptIds(hib, "Roztokî", 23308);
        resetCommuneRomanianNameExceptIds(hib, "Roztokî", 10053);
        resetVillageRomanianNameExceptIds(hib, "Krasna");
        resetCommuneRomanianNameExceptIds(hib, "Krasna");
        resetVillageRomanianNameExceptIds(hib, "Hlîboka");
        resetVillageRomanianNameExceptIds(hib, "Hora");
        resetCommuneRomanianNameExceptIds(hib, "Hora");
        resetVillageRomanianNameExceptIds(hib, "Vînnîkî");
        resetCommuneRomanianNameExceptIds(hib, "Vînnîkî");
        resetVillageRomanianNameExceptIds(hib, "Rata");
        resetVillageRomanianNameExceptIds(hib, "Pasat");
        resetCommuneRomanianNameExceptIds(hib, "Pasat");
        resetVillageRomanianNameExceptIds(hib, "Lviv");
        resetCommuneRomanianNameExceptIds(hib, "Lviv", 4995);
        resetVillageRomanianNameExceptIds(hib, "Zatoka");
        resetVillageRomanianNameExceptIds(hib, "Proletar");
        resetVillageRomanianNameExceptIds(hib, "Hotîn");
        resetCommuneRomanianNameExceptIds(hib, "Hotîn", 10107);
        resetVillageRomanianNameExceptIds(hib, "Poznan");
        resetCommuneRomanianNameExceptIds(hib, "Poznan");
        resetVillageRomanianNameExceptIds(hib, "Stanislav");
        resetCommuneRomanianNameExceptIds(hib, "Stanislav");
        resetVillageRomanianNameExceptIds(hib, "Hatna");
        resetCommuneRomanianNameExceptIds(hib, "Hatna");
        resetVillageRomanianNameExceptIds(hib, "Vasîlkiv");
        resetCommuneRomanianNameExceptIds(hib, "Vasîlkiv");
        resetVillageRomanianNameExceptIds(hib, "Zbruci");
        resetVillageRomanianNameExceptIds(hib, "Hutir");

        setCommuneRomanianNameTo(hib, "Biserica Albă", "Bila Țerkva", "Rahiv", "Zakarpatska");
        setVillageRomanianNameTo(hib, "Apșa de Sus", "Verhnie Vodeane", "Verhnie Vodeane", "Rahiv", "Zakarpatska");
        setVillageRomanianNameTo(hib, "Strâmba", "Strîmba", "Verhnie Vodeane", "Rahiv", "Zakarpatska");
        setCommuneRomanianNameTo(hib, "Apșa de Sus", "Verhnie Vodeane", "Rahiv", "Zakarpatska");
        setVillageRomanianNameTo(hib, "Apșa de Jos", "Dibrova", "Dibrova", "Teaciv", "Zakarpatska");
        setVillageUkrainianNameTo(hib, "Нижня Апша", "Dibrova", "Dibrova", "Teaciv", "Zakarpatska");
        setVillageRomanianNameTo(hib, "Peștera", "Peșcera", "Dibrova", "Teaciv", "Zakarpatska");
        setCommuneRomanianNameTo(hib, "Apșa de Jos", "Dibrova", "Teaciv", "Zakarpatska");
        setCommuneUkrainianNameTo(hib, "Нижня Апша", "Нижньоапшанська", "Dibrova", "Teaciv", "Zakarpatska");
        setVillageRomanianNameTo(hib, "Apșa de Mijloc", "Serednie Vodeane", "Serednie Vodeane", "Rahiv", "Zakarpatska");
        setCommuneRomanianNameTo(hib, "Apșa de Mijloc", "Serednie Vodeane", "Rahiv", "Zakarpatska");

        ses.getTransaction().commit();
    }

    private void setCommuneUkrainianNameTo(Hibernator hib, String toName, String toOriginalName, String fromName,
                                           String raionName, String regionName) {
        Region reg = hib.getRegionByTransliteratedName(regionName);
        if (null == reg) {
            return;
        }
        Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionName, reg);
        if (null == raion) {
            return;
        }
        Commune com = hib.getCommuneByTransliteratedNameAndRaion(fromName, raion);
        if (null == com) {
            return;
        }
        if (null != toName) {
            com.setName(toName);
            com.setTransliteratedName(new UkrainianTransliterator(toName).transliterate());
        }
        if (null != toOriginalName)
            com.setOriginalName(toOriginalName);
        hib.getSession().saveOrUpdate(com);
    }

    private void setCommuneRomanianNameTo(Hibernator hib, String toName, String fromName, String raionName, String regionName) {
        Region reg = hib.getRegionByTransliteratedName(regionName);
        if (null == reg) {
            return;
        }
        Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionName, reg);
        if (null == raion) {
            return;
        }
        Commune com = hib.getCommuneByTransliteratedNameAndRaion(fromName, raion);
        if (null == com) {
            return;
        }

        com.setRomanianName(toName);
        hib.getSession().saveOrUpdate(com);
    }

    private void setVillageRomanianNameTo(Hibernator hib, String toName, String fromName, String communeName,
                                          String raionName, String regionName) {
        Region reg = hib.getRegionByTransliteratedName(regionName);
        if (null == reg) {
            return;
        }
        Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionName, reg);
        if (null == raion) {
            return;
        }
        Commune com = hib.getCommuneByTransliteratedNameAndRaion(communeName, raion);
        if (null == com) {
            return;
        }
        Settlement village = hib.getSettlementByTransliteratedNameAndCommune(fromName, com);
        if (null == village) {
            return;
        }

        village.setRomanianName(toName);
        hib.getSession().saveOrUpdate(village);
    }

    private void setVillageUkrainianNameTo(Hibernator hib, String toName, String fromTranslName, String communeName,
                                           String raionName, String regionName) {
        Region reg = hib.getRegionByTransliteratedName(regionName);
        if (null == reg) {
            return;
        }
        Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionName, reg);
        if (null == raion) {
            return;
        }
        Commune com = hib.getCommuneByTransliteratedNameAndRaion(communeName, raion);
        if (null == com) {
            return;
        }
        Settlement village = hib.getSettlementByTransliteratedNameAndCommune(fromTranslName, com);
        if (null == village) {
            return;
        }

        village.setName(toName);
        village.setTransliteratedName(new UkrainianTransliterator(toName).transliterate());
        hib.getSession().saveOrUpdate(village);
    }

    private void resetVillageRomanianName(Hibernator hib, String string) {
        List<Settlement> villagesWithName = hib.findAllVillagesWithName(string);
        for (Settlement eachVillage : villagesWithName) {
            eachVillage.setRomanianName("");
            hib.getSession().saveOrUpdate(eachVillage);
        }
    }

    private void resetVillageRomanianNameExceptIds(Hibernator hib, String string, long... ids) {
        List<Settlement> villagesWithName = hib.findAllVillagesWithName(string);
        for (Settlement eachVillage : villagesWithName) {
            if (ArrayUtils.contains(ids, eachVillage.getId())) {
                continue;
            }
            eachVillage.setRomanianName("");
            hib.getSession().saveOrUpdate(eachVillage);
        }
    }

    private void resetCommuneRomanianNameExceptIds(Hibernator hib, String string, long... ids) {
        List<Commune> communesWithName = hib.findAllCommunesWithName(string);
        for (Commune eachVillage : communesWithName) {
            if (ArrayUtils.contains(ids, eachVillage.getId())) {
                continue;
            }
            eachVillage.setRomanianName("");
            hib.getSession().saveOrUpdate(eachVillage);
        }
    }

    private void fixCityWithoutRaionOrRegionBySettingRegionalTown(Hibernator hib, String cityTranslName,
                                                                  String miskradaTranslName, String regionTranslName) {
        Session ses = hib.getSession();
        Commune city = hib.getUnassignedCommuneByTransliteratedName(cityTranslName);
        if (null == city) {
            return;
        }
        Region reg = hib.getRegionByTransliteratedName(regionTranslName);
        if (null == reg) {
            return;
        }
        Raion raion = hib.getMiskradaByTransliteratedNameAndRegion(miskradaTranslName, reg);
        if (null == raion) {
            return;
        }
        city.setRaion(raion);
        ses.saveOrUpdate(city);
    }

    private void fixRaionNameAndCapitalByTransliteratedNames(final Hibernator hib, final String regionName,
                                                             final String raionWrongName, final String correctCapitalName) {
        final Region reg = hib.getRegionByTransliteratedName(regionName);
        if (null != reg) {
            final Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionWrongName, reg);
            if (null != raion) {
                final Commune com = hib.getCommuneByTransliteratedNameAndRegion(correctCapitalName, reg);
                if (null != com) {
                    raion.setTransliteratedName(com.getTransliteratedName());
                    raion.setName(com.getName());
                    raion.setRomanianName(com.getRomanianName());
                    raion.setCapital(com);
                    final Session ses = hib.getSession();
                    ses.saveOrUpdate(raion);
                }
            }
        }
    }

    private void fixCityWithoutRaionOrRegionBySettingRegion(Hibernator hib, String cityTranslName, String regionTranslName) {
        Session ses = hib.getSession();
        Commune city = hib.getUnassignedCommuneByTransliteratedName(cityTranslName);
        if (null == city) {
            return;
        }
        Region reg = hib.getRegionByTransliteratedName(regionTranslName);
        if (null == reg) {
            return;
        }
        city.setRegion(reg);
        ses.saveOrUpdate(city);
    }

    private void fixCityWithoutRaionOrRegionBySettingRaion(Hibernator hib, String cityTranslName, String raionTranslName,
                                                           String regionTranslName) {
        Session ses = hib.getSession();
        Commune city = hib.getUnassignedCommuneByTransliteratedName(cityTranslName);
        if (null == city) {
            return;
        }
        Region reg = hib.getRegionByTransliteratedName(regionTranslName);
        if (null == reg) {
            return;
        }
        Raion raion = hib.getRaionByTransliteratedNameAndRegion(raionTranslName, reg);
        if (null == raion) {
            return;
        }
        city.setRaion(raion);
        ses.saveOrUpdate(city);
    }

    public void parse() {
        final Hibernator hib = new Hibernator();
        Session session = hib.getSession();
        session.beginTransaction();

        final List<Language> limbi = new ArrayList<Language>();
        limbi.add(new Language("Ucraineană"));
        limbi.add(new Language("Rusă"));
        limbi.add(new Language("Belarusă"));
        limbi.add(new Language("Bulgară"));
        limbi.add(new Language("Armeană"));
        limbi.add(new Language("Găgăuză"));
        limbi.add(new Language("Tătară crimeeană"));
        limbi.add(new Language("Română"));
        limbi.add(new Language("Germană"));
        limbi.add(new Language("Polonă"));
        limbi.add(new Language("Romani"));
        limbi.add(new Language("Slovacă"));
        limbi.add(new Language("Maghiară"));
        limbi.add(new Language("Karaim"));
        limbi.add(new Language("Ebraică"));
        limbi.add(new Language("Greacă"));
        for (final Language lang : limbi) {
            session.save(lang);
        }

        session.getTransaction().commit();

        for (final File eachFile : files) {
            session = hib.getSession();
            session.beginTransaction();
            CSVReader reader = null;
            try {

                reader = new CSVReader(new InputStreamReader(new FileInputStream(eachFile), "Cp1251"), '\t', '\"');
                String[] line = null;
                for (int i = 0; i < 5; i++) {
                    line = reader.readNext();
                }
                Commune currentCommune = null;
                int currentCommuneLevel = 2;
                Raion currentRaion = null;
                Region currentRegion = null;
                while (null != (line = reader.readNext())) {
                    final String nume = line[0];
                    if (isEmpty(nume) || line.length < 2) {
                        continue;
                    }
                    final Transliterator t = new UkrainianTransliterator(nume);
                    final String numeTransliterat = t.transliterate();

                    final String[] splitName = split(numeTransliterat);
                    final String[] splitNameUa = split(nume);

                    if (splitName.length < 2) {
                        continue;
                    }

                    // regiune
                    if (ArrayUtils.contains(splitName, "OBLAST")
                        || (ArrayUtils.contains(splitName, "KRÎM") && ArrayUtils.contains(splitName, "AVTONOMNA") && ArrayUtils
                            .contains(splitName, "RESPUBLIKA"))) {
                        currentRegion = new Region();
                        currentRaion = null;
                        if (ArrayUtils.contains(splitName, "KRÎM")) {
                            currentRegion.setRomanianName("Crimeea");
                            currentRegion.setTransliteratedName("Krîm");
                            currentRegion.setName(capitalize(lowerCase("КРИМ")));
                        }
                        extractLanguageData(limbi, line, currentRegion);
                        session.save(currentRegion);
                        currentCommuneLevel = 2;
                        continue;
                    }
                    // raion
                    if (ArrayUtils.contains(splitName, "RAION")) {
                        currentRaion = new Raion();
                        currentRegion.getRaioane().add(currentRaion);
                        currentRaion.setRegion(currentRegion);
                        if (ArrayUtils.contains(splitName, "RAION")) {
                            final int indexOfRaion = ArrayUtils.indexOf(splitName, "RAION");
                            final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfRaion);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfRaion);
                            for (int i = 0; i < nameParts.length; i++) {
                                final String[] lineSeparatedParts = split(nameParts[i], '-');
                                for (int j = 0; j < lineSeparatedParts.length; j++) {
                                    lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                                }
                                nameParts[i] = join(lineSeparatedParts, '-');

                                final String[] lineSeparatedPartsUa = split(namePartsUa[i], '-');
                                for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                    lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                                }
                                namePartsUa[i] = join(lineSeparatedPartsUa, '-');
                            }
                            currentRaion.setTransliteratedName(join(nameParts, " "));
                            currentRaion.setName(join(namePartsUa, " "));
                            currentRaion.setOriginalName(join(namePartsUa, " "));
                            extractLanguageData(limbi, line, currentRaion);
                        }
                        session.save(currentRaion);
                        currentCommuneLevel = 2;

                        continue;
                    }
                    if (ArrayUtils.contains(splitName, "(miskrada)")) {
                        currentRaion = new Raion();
                        final int indexOfMiskrada = ArrayUtils.indexOf(splitName, "(miskrada)");
                        final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfMiskrada);
                        final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfMiskrada);

                        for (int i = 0; i < nameParts.length; i++) {
                            final String[] lineSeparatedParts = split(nameParts[i], '-');
                            for (int j = 0; j < lineSeparatedParts.length; j++) {
                                lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                            }
                            nameParts[i] = join(lineSeparatedParts, '-');

                            final String[] lineSeparatedPartsUa = split(namePartsUa[i], '-');
                            for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                            }
                            namePartsUa[i] = join(lineSeparatedPartsUa, '-');
                        }
                        currentRaion.setTransliteratedName(join(nameParts, " "));
                        currentRaion.setName(join(namePartsUa, " "));
                        currentRaion.setOriginalName(join(namePartsUa, " "));
                        currentRaion.setMiskrada(true);
                        currentRaion.setRegion(currentRegion);
                        currentCommuneLevel = 2;
                        System.out.println(" -- MISKRADA " + currentRaion.getName() + " - "
                            + currentRaion.getTransliteratedName());
                        extractLanguageData(limbi, line, currentRaion);
                        session.save(currentRaion);

                        if (null != currentCommune
                            && StringUtils.equals(currentCommune.getName(), capitalize(lowerCase("СЕВАСТОПОЛЬ")))) {
                            currentRegion = new Region();
                            currentRegion.setName(capitalize(lowerCase("СЕВАСТОПОЛЬ")));
                            currentRegion.setTransliteratedName("Sevastopol-oraș");
                            currentRegion.setRomanianName("orașul Sevastopol");
                            session.save(currentRegion);
                        }
                    }
                    // comuna, oras, asezare urbana
                    if (ArrayUtils.contains(splitName, "silrada") || StringUtils.equals(splitName[0], "smt")
                        || StringUtils.equals(splitName[0], "m.")) {

                        currentCommune = new Commune();
                        if (ArrayUtils.contains(splitName, "silrada")) {
                            final int indexOfSilrada = ArrayUtils.indexOf(splitName, "silrada");
                            final String[] nameParts = ArrayUtils.subarray(splitName, 0, indexOfSilrada);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 0, indexOfSilrada);

                            for (int i = 0; i < nameParts.length; i++) {
                                final String[] lineSeparatedParts = split(nameParts[i], '-');
                                for (int j = 0; j < lineSeparatedParts.length; j++) {
                                    lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                                }
                                nameParts[i] = join(lineSeparatedParts, '-');

                                final String[] lineSeparatedPartsUa = split(namePartsUa[i], '-');
                                for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                    lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                                }
                                namePartsUa[i] = join(lineSeparatedPartsUa, '-');
                            }

                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            // currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            currentCommune.setTown(0);
                            currentCommune.setRaion(currentRaion);
                            currentCommune.setOriginalName(currentCommune.getName());
                            session.save(currentCommune);
                        }
                        if (StringUtils.equals(splitName[0], "smt")) {
                            int i = splitName.length;
                            for (i = 1; i < splitName.length
                                && isAlpha(replaceEach(splitName[i], new String[] { "-", "`", "'" }, new String[] { "", "",
                                    "" })); i++) {
                                final String[] lineSeparatedParts = split(splitName[i], '-');
                                for (int j = 0; j < lineSeparatedParts.length; j++) {
                                    lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                                }
                                splitName[i] = join(lineSeparatedParts, '-');

                                final String[] lineSeparatedPartsUa = split(splitNameUa[i], '-');
                                for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                    lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                                }
                                splitNameUa[i] = join(lineSeparatedPartsUa, '-');
                            }
                            final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                            currentCommune.setTown(1);
                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            currentCommune.setOriginalName(currentCommune.getName());

                            if (null == currentRaion) {
                                currentCommune.setRegion(currentRegion);
                                currentRegion.getCities().add(currentCommune);
                                session.save(currentRegion);
                            } else {
                                currentCommune.setRaion(currentRaion);
                            }
                            session.save(currentCommune);

                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }
                        if (StringUtils.equals(splitName[0], "m.")) {
                            int i = splitName.length;
                            for (i = 1; i < splitName.length
                                && isAlpha(replaceEach(splitName[i], new String[] { "-", "`", "'" }, new String[] { "", "",
                                    "" })); i++) {
                                final String[] lineSeparatedParts = split(splitName[i], '-');
                                for (int j = 0; j < lineSeparatedParts.length; j++) {
                                    lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                                }
                                splitName[i] = join(lineSeparatedParts, '-');

                                final String[] lineSeparatedPartsUa = split(splitNameUa[i], '-');
                                for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                    lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                                }
                                splitNameUa[i] = join(lineSeparatedPartsUa, '-');
                            }
                            final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                            final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                            currentCommune.setTown(2);
                            currentCommune.setTransliteratedName(join(nameParts, " "));
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setName(join(namePartsUa, " "));
                            currentCommune.setOriginalName(currentCommune.getName());

                            if (currentCommuneLevel < 2) {
                                currentRaion = null;
                            }
                            if (null == currentRaion && null == currentRegion) {
                                if (StringUtils.equals(currentCommune.getName(), capitalize(lowerCase("КИЇВ")))) {
                                    currentRegion = new Region();
                                    currentRegion.setName(capitalize(lowerCase("КИЇВ")));
                                    currentRegion.setTransliteratedName("Kîiiv-oraș");
                                    currentRegion.setRomanianName("orașul Kiev");
                                    session.save(currentRegion);
                                }
                            }
                            if ((null == currentRaion || currentRaion.isMiskrada()) && null != currentRegion) {
                                if (isEmpty(currentRegion.getName())) {
                                    currentRegion.setName(currentCommune.getName());
                                    currentRegion.setTransliteratedName(currentCommune.getTransliteratedName());
                                    currentRegion.setRomanianName(getRomanianName(getPossibleNames(currentRegion)));
                                    currentRegion.setCapital(currentCommune);
                                    System.out.println("REGION - " + currentRegion.getName() + " - "
                                        + currentRegion.getTransliteratedName());
                                }
                                if (null == currentRaion) {
                                    currentRegion.getCities().add(currentCommune);
                                    currentCommune.setRegion(currentRegion);
                                }
                                session.save(currentRegion);
                            }
                            session.save(currentCommune);
                        }
                        final Transliterator t1 = new UkrainianTransliterator(currentCommune.getName());
                        t1.transliterate();

                        if (null != currentRaion) {
                            if (currentRaion.getCommunes().size() == 0) {
                                currentRaion
                                    .setRomanianName(getRomanianName(getPossibleNames(
                                        currentRaion,
                                        defaultString(currentCommune.getRomanianName(),
                                            currentCommune.getTransliteratedName()))));
                                currentRaion.setName(currentCommune.getName());
                                currentRaion.setTransliteratedName(new UkrainianTransliterator(currentCommune.getName())
                                    .transliterate());
                                currentRaion.setCapital(currentCommune);
                                currentCommune.setRaion(currentRaion);
                                System.out.println("Raion " + currentRaion.getName() + " - "
                                    + currentRaion.getTransliteratedName());
                            }
                            currentRaion.getCommunes().add(currentCommune);
                            session.save(currentRaion);
                        }
                        if (2 == currentCommune.getTown()) {
                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }

                        extractLanguageData(limbi, line, currentCommune);
                        session.save(currentCommune);
                        currentCommuneLevel = currentCommune.getTown();
                    }
                    // sat
                    if (StringUtils.equals(splitName[0], "s.") || StringUtils.equals(splitName[0], "s-șce.")) {
                        int i = splitName.length;
                        final Settlement sat = new Settlement();
                        for (i = 1; i < splitName.length
                            && isAlpha(replaceEach(splitName[i], new String[] { "-", "`", "'" }, new String[] { "", "", "" })); i++) {
                            final String[] lineSeparatedParts = split(splitName[i], '-');
                            for (int j = 0; j < lineSeparatedParts.length; j++) {
                                lineSeparatedParts[j] = capitalize(lowerCase(lineSeparatedParts[j]));
                            }
                            splitName[i] = join(lineSeparatedParts, '-');

                            final String[] lineSeparatedPartsUa = split(splitNameUa[i], '-');
                            for (int j = 0; j < lineSeparatedPartsUa.length; j++) {
                                lineSeparatedPartsUa[j] = capitalize(lowerCase(lineSeparatedPartsUa[j]));
                            }
                            splitNameUa[i] = join(lineSeparatedPartsUa, '-');
                        }

                        final String[] nameParts = ArrayUtils.subarray(splitName, 1, i);
                        final String[] namePartsUa = ArrayUtils.subarray(splitNameUa, 1, i);
                        sat.setTransliteratedName(join(nameParts, " "));
                        sat.setName(join(namePartsUa, " "));
                        if (0 == currentCommune.getSettlements().size() && 0 == currentCommune.getTown()) {
                            currentCommune.setName(sat.getName());
                            currentCommune.setTransliteratedName(sat.getTransliteratedName());
                            currentCommune.setRomanianName(getRomanianName(getPossibleNames(currentCommune)));
                            currentCommune.setCapital(sat);
                            System.out.println(currentCommune.getTown() + " - " + currentCommune.getName() + " - "
                                + currentCommune.getTransliteratedName());
                        }
                        sat.setCommune(currentCommune);
                        sat.setRomanianName(getRomanianName(getPossibleNames(sat)));
                        currentCommune.getSettlements().add(sat);

                        extractLanguageData(limbi, line, sat);

                        session.save(sat);
                        session.save(currentCommune);
                        System.out.println("\tsat " + sat.getName() + " - " + sat.getTransliteratedName());
                    }
                }
            } catch (final UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (null != reader) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            session.getTransaction().commit();
        }
    }

    private void extractLanguageData(final List<Language> limbi, final String[] line,
                                     final LanguageStructurable currentCommune) {
        for (int langIndex = 0; langIndex < line.length - 1; langIndex++) {
            final String langData = line[1 + langIndex];
            int langListIndex = langIndex;
            if (langIndex == 11) {
                langListIndex = 7;
            }
            if (langIndex > 11) {
                langListIndex = langIndex - 1;
            }
            Double langNumber = -1.0;
            try {
                langNumber = Double.parseDouble(langData);
            } catch (final NumberFormatException nfe) {
                continue;
            }
            if (langIndex == 11) {
                final Double moldValue = currentCommune.getLanguageStructure().get(limbi.get(7));
                if (null != moldValue && 0 < moldValue.doubleValue()) {
                    langNumber += moldValue;
                }
            }
            if (0 < langNumber) {
                currentCommune.getLanguageStructure().put(limbi.get(langListIndex), langNumber);
            }
        }
    }

    private String getRomanianName(final List<String> possibleNames) {
        boolean error = false;
        do {
            try {

                final boolean[] existance = rowiki.exists(possibleNames.toArray(new String[possibleNames.size()]));
                for (int i = 0; i < possibleNames.size(); i++) {
                    if (!existance[i]) {
                        continue;
                    }
                    String redirectResolution = rowiki.resolveRedirect(possibleNames.get(i));
                    if (isEmpty(redirectResolution)) {
                        redirectResolution = possibleNames.get(i);
                    }
                    redirectResolution = removeStart(redirectResolution, "Raionul ");
                    redirectResolution = removeStart(redirectResolution, "Regiunea ");
                    redirectResolution = substringBefore(redirectResolution, ",");
                    redirectResolution = substringBefore(redirectResolution, "(");
                    redirectResolution = trim(redirectResolution);
                    return redirectResolution;
                }
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                error = true;
            }
        } while (error);
        return "";
    }

    private List<String> getPossibleNames(final Settlement sat) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add(sat.getTransliteratedName() + ", Ucraina");
        possibleNames.add(sat.getTransliteratedName() + " (Ucraina)");
        if (null != sat.getCommune().getRaion()) {
            possibleNames.add(sat.getTransliteratedName() + ", " + sat.getCommune().getRaion().getTransliteratedName());
            if (!isEmpty(sat.getCommune().getRaion().getRomanianName())) {
                possibleNames.add(sat.getTransliteratedName() + ", " + sat.getCommune().getRaion().getRomanianName());
            }
        }
        possibleNames.add(sat.getTransliteratedName());
        return possibleNames;
    }

    private List<String> getPossibleNames(final Commune commune) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add(commune.getTransliteratedName() + ", Ucraina");
        possibleNames.add(commune.getTransliteratedName() + " (Ucraina)");
        if (null != commune.getRaion()) {
            possibleNames.add(commune.getTransliteratedName() + ", " + commune.getRaion().getTransliteratedName());
            if (!isEmpty(commune.getRaion().getRomanianName())) {
                possibleNames.add(commune.getTransliteratedName() + ", " + commune.getRaion().getRomanianName());
            }
        }
        possibleNames.add(commune.getTransliteratedName());
        return possibleNames;
    }

    private List<String> getPossibleNames(final Raion raion, final String roName) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add("Raionul " + roName + ", Ucraina");
        possibleNames.add("Raionul " + roName + " (Ucraina)");
        if (null != raion.getRegion()) {
            possibleNames.add("Raionul " + roName + ", " + raion.getRegion().getTransliteratedName());
            if (!isEmpty(raion.getRegion().getRomanianName())) {
                possibleNames.add("Raionul " + roName + ", " + raion.getRegion().getRomanianName());
            }
        }
        possibleNames.add("Raionul " + roName);
        return possibleNames;
    }

    private List<String> getPossibleNames(final Region region) {
        final List<String> possibleNames = new ArrayList<String>();
        possibleNames.add("Regiunea " + region.getTransliteratedName() + ", Ucraina");
        possibleNames.add("Regiunea " + region.getTransliteratedName() + " (Ucraina)");
        possibleNames.add("Regiunea " + region.getTransliteratedName());
        return possibleNames;
    }
}
