<?
  require_once('wv_lib.php');

  $interactive=false;

  $base_query="
    FROM
      diffs
    WHERE
      modifiedon_utc='2000-01-01 00:00:00' AND
      successive=1 AND
      curid>0
  ";

  $count_query="
    SELECT
      COUNT(*)".
    $base_query;

  $fetch_query="
    SELECT
      id,curid,new_id,old_id,title".
    $base_query;

  if ($interactive) {
    $h=wv_query($count_query);
    list($total)=mysql_fetch_row($h);
    //echo "Total: $total\n";
  }

  $h=wv_query($fetch_query);

  if ($interactive) {
    echo "Starting on ".date('r')."\n";
    $start_time=time();
  }

  for($i=1;$a=mysql_fetch_array($h);$i++) {
    set_time_limit(5);

    if ($interactive) {
      $timediff=time()-$start_time;
      $est_end=round(($timediff/$i)*($total-$i))+time();
      echo "\r$i/$total -- est end ".date('r',$est_end);
    }

    $url='http://ro.wikipedia.org/w/api.php?action=query&revids='.$a['new_id'].'&prop=revisions&rvprop=timestamp|user&format=php';
    $out=@file_get_contents($url);
    $data=unserialize($out);

    if (!$out || !$data) {
      echo "No data from $url:\n$out\n";
      continue;
    }
    if ($data['error']) {
      echo "Got error \"{$data['error']['code']}\" from $url:\n{$data['error']['info']}\n";
      continue;
    }
    //echo "Got data from $url:\n"; var_dump($data);
    $date=$data['query']['pages'][$a['curid']]['revisions'][0]["timestamp"];
    $date=str_replace('T',' ',str_replace('Z','',$date));
    wv_query("
      UPDATE LOW_PRIORITY
        diffs
      SET
        modifiedon_utc='$date',
        author='".addslashes($data['query']['pages'][$a['curid']]['revisions'][0]["user"])."'
      WHERE
        id={$a['id']}
    ");
  }
  if ($interactive) {
    echo "\n";
    echo "Finished on ".date('r')."\n";
  }
?>
