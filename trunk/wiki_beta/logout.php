<?
  require_once("wv_lib.php");
  $u=new wv_user();
  if ($u->id) {  
    session_destroy();
  }
  header("Location:./");
?>
