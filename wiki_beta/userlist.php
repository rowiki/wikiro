<?
  require_once('gui.php');
  if (!$u->hasRight('B')) return false;
?>
<hr>
<div><small>Note: Users are always sorted by username.</small></div>
<table border=1>
<?
  $hdr="
<tr style='background-color:#eeeeee'>
  <th>Username</th>
  <th>Rights</th>
  <th>Account<br>enabled</th>
  <th>Account<br>activated</th>
  <th>Added by</th>
  <th>Added on</th>
  <th>Actions</th>
</tr>
  ";
  $r=wv_query("
    SELECT
      p.id as id,
      p.uname as uname,
      p.rights as rights,
      p.disabled as disabled,
      p.changedpwd as changedpwd,
      b.uname as addedby,
      p.addedon as addedon
    FROM people p
    LEFT JOIN people b ON b.id=p.addedby
    ORDER BY p.uname
  ");
  $ctr=0;
  while($a=mysql_fetch_array($r)) {
    if (!($ctr%10)) {
      echo $hdr;
    }
    $ctr++;
?>
<tr>
  <th style='background-color:#fafafa'><?= $a['uname'] ?></th>
  <td><?= $a['rights']?$a['rights']:'none' ?></td>
  <td><?= $a['disabled']?'No':'Yes' ?></td>
  <td><?= $a['changedpwd']?'Yes':'No' ?></td>
  <td><?= $a['addedby']?$a['addedby']:"<i>God</i>" ?></td>
  <td><?= ($a['addedon']=='2000-01-01 00:00:00')?'n/a':$a['addedon'] ?></td>
  <td>
    <?= "<a href='?action=edit&amp;id={$a['id']}'>[Edit]</a>" ?>
    <?= "<a href='?action=resetpwd&amp;id={$a['id']}' onClick='return confirm(\"Are you sure you want to reset the password for user ".htmlspecialchars($a['uname'])."?\")'>[Reset password]</a>" ?>
  </td>
</tr>
<?
  }
?>
</table>
<a href="?action=add">[Create New User]</a>
