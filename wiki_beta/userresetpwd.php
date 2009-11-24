<?
  require_once('gui.php');
  if (!$u->hasRight('B')) return false;
  if (!$id=abs($_REQUEST['id'])) {
    return false;
  }
  $pwd=md5(uniqid('wvpass'));
  $hash=md5($pwd.md5($id));
  wv_query("UPDATE people SET pwd='$hash', changedpwd=0 WHERE id=$id");
  echo "<div align='center' style='color:green;background-color:#ccffcc;padding:3px;border-style:solid;border-color:#55aa55;margin:10px'>The new password for that user is <b>$pwd</b></div>\n";

?>
