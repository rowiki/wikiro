<?
  require_once("wv_lib.php");
  $u=new wv_user();
  if ($u->id) {
    $r=wv_query("SELECT changedpwd,pwd FROM people WHERE id={$u->id}");
    $a=mysql_fetch_array($r);
    if (!$a['changedpwd']) {
      if ($_POST['action']=='changepwd') {
        if ($_POST['pwd']!=$_POST['pwd2']) {
          echo "<div align='center' style='color:red'>The passwords do not match, please try again!</div><hr>";
        } elseif (md5($_POST['pwd'].md5($u->id))==$a['pwd']) {
          echo "<div align='center' style='color:red'>Please <b>change</b> your password (i.e. type a new one, different from the one you used to have).</div><hr>";
        } else {
          $query='
            UPDATE people
            SET
              pwd="'.md5($_POST['pwd'].md5($u->id)).'",
              changedpwd=1
            WHERE
              id='.$u->id.'
          ';
          wv_query($query);
          return true;
        }
      }
      include('changepwd.html');
      exit;
    }
    return true;
  }
  include('login_form.html');
  exit;
?>
