<?php
$file = 'redirect-log.txt';

$lmi=$_REQUEST["lmi"];
$ip=$_SERVER['REMOTE_ADDR'];
$ua = $_SERVER['HTTP_USER_AGENT'];
$date= date("Y/m/d:h:i:sa");

 switch ($lmi) {
   case "2015-1": $redirect="https://commons.wikimedia.org/wiki/File:Ansamblul_bisericii_evanghelice_fortificate_din_Archita_MS-II-a-A-15596.JPG";break;
   case "2015-2": $redirect="https://commons.wikimedia.org/wiki/File:Biserica_Calvaria_de_la_Cluj-M%C4%83n%C4%83%C8%99tur,_vedere_sud-vestic%C4%83,_2014.JPG";break;
   case "2015-3": $redirect="https://commons.wikimedia.org/wiki/File:RO_BZ_Gura_Siriului_World_War_I_heroes_cemetery_02.jpg";break;
   case "2015-4": $redirect="https://commons.wikimedia.org/wiki/File%3AOzd.JPG";break;
   case "2015-5": $redirect="https://commons.wikimedia.org/wiki/File%3APrejmer_-_Ansamblul_bisericii_evanghelice_fortificate_-_vedere_dinspre_nordest.jpg";break;
   case "2015-6": $redirect="https://commons.wikimedia.org/wiki/File%3ABruiu_-_Ansamblul_bisericii_evanghelice_fortificate.jpg";break;
   case "2015-7": $redirect="https://commons.wikimedia.org/wiki/File%3ATurnul_alb_din_Bra%C8%99ov%3B_pe_fundal_se_vede_T%C3%A2mpa.jpg";break;
   case "2015-8": $redirect="https://commons.wikimedia.org/wiki/File%3AAnsamblul_Bisericii_Fortificate_%E2%80%9ESf._Mihail%E2%80%9D%2C_Cisnadioara.jpg";break;
   case "2015-9": $redirect="https://commons.wikimedia.org/wiki/File%3AConacul_Procopie_Casota_4.jpg";break;
   case "2015-10": $redirect="https://commons.wikimedia.org/wiki/File%3ARO_GJ_Biserica_Sfantul_Ioan_din_Cojani_(35).JPG";break;
   case "2015-25": $redirect="https://commons.wikimedia.org/wiki/File:Conacul_S%C4%83h%C4%83teni_la_amurg.jpg";break;
   case "2014-1": $redirect="https://commons.wikimedia.org/wiki/File%3A006_MG_6430_Tropaeum_Traiani_Adamclisi_006.jpg";break;
   case "2014-2": $redirect="https://commons.wikimedia.org/wiki/File%3ACetatea_R%C3%A2%C8%99nov%2C_v%C4%83zut%C4%83_din_%C8%99oseaua_Cristian-R%C3%A2%C8%99nov..jpg";break;
   case "2014-3": $redirect="https://commons.wikimedia.org/wiki/File%3AAnsamblul_bisericii_evanghelice_fortificat-vedere_aeriana.JPG";break;
   case "2014-4": $redirect="https://commons.wikimedia.org/wiki/File%3ASarmizegetusa_Regia_%28Grigore_Roibu%29.jpg";break;
   case "2014-5": $redirect="https://commons.wikimedia.org/wiki/File%3ABiserica_Strei_(Grigore_Roibu).jpg";break;
   case "2014-6": $redirect="https://commons.wikimedia.org/wiki/File%3ABiserica_Neagra%2C_Brasov%2C_Romania.jpg";break;
   case "2014-7": $redirect="https://commons.wikimedia.org/wiki/File%3ACapela_Sfanta_Treime_a_Palatului_Stirbei.jpg";break;
   case "2014-8": $redirect="https://commons.wikimedia.org/wiki/File%3AIarna_la_Chilia_lui_Daniil_Sihastrul.jpg";break;
   case "2014-9": $redirect="https://commons.wikimedia.org/wiki/File%3AAnsamblul_%E2%80%9ECetate%E2%80%9D_-vedere_aeriana.JPG";break;
   case "2014-10": $redirect="https://commons.wikimedia.org/wiki/File%3AAnsamblul_bisericii_evanghelice-vedere_aeriana.JPG";break;
   case "2013-1": $redirect="https://commons.wikimedia.org/wiki/File%3ACetatea_Rupea%2C_judetul_Brasov.jpg";break;
   case "2013-2": $redirect="https://commons.wikimedia.org/wiki/File%3ACastelul_Corvinestilor_-_vedere_median-frontala_de_pe_podul_de_acces.jpg";break;
   case "2013-3": $redirect="https://commons.wikimedia.org/wiki/File%3AAlba_Iulia_-_Catedrala_Incoronarii_si_Catedrala_Sfantul_Mihail.jpg";break;
   case "2013-4": $redirect="https://commons.wikimedia.org/wiki/File%3ASarmizegetusa_Regia_-_Sanctuarul_mare_circular._%28Zona_sacra%29.jpg";break;
   case "2013-5": $redirect="https://commons.wikimedia.org/wiki/File%3AArhiepiscopiei_Romanului_%C8%99i_Bac%C4%83ului..JPG";break;
   case "2013-6": $redirect="https://commons.wikimedia.org/wiki/File%3ACetatea_Enisala_Panorama.jpg";break;
   case "2013-7": $redirect="https://commons.wikimedia.org/wiki/File%3ASarmizegetusa_Regia_panorama_Incina_sacra.jpg";break;
   case "2013-8": $redirect="https://commons.wikimedia.org/wiki/File%3ABiserica_evanghelic%C4%83_fortificat%C4%83_sat_IACOBENI%3B_comuna_IACOBENI.jpg";break;
   case "2013-9": $redirect="https://commons.wikimedia.org/wiki/File%3ACastelul_Corvinestilor_-_vedere_lateral_stanga_frontala_de_langa_podul_de_acces.JPG";break;
   case "2013-10": $redirect="https://commons.wikimedia.org/wiki/File%3ACasa_Artelor_(fosta_Hala_a_Macelarilor)%2C_Sibiu.jpg";break;
   case "2012-1": $redirect="http://ro.wikipedia.org/wiki/Fi%C8%99ier:Brasov_-_Casa_Sfatului.jpg";break;
   case "2012-2": $redirect="http://commons.wikimedia.org/wiki/File:Flickr_-_fusion-of-horizons_-_Ateneul_Rom%C3%A2n_%284%29.jpg";break;
   case "2012-3": $redirect="http://commons.wikimedia.org/wiki/File:Biserica_%22Sf._Nicolae%22..jpg";break;
   case "2012-4": $redirect="http://commons.wikimedia.org/wiki/File%3ABiserica_de_lamn_tisa_halmagiu_arad_romania.JPG";break;
   case "2012-5": $redirect="http://commons.wikimedia.org/wiki/File%3ABiserica_%22Adormirea_Maicii_Domnului%22_Strei.jpg";break;
   case "2012-6": $redirect="http://commons.wikimedia.org/wiki/File%3ACarta-_intrare.jpg";break;
   case "2012-7": $redirect="http://commons.wikimedia.org/wiki/File%3ACasa_Muzeul_Astra.jpg";break;
   case "2012-8": $redirect="http://commons.wikimedia.org/wiki/File%3ACastelul_Bran%2C_cruce.jpg";break;
   case "2012-9": $redirect="http://commons.wikimedia.org/wiki/File%3ASchitul_Crasna-panorama_stitch12.jpg";break;
   case "2012-10": $redirect="http://commons.wikimedia.org/wiki/File%3ATurn_clopotni%C8%9B%C4%83.jpg";break;
   case "2011-1": $redirect="http://commons.wikimedia.org/wiki/File%3ATimisoara_-_Union_Square_at_sunrise.jpg";break;
   case "2011-2": $redirect="http://commons.wikimedia.org/wiki/File%3AInterior_al_Bisericii_parohiale_romano-catolice_%22Sf._Treime%22.jpg";break;
   case "2011-3": $redirect="http://commons.wikimedia.org/wiki/File%3ALa_Ruine_-_Bociulesti_-_Vedere_laterala.jpg";break;
   case "2011-4": $redirect="https://commons.wikimedia.org/wiki/File:Panorama_Centrul_Vechi_2.jpg";break;
   case "2011-5": $redirect="https://commons.wikimedia.org/wiki/File:Palatul_Micul_Trianon.jpg";break;
   case "2011-6": $redirect="https://commons.wikimedia.org/wiki/File:Poart%C4%83_-_Cetatea_Alba_Carolina.jpg";break;
   case "2011-7": $redirect="https://commons.wikimedia.org/wiki/File:Crucea_comemorativ%C4%83_a_Eroilor_rom%C3%A2ni_din_primul_r%C4%83zboi_mondial.jpg";break;
   case "2011-8": $redirect="https://commons.wikimedia.org/wiki/File:XIII_century_church_from_Densu%C5%9F.jpg";break;
   case "2011-9": $redirect="https://commons.wikimedia.org/wiki/File:De_paza.jpg";break;
   case "2011-10": $redirect="https://commons.wikimedia.org/wiki/File:M%C4%83n%C4%83stirea_Chiajna_-_Giule%C8%99ti.jpg";break;
   case "MS-II-a-A-15596": $redirect="https://commons.wikimedia.org/wiki/File:Ansamblul_bisericii_evanghelice_fortificate_din_Archita_MS-II-a-A-15596.JPG";break;
   case "CJ-II-m-A-07396": $redirect="https://commons.wikimedia.org/wiki/File:Biserica_Calvaria_de_la_Cluj-M%C4%83n%C4%83%C8%99tur,_vedere_sud-vestic%C4%83,_2014.JPG";break;
   case "BZ-IV-m-B-21096.02": $redirect="https://commons.wikimedia.org/wiki/File:RO_BZ_Gura_Siriului_World_War_I_heroes_cemetery_02.jpg";break;
   case "MS-II-m-A-15741": $redirect="https://commons.wikimedia.org/wiki/File:Ozd.JPG";break;
   case "BV-II-a-A-11745": $redirect="https://commons.wikimedia.org/wiki/File:Prejmer_-_Ansamblul_bisericii_evanghelice_fortificate_-_vedere_dinspre_nordest.jpg";break;
   case "SB-II-a-A-12341": $redirect="https://commons.wikimedia.org/wiki/File:Bruiu_-_Ansamblul_bisericii_evanghelice_fortificate.jpg";break;
   case "SB-II-a-A-12358": $redirect="https://commons.wikimedia.org/wiki/File:Ansamblul_Bisericii_Fortificate_%E2%80%9ESf._Mihail%E2%80%9D,_Cisnadioara.jpg";break;
   case "BZ-II-m-B-02374": $redirect="https://commons.wikimedia.org/wiki/File:Conacul_Procopie_Ca%C8%99ota_4.jpg";break;
   case "BV-II-m-A-11294.05": $redirect="https://commons.wikimedia.org/wiki/File:Turnul_alb_din_Bra%C8%99ov;_pe_fundal_se_vede_T%C3%A2mpa.jpg";break;
   case "GJ-II-m-B-09398": $redirect="https://commons.wikimedia.org/wiki/File:RO_GJ_Biserica_Sfantul_Ioan_din_Cojani_%2835%29.JPG";break;
   case "BZ-II-m-A-02323": $redirect="https://commons.wikimedia.org/wiki/File:Prim%C4%83ria_municipiului_Buz%C4%83u.jpg";break;
   case "BZ-II-a-B-02347": $redirect="https://commons.wikimedia.org/wiki/File:Ansamblul_conacului_Marghiloman_2.jpg";break;
   case "BZ-II-m-B-02347.01": $redirect="https://commons.wikimedia.org/wiki/File:Reflexia_Vilei_Albatros.JPG";break;
   case "TM-II-m-A-06176": $redirect="https://commons.wikimedia.org/wiki/File:Domul_romano-catolic_%E2%80%9ESf._Gheorghe%E2%80%9D.jpg";break;
   case "HD-II-m-A-03452": $redirect="https://commons.wikimedia.org/wiki/File:RO_HD_Biserica_Adormirea_Maicii_Domnului_din_Strei_%2827%29.JPG";break;
   case "TM-II-s-A-06095": $redirect="https://commons.wikimedia.org/wiki/File:Piata_Victoriei_%28Operei%29.jpg";break;
   case "BZ-II-a-A-02363": $redirect="https://commons.wikimedia.org/wiki/File:Fosta_m%C4%83n%C4%83stire_Berca_Biserica_%E2%80%9ESf._Arhangheli_Mihail_%C8%99i_Gavril%E2%80%9D_2.jpg";break;
   case "HD-II-m-A-03307": $redirect="https://commons.wikimedia.org/wiki/File:Biserica_%E2%80%9ESf._Nicolae%E2%80%9D_sat_Densu%C8%99,_comuna_Densu%C8%99,_jud._Hunedoara.jpg";break;
   case "SB-II-m-A-12564": $redirect="https://commons.wikimedia.org/wiki/File:%C8%98omartin_-_Biserica_evanghelic%C4%83.jpg";break;
   case "CJ-II-m-A-07801": $redirect="https://commons.wikimedia.org/wiki/File:Salina_Turda,_Mina_Terezia.JPG";break;
   case "BV-II-a-A-11755": $redirect="https://commons.wikimedia.org/wiki/File:Cetatea_R%C3%A2%C8%99novului;_pe_fundal_masivul_Post%C4%83varu_%C8%99i_Poiana_Bra%C8%99ov.jpg";break;
   case "CJ-II-m-A-07801-1": $redirect="https://commons.wikimedia.org/wiki/File:Salina_Turda_%28panorama%29,_Cluj,_RO.jpg";break;
   case "BZ-II-m-B-02472": $redirect="https://commons.wikimedia.org/wiki/File:Conacul_S%C4%83h%C4%83teni_la_amurg.jpg";break;
   case "HD-II-m-A-03344-1": $redirect="https://commons.wikimedia.org/wiki/File:Castelul_Corvinilor_din_Hunedoara_vedere_de_sus.jpg";break;
   case "AR-II-m-A-00624": $redirect="https://commons.wikimedia.org/wiki/File:Biserica_de_lemn_din_LuncsoaraAR.JPG";break;

   case "SV-II-a-A-05595": $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Putna";break;
   case "SV-II-a-A-05651": $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Sucevi%C8%9Ba";break;
   case "VN-IV-m-A-06631":   $redirect="https://ro.wikipedia.org/wiki/Mausoleul_de_la_M%C4%83r%C4%83%C8%99e%C8%99ti";break;
   case "SV-II-m-B-05545":   $redirect="https://ro.wikipedia.org/wiki/Galeria_Oamenilor_de_Seam%C4%83_din_F%C4%83lticeni";break;
   case "VN-II-m-A-06417":   $redirect="https://ro.wikipedia.org/wiki/Tribunalul_jude%C8%9Bean_Vrancea";break;
   case "TM-II-m-A-06176":   $redirect="https://ro.wikipedia.org/wiki/Pia%C8%9Ba_Unirii_din_Timi%C8%99oara";break;
   case "SB-II-m-A-12093":   $redirect="https://ro.wikipedia.org/wiki/Biserica_parohial%C4%83_romano-catolic%C4%83_din_Sibiu";break;
   case "NT-I-s-B-10528":    $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Bociule%C8%99ti";break;
   case "B-II-m-A-19650":    $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Chiajna";break;
   case "BV-II-m-A-11586":   $redirect="https://ro.wikipedia.org/wiki/Casa_Sfatului_din_Bra%C8%99ov";break;
   case "B-II-m-A-18789":    $redirect="https://ro.wikipedia.org/wiki/Ateneul_Rom%C3%A2n";break;
   case "GR-II-m-A-15004.02": $redirect="https://ro.wikipedia.org/wiki/Conacul_%C8%98tefan_Bellu_din_Gostinari";break;
   case "BV-II-a-A-11769":   $redirect="https://ro.wikipedia.org/wiki/Cetatea_Rupea";break;
   case "HD-II-m-A-03344":   $redirect="https://ro.wikipedia.org/wiki/Castelul_Hunedoarei";break;
   case "AB-II-m-A-00129":   $redirect="https://ro.wikipedia.org/wiki/Catedrala_Sf%C3%A2ntul_Mihail_din_Alba_Iulia";break;
   case "CT-I-m-A-02567.05": $redirect="https://ro.wikipedia.org/wiki/Tropaeum_Traiani";break;
   case "BV-I-m-A-11284.01": $redirect="https://ro.wikipedia.org/wiki/Cetatea_R%C3%A2%C8%99nov";break;
   case "BV-II-a-A-11768": $redirect="https://ro.wikipedia.org/wiki/Biserica_evanghelic%C4%83_fortificat%C4%83_din_Rotbav";break;
   case "HD-I-s-A-03190": $redirect="https://ro.wikipedia.org/wiki/Sarmizegetusa_Regia";break;
   case "BV-II-a-A-11412": $redirect="https://ro.wikipedia.org/wiki/Biserica_Neagr%C4%83";break;
   case "IF-II-m-A-15257.02": $redirect="https://ro.wikipedia.org/wiki/Palatul_%C8%98tirbei";break;
   case "SV-II-m-B-05593": $redirect="https://ro.wikipedia.org/wiki/Chilia_lui_Daniil_Sihastrul";break;
   case "AB-II-a-A-00196": $redirect="https://ro.wikipedia.org/wiki/Cetatea_din_C%C3%A2lnic";break;
   case "SB-II-a-A-12471": $redirect="https://ro.wikipedia.org/wiki/Biserica_evanghelic%C4%83_fortificat%C4%83_din_Mo%C8%99na";break;
   case "HD-II-m-A-03452": $redirect="https://ro.wikipedia.org/wiki/Biserica_Adormirea_Maicii_Domnului_din_Strei";break;
   case "expozitie": $redirect="http://wikilovesmonuments.ro/2014/10/invitatie-la-expozitia-wiki-loves-monuments/";break;
         default: $redirect="https://commons.wikimedia.org/wiki/Category:Images_from_Wiki_Loves_Monuments_2015_in_Romania";
   }


$string="[$date]  [LMI=$lmi] [REDIRECT=$redirect] [$ip]  [$ua]\n";

//echo $string;

file_put_contents($file, $string, FILE_APPEND);

header("Location:". $redirect); /* Redirect browser */

exit;


?>

