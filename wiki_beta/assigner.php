<?
  require_once("wv_lib.php");
  require_once("diff_lib.php");
  header("Content-type:image/png");
  $u=new wv_user();
  if (!$u->id) {
    fpassthru(fopen("images/lock_large.png",'r'));
    exit;
  }
  register_hit('assignment');

  $new_id=wv_unslash($_GET['diff']);
  $old_id=wv_unslash($_GET['oldid']);
  if ($_GET['wv_newid']) {
    $new_id=wv_unslash($_GET['wv_newid']);
  }
  if ($_GET['wv_oldid']) {
    $old_id=wv_unslash($_GET['wv_oldid']);
  }
  if ($_GET['wv_user']) {
    $author=wv_unslash($_GET['wv_user']);
  }
  $title=wv_unslash($_GET['title']);
  if ($_GET['wv_curid']) {
    $curid=wv_unslash($_GET['wv_curid']);
  }

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
  $authorS=addslashes($author);
  $curidS=addslashes($curid);

  $r=wv_query("
    SELECT id,verified,author
    FROM diffs
    WHERE
      old_id=\"$old_idS\" AND
      new_id=\"$new_idS\"
  ");
  $a=mysql_fetch_array($r);
  if ($a['author']) {
    $authorS=addslashes($a['author']);
  } elseif ($authorS) {
    wv_query("
      UPDATE
        diffs
      SET
        author='".$authorS."'
      WHERE
        id={$a['id']}
    ");
  }

  $r2=wv_query("
    SELECT uname
    FROM people
    WHERE
      uname='$authorS' AND
      disabled=0 AND
      changedpwd=1
  ");
  $r3=wv_query("
    SELECT
      id,
      if(date_add(addedon, interval 31 day)<now(),'old','new') age
    FROM familiars
    WHERE
      uname='$authorS' AND
      disabled=0
  ");
  $r4=wv_query("
    SELECT uname
    FROM robots
    WHERE
      uname='$authorS' AND
      disabled=0
  ");
  if (mysql_fetch_row($r2)) {
    header("Pragma:cache");
    $d_id=$a['id'];
    $img='blue';
  } elseif ($r=mysql_fetch_array($r3)) {
    $d_id=$a['id'];
    if ($r['age']=='old') {
      $img='navyblue';
    } else {
      $img='red_navyblue';
    }
  } elseif (mysql_fetch_row($r4)) {
    $d_id=$a['id'];
    $img='black';
  } else {
    if ($a['verified']) {
      header("Pragma:cache");
      $d_id=$a['id'];
      $img='green';
    } else {
      header("Pragma:no-cache");
      if ($a['id']) {
        $query="UPDATE diffs";
        $insert=false;
      } else {
        $query="INSERT INTO diffs";
        $insert=true;
      }
      // We CANNOT determine whether the changes are successive
      $query.="
        SET
          title=\"$titleS\",
          old_id=\"$old_idS\",
          new_id=\"$new_idS\",
          ".($authorS?"author=\"$authorS\",":"")."
          ".($curidS?"curid=\"$curidS\",":"")."
          verified=1,
          ".($insert?"addedon=NOW(),":'')."
          everseen=1
      ";
      if ($a['id']) {
        $query.="WHERE id=\"{$a['id']}\"";
      }
      wv_query($query);
      if ($a['id']) {
        $d_id=$a['id'];
      } else {
        $d_id=mysql_insert_id();
      }
      $img='red_green';
    }
  }

  $r=wv_query("
    SELECT id,verified
    FROM diff_history
    WHERE
      diff_id=$d_id AND
      user_id={$u->id}
  ");
  $a=mysql_fetch_array($r);
  if (!$a['verified']) {
    if ($a['id']) {
      wv_query("
        UPDATE diff_history
        SET
          vdate=now(),
          verified=1
        WHERE id={$a['id']}
      ");
    } else {   
      wv_query("
        INSERT INTO diff_history
        SET
          diff_id=$d_id,
          user_id={$u->id},
          vdate=now(),
          verified=1
      ");
    }
  }
  $img.="_large.png";
  fpassthru(fopen('images/'.$img,'r'));
?>
