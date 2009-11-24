<?
  ob_start();
  require_once('gui.php');
  wv_query("UPDATE people SET changedpwd=0 WHERE id={$u->id}");
  header('Location:./');
?>
