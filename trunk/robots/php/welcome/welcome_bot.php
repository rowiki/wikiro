<?

  /**
  * WelcomeBot originally written for the Romanian Wikipedia
  * Requires SxWiki.php
  *
  * Copyright (C) 2008 Bogdan Stancescu <bogdan@moongate.ro>
  * aka Gutza, http://ro.wikipedia.org/wiki/Utilizator:Gutza
  *
  *  This program is free software; you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation version 2.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License along
  *  with this program; if not, write to the Free Software Foundation, Inc.,
  *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */

  $username="";
  $password="";

  $wikipedia="http://ro.wikipedia.org";
  $prefix="Special:Contribu".chr(197).chr(163)."ii";
  $disc="Discu%C5%A3ie_Utilizator";

  include("SxWiki.php"); //Include framework
  $url = $wikipedia.'/w/';
  $sxLgTA = sxLogin($username,$password); //Log in; dies on failure

  $html=sxGetUrl($wikipedia.'/w/index.php?title=Special:Jurnal&type=newusers&limit=100');

  //echo "HTML:\n$html\n\n------------------\n";


  //preg_match_all("|/w/index.php\\?title=$prefix\:(.*)&amp;action=edit|",$html,$matches);
  //preg_match_all("/ title=\"$prefix\/([^\"]+)\">/",$html,$matches);
  preg_match_all("/<li>(.*)<\/li>/",$html,$entries);

  $sxLgID = $sxLgTA[uid];
  $sxLgT = $sxLgTA[token];
  $maxlag = "5"; //Set maxlag to 5
  $epm = setEPM(10); //set 10 edits per min.
  $content='{{subst:bv}} ~~~~~';

  for($i=count($entries[1])-1;$i>-1;$i--) {
    $entry=trim($entries[1][$i]);
    if (!preg_match("|/w/index.php\\?title=$disc\:(.*)&amp;action=edit|",$entry,$matches)) {
      continue;
    }
    $user=$matches[1];
    $send=false;
    if (strstr($entry,"Utilizator nou")) {
      // Local user, send unconditionally
      $send=true;
    } elseif (strstr($entry,"Cont creat automat")) {
      // SUL user, check if they have contributions
      if (preg_match("/ title=\"$prefix\/([^\"]+)\">/",$entry)) {
        $send=true;
      }
    } else {
      echo "Unknown entry type, ignoring: $entry\n";
      continue;
    }
    if (!$send) {
      continue;
    }
    //echo "Sending welcome message to user $user\n";
    //continue;
    set_time_limit(30);
//    echo "[$i] Welcoming ".$matches[1][$i]."... ";
    $r=sxPutPage("Discu".chr(197).chr(163)."ie Utilizator:".urldecode($user), "bun venit", $content, 0);
//    echo "ok.\n";
  }

?>
