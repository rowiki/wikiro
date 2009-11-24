<?
  require_once('gui.php');
  if (!$u->hasRight('B')) return false;
  if ($id=abs($_REQUEST['id'])) {
    // EDIT user
    $r=wv_query("
      SELECT
        uname, rights, disabled
      FROM people
      WHERE id=$id
    ");
    $a=mysql_fetch_array($r);
  } else {
    // ADD user
    $a=array(
      'uname'=>'',
      'rights'=>'',
      'disabled'=>0
    );
  }
?>
<hr>
<div style='border-style:solid; border-color:red; margin-bottom: 8px; background-color:#ffeeee; padding:10px'>
  <b>VERY IMPORTANT!</b> All users here must have exactly the same username
  they have on Wikipedia!
</div>
<form action='' method='POST'>
<input type='hidden' name='action' value='do_edit'>
<input type='hidden' name='id' value='<?= $id ?>'>
<table border=1>
<tr>
  <th align=right style='background-color:#eeeeee'>Username</th>
  <td><input type='text' name='uname' value='<?= addslashes($a['uname']) ?>'></td>
</tr>
<tr>
  <th align=right style='background-color:#eeeeee'>Rights</th>
  <td>
    <input type='checkbox' name='rights[]' value='B' id='right_B'
      <?= strstr($a['rights'],'B')?'checked':'' ?>
    >
    <label for='right_B'>Bureaucrat</label>
  </td>
</tr>
<tr>
  <th align=right style='background-color:#eeeeee'>Account<br>enabled</th>
  <td>
    <input id='active_yes' type='radio' name='disabled' value='0' <?= $a['disabled']?'':'checked'?>>
      <label for='active_yes'>Yes</label>
    <input id='active_no' type='radio' name='disabled' value='1' <?= $a['disabled']?'checked':''?>>
      <label for='active_no'>No</label>
  </td>
</tr>
<tr>
  <td colspan=2 align=center>
    <input type='submit' name='submit' <?
  if (!$id) {
    echo "onClick='if (!form.uname.value) { alert(\"Please specify a username!\"); return false; } return confirm(\"Once created, users cannot be deleted. Are you sure you want to create this user?\")'";
  }
?> value='Submit'>
    <input type='button' name='cancel' value='Cancel' onClick='window.location="?"'>
  </td>
</tr>
</table>
</form>
<script language=JavaScript>
<!--
  document.forms[0].uname.focus();
-->
</script>
