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

  // 0=no output
  // 1=casual progress monitoring
  // 2=debugging
  define('DEBUG_LEVEL',0);

  $lock_file=dirname(__FILE__)."/make_redirect.lock";

  $lock=fopen($lock_file,'w+');
  if (!$lock) {
    echo "Failed opening lock file $lock_file, exiting!\n";
    exit;
  }
  if (!flock($lock,LOCK_EX + LOCK_NB)) {
    debug("Another instance already running, exiting.");
    exit;
  }
  debug("No other instance running, starting execution.",2);

  if(isset($_SERVER['REQUEST_METHOD'])) {
    echo "This should only run via console. Exiting.\n";
    exit;
  }

  set_time_limit(0);

  require_once('SxWikiPro.php');
  $sx=new SxWiki();
  $sx->configFile='/home/bogdan/sxwiki.cnf';
  $sx->retries=10;
  if (DEBUG_LEVEL) {
    $sx->verbosity=DEBUG_LEVEL-1;
  }
  if (!$sx->login()) {
    echo "Login failed, exiting!\n";
    exit;
  }

  $statusFile=dirname(__FILE__)."/make_redir_status.csv";
  $oldenStatus=readOldStatus();

  $output=processPage($sx->fromURL("Utilizator:GutzaBot/Creeaz%C4%83_redirec%C5%A3ion%C4%83ri"));
  writeOldStatus($oldenStatus);
  if (!$output) {
    debug("No output; clean exit.");
    exit;
  }
  debug("Output:\n$output");
  debug("Saving output...");
  $pageName=$sx->fromURL("Discu%C5%A3ie_Utilizator:GutzaBot/Creeaz%C4%83_redirec%C5%A3ion%C4%83ri");
  $old=$sx->getPage($pageName);
  if (!$old) {
    echo "Failed retrieving old page content!\n";
    exit;
  }
  $sx->putPage($pageName,"Actualizare automata",$old."\n----\n'''~~~~~'''\n$output\n",false);
  debug("Clean exit.");

  function processPage($pageName)
  {
    debug("Starting to process page $pageName...");
    $output='';
    global $sx,$oldenStatus;
    debug("Checking whether page is protected... ",2);
    if (!$sx->isPageProtected($pageName)) {
      debug("Page is not protected, skipping it.");
      return NULL;
    }
    debug("Page is protected, processing it.",2);
    debug("Retrieving page info... ",2);
    $info=$sx->getPageInfo($pageName);
    debug("Done.",2);
    if ($info["lastrevid"]==$oldenStatus[$pageName]) {
      debug("Page hasn't changed since last read, only processing includes.");
      $newLinks=false;
    } else {
      debug("Page has changed since last read, processing both includes and links.");
      $newLinks=true;
    }
    $oldenStatus[$pageName]=$info["lastrevid"];
    debug("Retrieving page content... ",2);
    $pageContent=$sx->getPage($pageName);
    debug("Done.",2);
    if (!$pageContent) {
      echo "Failed retrieving page content in $pageName!\n";
      return false;
    }

    if ($newLinks) {
      // Process local directives
      $tmpOut=processArticles($pageContent);
      if ($tmpOut===false) {
        echo "Failed processing articles!\n";
        return false;
      }
      $output.=$tmpOut;
    }

    // Process includes
    $tmpOut=processIncludes($pageContent);
    if ($tmpOut===false) {
      echo "Failed processing imports!\n";
      return false;
    }
    $output.=$tmpOut;
    debug("Finished processing page $pageName.");
    return $output;
  }

  function processArticles($content)
  {
    debug("Starting to process links...");
    global $sx;
    $rarr=chr(hexdec('e2')).chr(hexdec('86')).chr(hexdec('92'));
    $oneLink="\[\[([^\|\]]+)(?:\|[^\]]+)?\]\]";
    if (!preg_match_all(
      "/[*#][\s]*".$oneLink."[\s]*".$rarr."[\s]*".$oneLink."/",
      $content,
      $matches
    )) {
      debug("No links found in this page.");
      return NULL;
    }

    $output='';
    foreach($matches[1] as $key=>$match) {
      $redirectFrom=$match;
      $redirectTo=$matches[2][$key];
      $content=$sx->getPage($redirectFrom);
      if ($content===false) {
        echo "Failed retrieving content for page $redirectFrom!\n";
        continue;
      }
      if (trim($content)) {
        // "redirect" page exists
        continue;
      }
      $content=$sx->getPage($redirectTo);
      if (!trim($content)) {
        // "article" page doesn't exist
        continue;
      }
      $output.="# ".$redirectFrom." ".$rarr." ".$redirectTo."\n";
      debug("Creating redirect article $redirectFrom pointing to $redirectTo.");
      $sx->putPage(
        $redirectFrom,
        "Redirectionare automata ([[Utilizator:GutzaBot/GB02|GB02]])",
        "#REDIRECT [[$redirectTo]]",
        false
      );
    }
    debug("Finished processing links.");
    return $output;
  }

  function processIncludes($content)
  {
    debug("Processing includes...");
    if (!preg_match_all(
      "/[\s]*\{\{([^\}]+)\}\}/",
      $content,
      $matches
    )) {
      debug("No includes in this page.");
      return NULL;
    }
    foreach($matches[1] as $match) {
      $tmpOut=processPage($match);
      if ($tmpOut===false) {
        return false;
      }
      $output.=$tmpOut;
    }
    debug("Finished processing includes.");
    return $output;
  }

  function readOldStatus()
  {
    global $statusFile;
    if (!is_file($statusFile)) {
      return NULL;
    }
    $lines=file($statusFile);
    $data=array();
    foreach($lines as $line) {
      list($file,$rev)=explode("\t",$line);
      $data[$file]=trim($rev);
    }
    return $data;
  }

  function writeOldStatus($data)
  {
    global $statusFile;
    $fp=fopen($statusFile,'w');
    if (!$fp) {
      return false;
    }
    foreach($data as $file=>$rev) {
      if (!fputs($fp,$file."\t".$rev."\n")) {
        return false;
      }
    }
    fclose($fp);
    return true;
  }

  function debug($message,$level=1)
  {
    if (DEBUG_LEVEL<$level) {
      return NULL;
    }
    echo $message."\n";
  }
?>
