<?php
$file = 'redirect-log.txt';

$lmi=$_REQUEST["lmi"];
$ip=$_SERVER['REMOTE_ADDR'];
$ua = $_SERVER['HTTP_USER_AGENT'];
$date= date("Y/m/d:h:i:sa");

 switch ($lmi) {
	 case "SV-II-a-A-05595": $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Sucevi%C8%9Ba";break;
	 case "SV-II-a-A-05595-1": $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Sucevi%C8%9Ba";break;  
   case "VN-IV-m-A-06632":   $redirect="https://ro.wikipedia.org/wiki/Mausoleul_de_la_M%C4%83r%C4%83%C8%99e%C8%99ti";break;
   case "SV-II-m-B-05545":   $redirect="https://commons.wikimedia.org/wiki/File:Galeria_Oamenilor_de_Seama_Falticeni.JPG";break;
   case "VN-II-m-A-06417":   $redirect="https://commons.wikimedia.org/wiki/File:Tribunalul_Judetean_Vrancea,_Focsani.JPG";break;
   case "TM-II-m-A-06176":   $redirect="https://ro.wikipedia.org/wiki/Pia%C8%9Ba_Unirii_din_Timi%C8%99oara";break;
   case "SB-II-m-A-12093":   $redirect="https://commons.wikimedia.org/wiki/File:Interior_al_Bisericii_parohiale_romano-catolice_%22Sf._Treime%22.jpg";break;
   case "NT-I-s-B-10528":    $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Bociule%C8%99ti";break;
   case "B-II-m-A-19650":    $redirect="https://ro.wikipedia.org/wiki/M%C4%83n%C4%83stirea_Chiajna";break;
   case "BV-II-m-A-11586":   $redirect="https://commons.wikimedia.org/wiki/File:Brasov_-_Casa_Sfatului.jpg";break;
   case "B-II-m-A-18789":    $redirect="https://ro.wikipedia.org/wiki/Ateneul_Rom%C3%A2n";break;
   case "GR-II-m-A-15004.02": $redirect="https://commons.wikimedia.org/wiki/File:Biserica_%22Sf._Nicolae%22..jpg";break;
   case "BV-II-a-A-11769":   $redirect="https://ro.wikipedia.org/wiki/Cetatea_Rupea";break;
   case "HD-II-m-A-03344":   $redirect="https://ro.wikipedia.org/wiki/Castelul_Hunedoarei";break;
   case "AB-II-m-A-00129":   $redirect="https://ro.wikipedia.org/wiki/Catedrala_Sf%C3%A2ntul_Mihail_din_Alba_Iulia";break;
         default: $redirect="https://commons.wikimedia.org/wiki/Category:Images_from_Wiki_Loves_Monuments_2014_in_Romania";
	 } 


$string="[$date]  [LMI=$lmi] [REDIRECT=$redirect] [$ip]  [$ua]\n";

//echo $string;

file_put_contents($file, $string, FILE_APPEND);

header("Location:". $redirect); /* Redirect browser */

exit;


?>

