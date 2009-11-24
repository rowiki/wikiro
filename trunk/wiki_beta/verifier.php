<?
  require_once("wv_lib.php");
  header("Content-type:image/png");
  $u=new wv_user();
  if (!$u->id) {
    fpassthru(fopen("images/lock.png",'r'));
    exit;
  }
  register_hit('verification');

  $new_id=wv_unslash($_GET['diff']);
  $old_id=wv_unslash($_GET['oldid']);
  $succ=wv_unslash($_GET['succ']);
  $curid=wv_unslash($_GET['curid']);
  $title=wv_unslash($_GET['title']);
  $author=wv_unslash(str_replace('_',' ',str_replace('&action=edit','',$_GET['wv_user'])));

  if ($new_id=='next' || $new_id=='prev') {
    list($old_id,$new_id)=wv_infer($old_id,$new_id);
    if (!$new_id) {
      fpassthru(fopen("images/grey.png",'r'));
      exit;
    }
  }

  $new_idS=addslashes($new_id);
  $old_idS=addslashes($old_id);
  $titleS=addslashes($title);
  $curidS=addslashes($curid);
  $authorS=addslashes($author);

  $r=wv_query("
    SELECT
      d.id,
      d.verified,
      d.successive,
      d.everseen,
      d.author,
      group_concat(dh.user_id) user
    FROM diffs d
    LEFT JOIN diff_history dh ON
      dh.diff_id=d.id AND
      dh.verified=1
    WHERE
      old_id=\"$old_idS\" AND
      new_id=\"$new_idS\"
    GROUP BY d.id
  ");
  $a=mysql_fetch_array($r);
  if ($authorS) {
    $authorRS=$authorS;
  } else {
    $authorRS=addslashes($a['author']);
  }
  $r2=wv_query("
    SELECT id
    FROM people
    WHERE
      uname=\"$authorRS\" AND
      disabled=0 AND
      changedpwd=1
  ");
  $r3=wv_query("
    SELECT
      id,
      if(date_add(addedon, interval 31 day)<now(),'old','new') age
    FROM familiars
    WHERE
      uname='$authorRS' AND
      disabled=0
  ");
  $r4=wv_query("
    SELECT id
    FROM robots
    WHERE
      uname='$authorRS' AND
      disabled=0
  ");
  $verified=0;
  if (mysql_fetch_row($r2)) {
    header("Pragma:cache");
    $img='blue';
    $verified=2;
  } elseif ($r=mysql_fetch_array($r3)) {
    header("Pragma:cache");
    if ($r['age']=='old') {
      $img='navyblue';
    } else {
      $img='red_navyblue';
    }
    $verified=3;
  } elseif (mysql_fetch_row($r4)) {
    header("Pragma:cache");
    $img='black';
    $verified=4;
  } else {
    header("Pragma:no-cache");
    if ($a['verified']) {
      if ($a['user']==$u->id) {
        $img='green_myself';
      } elseif (in_array($u->id,explode(',',$a['user']))) {
        $img='green_both';
      } else {
        $img='green_others';
      }
      //$img='green';
    } else {
      if (!$a['everseen']) {
        if ($a['id']) {
          $img='red';
        } else {
          $img='pink_red';
        }
      } else {
        $img='red_exclamation';
      }
    }
  }
  $img.=".png";
  fpassthru(fopen('images/'.$img,'r'));
  flush();

  if (!$a['id']) {
    if ($succ==='yes') {
      $s=",successive=1";
    }
    if ($succ==='no') {
      $s=",successive=0";
    }
    wv_query("
      INSERT INTO diffs
      SET
        title=\"$titleS\",
        old_id=\"$old_idS\",
        new_id=\"$new_idS\",
        verified=$verified,
        curid=\"$curidS\",
        author=\"$authorS\",
        addedon=NOW()
        $s
    ");
    // Prevent race conditions from producing duplicates
    $myID=mysql_insert_id();
    $dupes=wv_query("
      SELECT COUNT(*) FROM diffs WHERE
      old_id=\"$old_idS\" AND
      new_id=\"$new_idS\"
    ");
    list($dupe_count)=mysql_fetch_row($dupes);
    if ($dupe_count>1) {
      wv_query("
        DELETE FROM diffs WHERE id=$myID
      ");
    }
  } else {
    if ($succ && $a['successive']===NULL) {
      if ($succ==='yes') {
        $s='1';
      } elseif ($succ==='no') {
        $s='0';
      } else {
        $s='NULL';
      }
      wv_query("
        UPDATE diffs SET successive=$s WHERE id={$a['id']}
      ");
    }
    if ($author && !$a['author']) {
      wv_query("
        UPDATE diffs SET author=\"$authorS\" WHERE id={$a['id']}
      ");
/*
      v_log("Doing the author thing!");
    } else {
      v_log("Got both author and ID, but not doing it because ".($author?"I already have an author in DB":"I don't have an author in GET")." (succ=$succ)");
*/
    }
  }

  function v_log($msg)
  {
    global $title,$author,$a;
    static $secondrun;
    if (!$secondrun) {
      wv_log("Title: $title\nAuthor: $author\nID: {$a['id']}\nAuthor[GET]: {$_GET['wv_user']}");
      $secondrun=true;
    }
    wv_log($msg);
  }
?>
