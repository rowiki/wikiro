<?
  require_once("wv_lib.php");
  require_once("login.php");
?>
<html>
<head>
  <title>WikiVerifier -- Istoria validarilor</title>
<script language='JavaScript' type='text/javascript'>
  function do_trust()
  {
    var cb=document.getElementById('sigur');
    if (!cb.checked) {
      alert('Trebuie sa bifati casuta din dreapta inainte de a apasa pe '+
        'buton pentru a va confirma intentia.');
      return false;
    }
    document.location=document.location+'&action=trust';
  }
</script>
<style type="text/css">
  h1 {
    font-weight: bold;
    font-size: 12pt;
    padding:10px 0px 5px 0px
  }
  h2 {
    font-weight: normal;
    font-size: 10pt;
    padding:10px 0px 5px 0px
  }
table.wv_history {
	border-width: 0px;
	border-spacing: 0px;
	border-style: solid;
	border-color: black;
	border-collapse: collapse;
	background-color: white;
	padding:3px;
}
table.wv_history th {
	border-width: 1px;
	border-style: solid;
	background-color: #eeeeee;
	padding:3px;
	-moz-border-radius: 0px;
}
table.wv_history td {
	border-width: 1px;
	border-style: solid;
	background-color: white;
	padding:3px;
	-moz-border-radius: 0px;
}
</style>

<script language='JavaScript'>
<!--
  function wv_hist_add_warning(link)
  {
    var warning=document.createElement("div");
    warning.innerHTML='<small>Nu uitaţi că vizualizarea diferenţei o validează!</small>';
    warning.style.backgroundColor='#FFE0E0';
    warning.style.padding='3px';
    warning.style.marginTop='3px';
    link.parentNode.appendChild(warning);
  }
-->
</script>
</head>
<body>
<?
  $new_id=wv_unslash($_GET['diff']);
  $old_id=wv_unslash($_GET['oldid']);
  $title=wv_unslash($_GET['title']);

  $tite=str_replace("%3A",":",str_replace("%2F","/",$title));

  $titleS=addslashes($title);

  echo "<table width='100%' border=0><tr><td width='100%'>\n";
  echo "<h1>".htmlspecialchars(str_replace("_"," ",$title))."</h1>\n";
  echo "</td><td><img src='images/logo_small.png'></td></tr></table>\n";
  echo "<hr>\n";

  echo "<div align=center><small>";
  echo "<a href='http://ro.wikipedia.org/wiki/$title' onClick='window.opener.location=this.href; return false'>[articol]</a>&nbsp;&mdash;&nbsp;";
  echo "<a href='http://ro.wikipedia.org/w/index.php?title=".rawurlencode($title)."&amp;oldid=$old_id&amp;diff=$new_id' onClick='window.opener.location=this.href; wv_hist_add_warning(this); return false'>[diferenţă]</a>";
  echo "<br>\n";

  if ($new_id=='next' || $new_id=='prev') {
    list($old_id,$new_id)=wv_infer($old_id,$new_id);
    if (!$new_id) {
      echo "Această diferenţă <b>nu poate fi verificată</b>.\n";
      exit;
    }
  }
  $new_idS=addslashes($new_id);
  $old_idS=addslashes($old_id);

  $r=wv_query("
    SELECT id, verified, author, successive
    FROM diffs
    WHERE
      new_id=\"$new_idS\" AND
      old_id=\"$old_idS\"
  ");
  $a=mysql_fetch_array($r);
  $author=$a['author'];
  $verified=$a['verified'];

  if ($verified==3) {
    $trusted=true;
  }

  if (!$trusted) {
    $r3=wv_query("
      SELECT
        id
      FROM
        people
      WHERE
        uname='".addslashes($author)."'
    ");
    if (mysql_fetch_row($r3)) {
      $trusted=true;
    }
  }

  if (!$trusted) {
    $r2=wv_query("
      SELECT
        COUNT(*)
      FROM
        familiars
      WHERE
        uname='".addslashes($author)."'
    ");
    list($cnt)=mysql_fetch_row($r2);
    $trusted=(bool) $cnt;
  }

  $goodtotrust=true;
  if (!$trusted) {
    $goodtotrust=!preg_match("/^[\d]{1,3}\.[\d]{1,3}\.[\d]{1,3}\.[\d]{1,3}$/",$author);
    if ($goodtotrust) {
      $h=wv_query("
        SELECT
          COUNT(*)
        FROM
          robots
        WHERE
          uname='".addslashes($author)."' AND
          disabled=0
      ");
      list($cnt)=mysql_fetch_row($h);
      if ($cnt) {
        $goodtotrust=false;
      }
    }
  }

  if (!$trusted && $_GET['action']=='trust' && $goodtotrust) {
    wv_query("
      INSERT
      INTO familiars
      SET
        uname='".addslashes($author)."',
        addedby={$u->id},
        addedon=NOW()
    ");
    echo "<b>$author</b> este acum un utilizator de incredere.";
    $trusted=true;
  }

  if (!$trusted && $a['author'] && $goodtotrust) {
    echo "<b>$author</b> este de incredere? ";
    echo "<input type='button' value='Da' onClick='do_trust()'> ";
    echo "<label for='sigur'>Confirmare</label> <input type='checkbox' id='sigur'>";
  }
  echo "</small></div>\n";

  echo "<h2>Diferenţa selectată <b>".($a['verified']?"a fost verificată":"nu a fost verificată")."</b>:</h2>\n\n";

  if ($d_id=$a['id']) {
    $r=wv_query("
      SELECT
        dh.id as id,
        dh.verified as verified,
        p.uname as uname,
        dh.vdate as vdate
      FROM
        diff_history dh
      LEFT JOIN
        people p ON p.id=dh.user_id
      WHERE
        dh.diff_id=$d_id
      ORDER BY
        dh.vdate
    ");
  }
  if (!$d_id || !mysql_num_rows($r)) {
    if ($a['verified']==2) {
      echo "<h2>Autorul acestei modificări este utilizator WikiVerifier.</h2>\n";
    } else {
      echo "<h2>Diferenţa nu a fost văzută de nimeni.</h2>\n";
    }
  } else {

?>
<table width='100%' class='wv_history'>
  <thead>
    <th>Utilizator</th>
    <th>Data</th>
    <th>Verificat</th>
  </thead>
<?
    while($a=mysql_fetch_array($r)) {
      echo "<tr>\n";
      echo "  <td>".htmlspecialchars($a['uname'])."</td>\n";
      echo "  <td>".htmlspecialchars($a['vdate'])."</td>\n";
      echo "  <td>".($a['verified']?"Da":"Nu")."</td>\n";
      if ($a['successive'] && ($_SERVER['REMOTE_ADDR']=='81.181.249.131' || $_SERVER['REMOTE_ADDR']=='192.168.0.200')) {
        echo "<td>".$a['author']."</td>\n";
      }
      echo "</tr>\n";
    }
  }
?>
