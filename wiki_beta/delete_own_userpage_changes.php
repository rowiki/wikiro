<?
  if (strtotime('2007-11-30')<time()) {
    // no need any more
    exit;
  }

  require('wv_lib.php');

  wv_query("delete from diff_history where diff_id in (select id from diffs where diffs.title LIKE CONCAT('Utilizator:',author,'%'))");

//  echo "Errors1: ".mysql_error();

  wv_query("delete from diffs where diffs.title LIKE concat('Utilizator:',author,'%')");
//  echo "Errors2: ".mysql_error();
?>
