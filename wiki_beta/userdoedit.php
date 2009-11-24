<?
  require_once('gui.php');
  if (!$u->hasRight('B')) return false;
  $id=abs($_REQUEST['id']);
  if (!$_REQUEST['uname']) {
?>
<div align='center' style='color:red'>You need to provide a username!</div>
<?
    return false;
  }
  $r=wv_query("
    SELECT uname
    FROM people
    WHERE
      uname=\"".wv_unslash($_REQUEST['uname'])."\" AND
      id!=$id
  ");
  $a=mysql_fetch_array($r);
  if ($a) {
?>
<div align='center' style='color:red'>Duplicate username!</div>
<?
    return false;
  }
  if ($id) {
    $query='UPDATE people ';
  } else {
    $query='INSERT INTO people';
  }
  $query.='
    SET
      uname="'.wv_unslash($_REQUEST['uname']).'",
      rights="'.(is_array($_REQUEST['rights'])?wv_unslash(implode('',$_REQUEST['rights'])):'').'",
      disabled="'.($_REQUEST['disabled']?1:0).'"
  ';
  if ($id) {
    $query.="WHERE id=$id";
  } else {
    $query.=", addedby={$u->id}, addedon=now()";
  }
  wv_query($query);
  if (!$id) {
    $pwd=md5(uniqid('wvpass'));
    $id=mysql_insert_id();
    $hash=md5($pwd.md5($id));
    wv_query("UPDATE people SET pwd='$hash' WHERE id=$id");
    echo "<div align='center' style='color:green;background-color:#ccffcc;padding:3px;border-style:solid;border-color:#55aa55;margin:10px'>The password for user ".wv_unslash($_REQUEST['uname'])." is <b>$pwd</b></div>\n";
  }
?>
