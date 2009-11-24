<?

require_once('wv_lib.php');

do {
  $count=0;

  $h=wv_query("
    UPDATE LOW_PRIORITY
      diffs dm,
      diffs ds
    SET
      dm.curid=ds.curid
    WHERE
      dm.curid=0 AND
      ds.curid!=0 AND
      dm.old_id=ds.new_id
  ");
  $count+=mysql_affected_rows();


  $h=wv_query("
    UPDATE LOW_PRIORITY
      diffs dm,
      diffs ds
    SET
      dm.curid=ds.curid
    WHERE
      dm.curid=0 AND
      ds.curid!=0 AND
      dm.new_id=ds.old_id
  ");
  $count+=mysql_affected_rows();
} while($count);

?>
