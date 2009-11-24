<?
  require_once("wv_lib.php");
  require_once("diff_lib.php");
  header("Content-type:image/png");
  $u=new wv_user();
  if (!$u->id) {
    fpassthru(fopen("images/lock_large.png",'r'));
    exit;
  }

  $new_id=wv_unslash($_GET['diff']);
  $old_id=wv_unslash($_GET['oldid']);
  if ($_GET['wv_newid']) {
    $new_id=wv_unslash($_GET['wv_newid']);
  }
  if ($_GET['wv_oldid']) {
    $new_id=wv_unslash($_GET['wv_oldid']);
  }
  if ($_GET['wv_user']) {
    $author=wv_unslash($_GET['wv_user']);
  }
  $title=wv_unslash($_GET['title']);

  $tite=str_replace("%3A",":",str_replace("%2F","/",$title));
  if ($new_id=='next' || $new_id=='prev') {
    list($old_id,$new_id)=wv_infer($old_id,$new_id);
    if (!$new_id) {
      fpassthru(fopen("images/grey_large.png",'r'));
      exit;
    }
  }

  $new_idS=addslashes($new_id);
  $old_idS=addslashes($old_id);
  $titleS=addslashes($title);

  $r=wv_query("
    SELECT id,verified,author
    FROM diffs
    WHERE
      old_id=\"$old_idS\" AND
      new_id=\"$new_idS\"
  ");
  $a=mysql_fetch_array($r);

  $verified_anyway=$a['verified']>1;

/*
  // People sometimes want to make political statements by unack'ing
  // a diff they previously verified. We allow this.
  switch($a['verified']) {
    case 2:
      fpassthru(fopen("images/blue_large.png",'r'));
      exit;
    case 3:
      fpassthru(fopen("images/navyblue_large.png",'r'));
      exit;
    case 4:
      fpassthru(fopen("images/black_large.png",'r'));
      exit;
  }
*/

/*
  // Old, before verified could be 2 or 3
  $r2=wv_query("
    SELECT uname
    FROM people
    WHERE
      uname=\"{$a['author']}\" AND
      disabled=0 AND
      changedpwd=1
  ");
  if (mysql_fetch_row($r2)) {
    fpassthru(fopen("images/blue_large.png",'r'));
    exit;
  }
*/
  if (!$a['verified']) {
    fpassthru(fopen("images/grey_large.png",'r'));
    exit;
  }

  $d_id=$a['id'];
  $r=wv_query("
    SELECT id
    FROM diff_history
    WHERE
      diff_id=$d_id AND
      verified=1 AND
      user_id={$u->id}
  ");
  $a=mysql_fetch_array($r);
  if (!$dh_id=$a['id']) {
    fpassthru(fopen("images/grey_large.png",'r'));
    exit;
  }

  wv_query("
    UPDATE diff_history
    SET verified=0
    WHERE id=$dh_id
  ");

  $r=wv_query("
    SELECT COUNT(*) AS ctr
    FROM diff_history
    WHERE
      diff_id=$d_id AND
      verified=1
  ");
  $a=mysql_fetch_array($r);
  if ($a['ctr'] || $verified_anyway) {
    $img='green_black';
  } else {
    $img='red_exclamation';
    wv_query("
      UPDATE diffs
      SET verified=0
      WHERE id=$d_id
    ");
  }

  header("Pragma:no-cache");
  $img.="_large.png";
  fpassthru(fopen('images/'.$img,'r'));
?>
