<?

  /*
  * Robotul Arhivar -- arhiveaza pagini de discutie la Wikipedia
  *
  * Licenta: GPLv3 sau urmatoarele (http://www.gnu.org/copyleft/gpl.html)
  * Autor: Bogdan Stancescu (http://ro.wikipedia.org/wiki/Utilizator:Gutza)
  * Prima versiune: noiembrie 2008
  * Versiunea curenta: 1.3, 10 decembrie 2008
  */

  $test="ps -ef | grep RobotulArhivar | grep php | grep -v ".getmypid();

  if (trim(shell_exec($test))) {
    echo "This script seems to be running already, exiting.\n";
    exit;
  }

  require_once("SxWiki.php");
  require_once("RAlib.php");

  $sx=new SxWiki;
  //$sx->verbosity=4;
  $sx->configFile="ra.cfg";
  $levelCodes=array('','<anul>','<luna>','<ziua>');

  $articles=$sx->getCat("Pagini arhivate automat");
  if ($sx->errors) {
    exit;
  }

  foreach($articles as $article) {
    process_article($article);
  }

  function process_article($article)
  {
    global $sx, $month_names, $months;
    
    $cur_date=time();
    $orig_month_name=date('F',$cur_date);
    $local_month_name=array_search($orig_month_name,$months);
    $full_ts=date('j F Y H:i (T)',$cur_date);
    $full_ts=str_replace($orig_month_name,$local_month_name,$full_ts);
    $tag_message="<!-- Vazut la $full_ts. --Robotul Arhivar -->\n";

    $source=$sx->getPage($article);
    $sx->errors=array();
    if (!$source) {
      return false;
    }

    // Identify sections
    $pattern="/.*(==(.*?)==).*/";
    if (!preg_match_all($pattern,$source,$matches)) {
      return false;
    }
    $section_meta=array();
    foreach($matches[0] as $key=>$section) {
      $section=rtrim($section);
      if (substr($section,-2)!=='==' || substr($section,0,2)!='==') {
        continue;
      }
      if (substr($section,-3)=='===' && substr($section,0,3)=='===') {
        continue;
      }
      $section_meta[]=array(
        "line"=>$matches[0][$key],
        "title"=>trim($matches[2][$key])
      );
    }

    // Process content
    $working_source=$source;
    $final_source='';
    $archive_source=array();
    $section_index=0;
    $tag=false;
    $first_section=true;
    foreach($section_meta as $key=>$meta) {
      $section_line=$meta['line'];
      $section_title=$meta['title'];
      //echo "-------------\nSection name: ".$section_title."\n";
      $pos=strpos($working_source,$section_line);
      if ($first_section) {
        $first_section=false;
        $final_source=substr($working_source,0,$pos);
        list($config,$level)=process_options($final_source,$article);
        if (!$config) {
          return false;
        }
      }
      $working_source=$section_source=substr($working_source,$pos);
      if ($key<count($section_meta)-1) { // if not last section
        $pos=strpos($section_source,$section_meta[$key+1]['line'],strlen($section_line));
        $section_source=substr($section_source,0,$pos);
      }
      $source_start=ltrim(substr($working_source,strlen($section_line)));
      $force_no_archive=(substr($source_start,0,strlen(NO_ARCHIVE_TEMPLATE))==NO_ARCHIVE_TEMPLATE);
      //echo "Section content:\n".$section_source."\n";
      $timestamps=preg_match_all("/([0-9]{1,2} )([a-z]{3,10})( \d\d\d\d \d\d:\d\d )\((EE[S]?T)\)/",$section_source,$ts);
      $max_date=0;
      foreach($ts[0] as $ts_key=>$ts_full) {
        $ts_str=$ts[1][$ts_key].$months[$ts[2][$ts_key]].$ts[3][$ts_key];
        $ts_date=strtotime($ts_str);
        if ($ts_date>$cur_date) {
          continue;
        }
        $max_date=max($max_date,$ts_date);
      }
      //echo "Section $article#$section_title max date: ".date('r',$max_date)."\n";

      if (!$max_date || $max_date+$config['age']>=$cur_date || $force_no_archive) {
        $final_source.=rtrim($section_source)."\n";
        if (!$timestamps) {
          $final_source.=$tag_message;
          $tag=true;
        }
        $final_source.="\n";
      } else {
        list($year,$month,$date)=explode(" ",date('Y n j',$max_date));
        $archive_source[$year][$month][$date][]=array(
          'index'=>$section_index,
          'content'=>$section_source,
          'title'=>$section_title
        );
        $section_index++;
      }

      $working_source=substr($working_source,strlen($section_source));
    }

    $all_archive_titles=array();
    while($archive_source) {
      $myArchive=&$archive_source;
      $myArchiveID=array();
      for($i=0;$i<$level;$i++) {
        $myArchiveID[]=$tmpKey=array_shift(array_keys($myArchive));
        $myArchive=&$myArchive[$tmpKey];
      }
      $archive_page=$config["archive"];
      // Fugly; the only alternative I could think of was an eval() and I don't like doing that.
      $force=true;
      switch($level) {
        case 3:
          unset($archive_source[$myArchiveID[0]][$myArchiveID[1]][$myArchiveID[2]]);
          $archive_page=str_replace("<ziua>",$myArchiveID[2],$archive_page);
          $force=false;
        case 2:
          if ($force || !$archive_source[$myArchiveID[0]][$myArchiveID[1]]) {
            unset($archive_source[$myArchiveID[0]][$myArchiveID[1]]);
          }
          $archive_page=str_replace("<luna>",$month_names[$myArchiveID[1]-1],$archive_page);
          $force=false;
        case 1:
          if ($force || !$archive_source[$myArchiveID[0]]) {
            unset($archive_source[$myArchiveID[0]]);
          }
          $archive_page=str_replace("<anul>",$myArchiveID[0],$archive_page);
          $force=false;
        case 0:
          if ($force || !$archive_source) {
            unset($archive_source);
          }
      }
      do {
        $sx->errors=array();
        $archive_text=$sx->getPage($archive_page);
      } while ($sx->errors);

      if ($archive_text) {
        $archive_text.="\n";
      }
      list($this_archive_text,$these_archive_titles)=array_values(multimplode($myArchive));
      $all_archive_titles=array_merge($all_archive_titles,$these_archive_titles);
      if ($config['beginning']) {
        $pos=strpos($archive_text,"__TOC__");
        if ($pos===false) {
          $archive_text=$this_archive_text.$archive_text;
        } else {
          $archive_pre=substr($archive_text,0,$pos+strlen("__TOC__")+1);
          $archive_post="\n\n".trim(substr($archive_text,strlen($archive_pre)));
          $archive_text=$archive_pre."\n".$this_archive_text.$archive_post;
        }
      } else {
        $archive_text.=$this_archive_text;
      }

      $this_archive_summary=make_archive_summary($these_archive_titles);

      do {
        $sx->errors=array();
        $sx->putPage($archive_page,"+arhivat".$this_archive_summary,$archive_text);
      } while ($sx->errors);
    }

    $summary=array();
    $minor=true;
    if ($archive_text) {
      $full_archive_summary=make_archive_summary($all_archive_titles);
      $summary[]='-arhivat'.$full_archive_summary;
      $minor=false;
    }
    if ($tag) {
      $summary[]='+marcat';
    }
    $summary=implode(', ',$summary);
    if (!$summary) {
      return NULL;
    }
    if (isset($config['force_minor'])) {
      $minor=(bool) $config['force_minor'];
    }
    do {
      $sx->errors=array();
      $sx->putPage($article,$summary,$final_source,$minor);
    } while ($sx->errors);
  }

  function parse_age($age_text)
  {
    if (!preg_match("/^([0-9]+)[ ]*([zsla])$/",$age_text,$matches)) {
      return false;
    }
    
    switch($matches[2]) {
      case 'z':
        return $matches[1]*DAY_IN_SECONDS;
      case 's':
        return $matches[1]*DAY_IN_SECONDS*7;
      case 'l':
        return $matches[1]*DAY_IN_SECONDS*31;
      case 'a':
        return $matches[1]*DAY_IN_SECONDS*365;
    }
  }

  function multimplode($structure)
  {
    $array=flatten($structure);
    $final=array();
    $titles=array();
    foreach($array as $element) {
      $final[$element['index']]=$element['content'];
      $titles[$element['index']]=$element['title'];
    }
    ksort($final,SORT_NUMERIC);
    ksort($titles,SORT_NUMERIC);
    return array(
      'text'=>implode('',$final),
      'titles'=>$titles
    );
  }

  function flatten($structure,$nesting=0)
  {
    $result=array();
    foreach($structure as $element) {
      if (!isset($element['index'])) {
        $result=array_merge($result,flatten($element));
      } else {
        $result=array_merge($result,array($element));
      }
    }
    return $result;
  }

  function process_options($source,$article)
  {
    global $levelCodes;
    $pattern="/\\{\\{Utilizator:Robotul[ _]Arhivar\\/config((\\|.*?=.*?)*)\\}\\}/";
    if (!preg_match_all($pattern,str_replace("\n","",$source),$options)) {
      echo "Archive configuration not found in $article!\n";
      return;
    }
    $options=explode("|",$options[1][0]); // only the first template
    $config=array();
    foreach($options as $option) {
      list($key,$value)=explode("=",$option);
      $key=trim($key);
      $value=trim($value);
      if (!$key || !strlen($value)) {
        continue;
      }
      //echo "$key -> $value\n";
      switch($key) {
        case 'vechime':
          $config['age']=parse_age($value);
          break;
        case 'arhiva':
          $config['archive']=$value;
          break;
        case 'minor':
          if ($value=='auto') {
            break;
          }
          $config['force_minor']=$value;
          break;
        case 'mod':
          if ($value=='prefix') {
            $config['beginning']=true;
          }
          break;
      }
    }
    if (!$config['age']) {
      echo "Age not specified or malformed in $article!\n";
      return false;
    }
    if (!$config['archive']) {
      echo "Archive not specified or malformed in $article!\n";
      return false;
    }

    // What, I can't have fun anymore?
    $complete=$level=0;
    for($i=0;$i<count($levelCodes)-1;$i++) {
      if (strstr($config['archive'],$levelCodes[$i+1])) {
        $complete|=1<<$i;
        $level=$i+1;
      }
    }
    if ($level && strstr(decbin($complete),'0')) {
      echo "Incomplete date specification in $article ({$config['archive']}).\n";
      return false;
    }

    return array($config,$level);;
  }

  function make_archive_summary($titles)
  {
    $this_archive_summary=implode("; ",$titles);
    if (strlen($this_archive_summary)>150) {
      $this_archive_summary=count($titles).": ".
        substr($this_archive_summary,0,145)."...";
    }
    return " (".$this_archive_summary.")";
  }
?>
