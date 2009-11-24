<?

  if ($_SERVER['REMOTE_ADDR']) {
    echo "This is an internal script.";
    exit;
  }

  require_once('wv_lib.php');

  $query_descriptors=array(
    array(
      "verified"=>2,
      "author"=>"select uname from people where disabled=0"
    ),
    array(
      "verified"=>3,
      "author"=>"select uname from familiars where disabled=0"
    ),
    array(
      "verified"=>4,
      "author"=>"select uname from robots where disabled=0"
    )
  );

  for($i=0;$i<count($query_descriptors);$i++) {
    $rest='';
    for($j=$i+1;$j<count($query_descriptors);$j++) {
      $rest.=" &&
          author NOT IN (".$query_descriptors[$j]['author'].")"
      ;
    }
    $query="
      UPDATE LOW_PRIORITY diffs
        SET verified=".$query_descriptors[$i]['verified']."
        WHERE
          author IN (".$query_descriptors[$i]['author'].")".
          $rest
    ;
    wv_query($query);
    if ($e=mysql_error()) {
      echo $e;
    }
  }
/*
  wv_query("
    update LOW_PRIORITY diffs set verified =2 where author in (select uname from people where disabled=0)
  ");
  if ($e=mysql_error()) {
    echo $e;
  }

  wv_query("
    update LOW_PRIORITY diffs set verified =3 where author in (select uname from familiars where disabled=0)
  ");
  if ($e=mysql_error()) {
    echo $e;
  }

  wv_query("
    update LOW_PRIORITY diffs set verified =4 where author in (select uname from robots where disabled=0)
  ");
  if ($e=mysql_error()) {
    echo $e;
  }
*/
?>
