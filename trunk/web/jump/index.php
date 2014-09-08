<?php
//jump to a Commons link while logging the access
$file = 'jump-log.txt';

$go=$_REQUEST["go"];
$ip=$_SERVER['REMOTE_ADDR'];
$ua = $_SERVER['HTTP_USER_AGENT'];
$date= date("Y/m/d:h:i:sa");

//check if redirecting to commons
if ((strpos('1'.$go, 'https://commons.wikimedia.org/wiki/')=="1")||(strpos('1'.$go, 'http://commons.wikimedia.org/wiki/')=="1"))
 { $redirect=$go; } 
else 
 { $redirect="https://commons.wikimedia.org/wiki/Category:Images_from_Wiki_Loves_Monuments_2014_in_Romania"; }

$string="[$date] [REDIRECT=$redirect] [$ip] [$ua]\n";

echo $string;

file_put_contents($file, $string, FILE_APPEND);

header("Location:". $redirect); /* Redirect browser */

exit;


?>
