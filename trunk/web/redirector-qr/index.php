<?php
$file = 'redirect-log.txt';

$lmi=$_REQUEST["lmi"];
$ip=$_SERVER['REMOTE_ADDR'];
$ua = $_SERVER['HTTP_USER_AGENT'];
$date= date("Y/m/d:h:i:sa");

 switch ($lmi) {
	 case "BV-II-a-A-11769": $redirect="https://commons.wikimedia.org/wiki/File:Cetatea_Rupea,_judetul_Brasov.jpg";break;
 	 case "HD-II-m-A-03344": $redirect="https://commons.wikimedia.org/wiki/File:Castelul_Corvinestilor_-_vedere_median-frontala_de_pe_podul_de_acces.jpg";break;
	 case "AB-II-m-A-00129": $redirect="https://commons.wikimedia.org/wiki/File%3AAlba_Iulia_-_Catedrala_Incoronarii_si_Catedrala_Sfantul_Mihail.jpg";break; 	 
         default: $redirect="https://commons.wikimedia.org/wiki/Category:Images_from_Wiki_Loves_Monuments_2014_in_Romania";
	 } 


$string="[$date]  [LMI=$lmi] [REDIRECT=$redirect] [$ip]  [$ua]\n";

//echo $string;

file_put_contents($file, $string, FILE_APPEND);

header("Location:". $redirect); /* Redirect browser */

exit;


?>

