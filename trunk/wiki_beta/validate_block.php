<?
  set_time_limit(1);
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
      fpassthru(fopen("images/unknown_block_large.png",'r'));
      exit;
    }
  }

  $min_id=$old_id;
  $max_id=$new_id;
  $new_idS=$max_idS=addslashes($new_id);
  $old_idS=$min_idS=addslashes($old_id);

  // Make sure this is successive
  $r=wv_query("
    SELECT id
    FROM diffs
    WHERE old_id=\"$old_idS\" and new_id=\"$new_idS\" and successive=1
  ");
  if (!mysql_fetch_row($r)) {
    fpassthru(fopen("images/unknown_block_large.png",'r'));
    exit;
  }

  // Make sure we have an author
  $r=wv_query("
    SELECT id,verified,author
    FROM diffs
    WHERE
      old_id=\"$old_idS\" AND
      new_id=\"$new_idS\"
  ");
  $a=mysql_fetch_array($r);
  if ($a['author']) {
    $author=$a['author'];
    $authorS=addslashes($a['author']);
  } elseif ($author) {
    $authorS=addslashes($author);
  } else {
    fpassthru(fopen("images/unknown_block_large.png",'r'));
    exit;
  }

  // Determine max in block
  do {
    $changed=false;
    $r=wv_query("
      SELECT
        author,
        new_id
      FROM
        diffs
      WHERE
        old_id=\"$max_idS\" AND
        successive=1
    ");
    if (!$r) {
      break;
    }
    $a=mysql_fetch_array($r);
    if (!$a) {
      break;
    }
    if ($a['author']==$author) {
      $max_id=$a['new_id'];
      $max_idS=addslashes($max_id);
      $changed=true;
    }
  } while($changed);

  // Determine min in block
  do {
    $changed=false;
    $r=wv_query("
      SELECT
        author,
        old_id
      FROM
        diffs
      WHERE
        new_id=\"$min_idS\" AND
        successive=1
    ");
    if (!$r) {
      break;
    }
    $a=mysql_fetch_array($r);
    if (!$a) {
      break;
    }
    if ($a['author']==$author) {
      $min_id=$a['old_id'];
      $min_idS=addslashes($min_id);
      $changed=true;
    }
  } while($changed);

  if ($min_id==$old_id && $max_id==$new_id) {
    // There is no block
    fpassthru(fopen("images/no_block_large.png",'r'));
    exit;
  }

  $walk_id=$min_id;
  $walk_idS=addslashes($walk_id);
  while($walk_id!=$max_id) {
    $r=wv_query("
      SELECT
        d.id d_id,
        dh.id dh_id,
        d.new_id,
        d.verified d_verified,
        dh.verified dh_verified
      FROM
        diffs d
      LEFT JOIN
        diff_history dh ON dh.diff_id=d.id AND
        dh.user_id={$u->id}
      WHERE
        d.old_id=\"$walk_idS\" AND
        d.successive=1
    ");
    $a=mysql_fetch_array($r);
    if (!$a) {
      // This should never happen
      fpassthru(fopen("images/unknown_block_large.png",'r'));
      exit;
    }
    if (!$a['d_verified']) {
      wv_query("
        UPDATE
          diffs
        SET
          verified=1
        WHERE
          id={$a['d_id']}
      ");
    }
    if (!$a['dh_id']) {
      wv_query("
        INSERT INTO diff_history
        SET
          diff_id={$a['d_id']},
          user_id={$u->id},
          vdate=now(),
          verified=1
      ");
    } elseif (!$a['dh_verified']) {
      wv_query("
        UPDATE diff_history
        SET
          vdate=now(),
          verified=1
        WHERE id={$a['dh_id']}
      ");
    }
    $walk_id=$a['new_id'];
    $walk_idS=addslashes($walk_id);
  }
  fpassthru(fopen("images/validated_block_large.png",'r'));
  exit;

?>
