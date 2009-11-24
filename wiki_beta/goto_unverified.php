<?

  require_once('wv_lib.php');
  require('login.php');

  $exceptions=array();
  set_time_limit(1);
  do {
    if (!$exceptions) {
      $except='0';
    } else {
      $except=implode(',',$exceptions);
    }
    $h=wv_query("
      SELECT
        id,
        title,
        curid,
        new_id,
        old_id,
        author,
        UNIX_TIMESTAMP(modifiedon_utc) AS `mod`
      FROM
        diffs
      WHERE
        successive=1 AND
        verified=0 AND
        id NOT IN ($except)
      ORDER BY
--        modifiedon_utc DESC
        new_id DESC
      LIMIT 1
    ");
    $a=mysql_fetch_array($h);

    $h=wv_query("
      SELECT
        id
      FROM
        diff_history
      WHERE
        diff_id={$a['id']} AND
        user_id={$u->id}
    ");

    if ($h && ($a2=mysql_fetch_row($h))) {
      $exceptions[]=$a['id'];
      continue;
    } elseif (!$h) {
      echo mysql_error();
      exit;
    }
//echo "ID: ".$a['id']."\n";
//exit;
    header("Location: http://ro.wikipedia.org/w/index.php?title=".rawurlencode($a['title'])."&curid={$a['curid']}&diff={$a['new_id']}&oldid={$a['old_id']}");
    exit;
  } while (true);

?>
