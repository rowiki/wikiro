<?
  require_once('gui.php');
?>
<hr>
<div><small>Note: Users are always sorted by username.</small></div>
<table border=1>
<?
  $hdr="
<tr style='background-color:#eeeeee'>
  <th>Wikipedia<br>Username</th>
  <th>Account<br>enabled</th>
  <th>Added by</th>
  <th>Added on</th>
</tr>
  ";
  $r=wv_query("
    SELECT
      f.id as id,
      f.uname as uname,
      f.disabled as disabled,
      p.uname as addedby,
      f.addedon as addedon
    FROM familiars f
    LEFT JOIN people p ON p.id=f.addedby
    ORDER BY f.uname
  ");
  $ctr=0;
  while($a=mysql_fetch_array($r)) {
    if (!($ctr%10)) {
      echo $hdr;
    }
    $ctr++;
?>
<tr>
  <th style='background-color:#fafafa'><a href='http://ro.wikipedia.org/wiki/Special:Contributions/<?= $a['uname'] ?>'><?= $a['uname'] ?></a></th>
  <td><?= $a['disabled']?'No':'Yes' ?></td>
  <td><?= $a['addedby']?$a['addedby']:"<i>God</i>" ?></td>
  <td><?= ($a['addedon']=='2000-01-01 00:00:00')?'n/a':$a['addedon'] ?></td>
</tr>
<?
  }
?>
</table>
