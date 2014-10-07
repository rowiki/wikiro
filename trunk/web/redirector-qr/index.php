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
         default: $redirect="https://commons.wikimedia.org/wiki/Category:Images_from_Wiki_Loves_Monuments_2014_in_Romania";
	 } 


$string="[$date]  [LMI=$lmi] [REDIRECT=$redirect] [$ip]  [$ua]\n";

//echo $string;

file_put_contents($file, $string, FILE_APPEND);

header("Location:". $redirect); /* Redirect browser */

exit;


?>

