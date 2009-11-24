<?
  require_once('gui.php');
  if (!$u->hasRight('B')) return false;

  switch($_REQUEST['action']) {
    case 'add':
      require('useredit.php');
      break;
    case 'edit':
      require('useredit.php');
      break;
    case 'do_edit':
      require('userdoedit.php');
      require('userlist.php');
      break;
    case 'resetpwd':
      require('userresetpwd.php');
      require('userlist.php');
      break;
    default:
      require('userlist.php');
  }
?>
