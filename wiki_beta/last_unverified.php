<?

  require_once('wv_lib.php');

  $h=wv_query("
    SELECT
      title,
      curid,
      new_id,
      old_id,
      author,
      UNIX_TIMESTAMP(modifiedon_utc) AS `mod`
    FROM
      diffs
    WHERE
      successive=1 AND
      verified=0
    ORDER BY
      modifiedon_utc DESC
    LIMIT 1
  ");

  echo "<table>\n";
  while($a=mysql_fetch_array($h)) {
    $time=date('r',date('Z',$a['mod'])*60+$a['mod']);
    echo "<tr>\n";
    echo "<td><a href='http://ro.wikipedia.org/w/index.php?title=".rawurlencode($a['title'])."&amp;curid={$a['curid']}&amp;diff={$a['new_id']}&amp;oldid={$a['old_id']}'>prec</a></td><td>{$a['title']}</td><td>{$a['author']}</td><td>$time</td>\n";
    echo "</tr>\n";
  }
  echo "</ul>\n";

/*
http://ro.wikipedia.org/w/index.php?title=Antipap%C4%83&curid=35522&diff=1521333&oldid=1521328


CREATE TABLE `diffs` (
  `id` int(8) unsigned NOT NULL auto_increment,
  `title` varchar(255) NOT NULL default '',
  `new_id` int(8) unsigned NOT NULL default '0',
  `old_id` int(8) unsigned NOT NULL default '0',
  `verified` tinyint(1) unsigned NOT NULL default '0',
  `successive` tinyint(1) unsigned default NULL,
  `curid` int(10) unsigned NOT NULL default '0',
  `aggregate_id` int(10) unsigned NOT NULL default '0',
  `everseen` tinyint(1) unsigned NOT NULL default '0',
  `author` varchar(255) NOT NULL default '',
  `addedon` datetime NOT NULL default '2000-01-01 00:00:00',
  `modifiedon_utc` datetime NOT NULL default '2000-01-01 00:00:00'
*/
?>
