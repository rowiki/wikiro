<?php
$file = 'redirect-log.txt';

$lmi=$_REQUEST["lmi"];
$ip=$_SERVER['REMOTE_ADDR'];
$ua = $_SERVER['HTTP_USER_AGENT'];
$date= date("Y/m/d:h:i:sa");

 switch ($lmi) {
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
   case "BV-II-a-A-11768": $redirect="https://ro.wikipedia.org/wiki/Rotbav,_Bra%C8%99ov";break;
   case "HD-I-s-A-03190": $redirect="https://ro.wikipedia.org/wiki/Sarmizegetusa_Regia";break;
   case "BV-II-a-A-11412": $redirect="https://ro.wikipedia.org/wiki/Biserica_Neagr%C4%83";break;
   case "IF-II-m-A-15257.02": $redirect="https://ro.wikipedia.org/wiki/Palatul_%C8%98tirbei";break;
   case "SV-II-m-B-05593": $redirect="https://ro.wikipedia.org/wiki/Chilia_lui_Daniil_Sihastrul";break;
   case "AB-II-a-A-00196": $redirect="https://ro.wikipedia.org/wiki/Cetatea_din_C%C3%A2lnic";break;
   case "SB-II-a-A-12471": $redirect="https://ro.wikipedia.org/wiki/Mo%C8%99na,_Sibiu";break;
   case "HD-II-m-A-03452": $redirect="https://ro.wikipedia.org/wiki/Biserica_Adormirea_Maicii_Domnului_din_Strei";break;
   case "expozitie": $redirect="http://wikilovesmonuments.ro/2014/10/invitatie-la-expozitia-wiki-loves-monuments/";break;
         default: $redirect="https://commons.wikimedia.org/wiki/Category:Images_from_Wiki_Loves_Monuments_2014_in_Romania";
   }


$string="[$date]  [LMI=$lmi] [REDIRECT=$redirect] [$ip]  [$ua]\n";

//echo $string;

file_put_contents($file, $string, FILE_APPEND);

header("Location:". $redirect); /* Redirect browser */

exit;


?>

