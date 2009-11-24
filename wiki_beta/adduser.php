<?
  // Fill these in
  $uname='';
  $pwd='';

  // -------------------------------------------------

  if (!$uname || !$pwd) {
    echo "You need to edit ".__FILE__." before executing it.\n";
    exit;
  }
  if ($_SERVER['REQUEST_METHOD']) {
    exit;
  }
  require_once "wv_lib.php";

  wv_adduser($uname,$pwd);
  echo "Done.\n";
