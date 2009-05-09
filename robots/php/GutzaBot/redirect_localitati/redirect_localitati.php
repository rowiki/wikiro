<?
  /*
  *    Copyright (c) 2008 Bogdan Stancescu aka Gutza
  *    http://ro.wikipedia.org/wiki/Utilizator:Gutza
  *
  *    This program is free software; you can redistribute it and/or modify
  *    it under the terms of the GNU General Public License as published by
  *    the Free Software Foundation version 2.
  *
  *    This program is distributed in the hope that it will be useful,
  *    but WITHOUT ANY WARRANTY; without even the implied warranty of
  *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *    GNU General Public License for more details.
  *
  *    You should have received a copy of the GNU General Public License along
  *    with this program; if not, write to the Free Software Foundation, Inc.,
  *    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */

  if(isset($_SERVER['REQUEST_METHOD'])) {
    echo "This should only run via console. Exiting.\n";
    exit;
  }

  $createDisambig=true;
  if ($createDisambig) {
    $tofix=explode("\n",file_get_contents('tofix.txt'));
    foreach($tofix as $k=>$v) {
      $tofix[$k]=trim($v);
    }
    @unlink("disambig.txt");
  }
  set_time_limit(0);
  $rarr=chr(hexdec('e2')).chr(hexdec('86')).chr(hexdec('92'));

  require_once('SxWikiPro.php');
  $sx=new SxWiki;
  $sx->configFile='/home/bogdan/sxwiki.cnf';
  $sx->retries=10;
  //$sx->verbosity=100;

  $fp=fopen("out.txt",'w');
  if (!$fp) {
    exit;
  }

  echo "Retrieving metalist... ";
  $metalist=$sx->getPage(urldecode("Proiect:Localit%C4%83%C5%A3ile din Rom%C3%A2nia/ghid alfabetic"));
  if (!$metalist) {
    echo "Failed!\n";
    exit;
  }
  if (!preg_match_all("/\[\[(Wikipedia:Proiectul local[^|]+)\|[^\]]+\]\]/",$metalist,$matches)) {
    echo "Metalist improperly formatted!\n";
    exit;
  }
  $urls=array();
  foreach($matches[1] as $match) {
    $urls[]=$sx->toURL($match);
  }
  if ($urls) {
    echo "Ok\n";
  } else {
    echo "Somehow failed to retrieve the final metalist data.\n";
    exit;
  }

  $baseURL=substr($sx->_getBaseURL(),0,-2)."wiki/";
  $total=count($urls);
  for($ctr=0;$ctr<$total;$ctr++) {
    $url=$urls[$ctr];
    echo "Retrieving list ".($ctr+1)."/".$total." (".$sx->fromURL($url).")... ";
    $url=$baseURL.$url;
    $list=$sx->getURL($url);
    if (!$list) {
      echo "Failed!\n";
    }
    if (!($pos=strpos($list,"tipul"))) {
      echo "Improperly formatted (was expecting to find \"tipul\" in the text!\n";
      exit;
    }
    echo "Ok\n";
    $list=substr($list,$pos);
    $list=substr($list,strpos($list,"<tr"));
    $list=substr($list,0,strpos($list,"</table>")+1);
    echo "Parsing list... ";
    $entries=$duplicates[]=$unique=array();
    $problems=false;
    while(strpos($list,"<tr")!==false) {
      $list=substr($list,1);
      $entry=substr($list,0,strpos($list,"</tr"));
      $list=substr($list,strpos($list,"</tr")+6);
      if (!preg_match_all("/<a href=\"(\/w[^\"]+)/",$entry,$matches)) {
        // breaking line in the list
        continue;
      }
      if (count($matches[1])<6) {
        if (!$problems) {
          echo "\n";
          $problems=true;
        }
        echo "  (!) Mismatch, will skip (expected at least six links per line): ";
        var_dump($matches[1]);
        continue;
      }

      $redirect=$matches[1][2];
      if (substr($redirect,0,6)=='/wiki/') {
        // redirect already exists, skipping
        continue;
      }
      $redirect=article_from_link($redirect);
      if (strpos($redirect,"(")) {
        // we redirect would contain brackets, skipping
        continue;
      }

      $article=$matches[1][0];
      if (substr($article,0,6)!='/wiki/') {
        // article doesn't exist (!!), skipping
        continue;
      }
      $article=article_from_link($article);

      $disambig=article_from_link($matches[1][4]);

      if (!$article || !$redirect || !$disambig) {
        if (!$problems) {
          echo "\n";
          $problems=true;
        }
        echo "  (!) Problem with $article/$redirect/$disambig, skipping!\n";
        continue;
      }

      $entries[]=array(
        'article'=>$article,
        'redirect'=>$redirect,
        'disambig'=>$disambig
      );
      if (in_array($redirect,$unique)) {
        $duplicates[]=$redirect;
      } else {
        $unique[]=$redirect;
      }
    }
    if (!$problems) {
      echo "Ok\n";
    } else {
      echo "Done parsing list list, but found problems (see above).\n";
    }
    echo "Processing list... ";
    $current_duplicates=array();
    $lastEntry='';
    $lastWasDuplicate=false;
    foreach($entries as $entry) {
      if ($lastWasDuplicate) {
        if ($entry['redirect']!=$lastEntry['redirect']) {
          fputs($fp,"# [[".$lastEntry['redirect']."]] $rarr [[".$lastEntry['disambig']."]] ([[".
            implode("]]; [[",$current_duplicates).
            "]])\n"
          );

          // Create disambig article if requested
          if ($createDisambig) {
            if (in_array($lastEntry['disambig'],$tofix)) {
              $d_c='';
            } else {
              $d_c=$sx->getPage($lastEntry['disambig']);
            }
            if ($d_c===false) {
              echo "Problem getting content for ".$lastEntry['disambig']."\n";
            } elseif (!$d_c) {
              $content="'''".$lastEntry['redirect']."''' se poate referi la:\n".
                "*[[".implode("]]\n*[[",$current_duplicates)."]]\n".
                "{{dezambiguizare}}";
              if (
                $sx->putPage(
                  $lastEntry['disambig'],
                  "Dezambiguare automata ([[Utilizator:GutzaBot/GB01|GB01]])",
                  $content,
                  false
                )
              ) {
                echo "Created disambiguation page ".$lastEntry['disambig']."\n";
                $fp2=fopen("disambig.txt",'a');
                fputs($fp2,$lastEntry['disambig']."\n");
                fclose($fp2);
              } else {
                echo "Failed creating disambiguation page ".
                  $lastEntry['disambig']."\n";
              }
            }
          }
          $current_duplicates=array();
        }
      }
      $lastEntry=$entry;
      if (!in_array($entry['redirect'],$duplicates)) {
        fputs($fp,"# [[".$entry['redirect']."]] $rarr [[".$entry['article']."]]\n");
        $current_duplicates=array();
        $lastWasDuplicate=false;
      } else {
        $current_duplicates[]=$entry['article'];
        $lastWasDuplicate=true;
      }
    }
    echo "Ok\n";
  }

  function article_from_link($link)
  {
    if(substr($link,0,6)=='/wiki/') {
      return SxWiki::fromURL(substr($link,6));
    }
    if (!preg_match("/\/w\/index\.php\?title=([^&]+)&amp;action=edit&amp;redlink=1/",$link,$matches)) {
      return false;
    }
    return SxWiki::fromURL($matches[1]);
  }
?>
