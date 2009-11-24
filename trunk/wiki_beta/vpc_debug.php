<?
  $cDate=time();
  require_once('wv_lib.php');

    $utc_cDate=$cDate-date('Z');

    $query="
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1
    ";
    $r=wv_query($query);
    echo "Query 1:\n$query";

    list($total_diffs)=mysql_fetch_row($r);
    $query="
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1 AND
        verified>0
    ";
    $r=wv_query($query);
    echo "Query 2:\n$query";
    list($verified_diffs)=mysql_fetch_row($r);
    $percent=round($verified_diffs/$total_diffs*100);

    $query="
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1 AND
        verified=0
    ";
    $r=wv_query($query);
    echo "Query 3:\n$query";
    list($unverified_diffs)=mysql_fetch_row($r);

    echo "total: $total_diffs; verified: $verified_diffs; unverified: $unverified_diffs; percent: $percent\n";

    $r=wv_query("
      SELECT
        DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) mindate,
        DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) maxdate,
        FROM_UNIXTIME($utc_cDate) cdate
    ");
    var_dump(mysql_fetch_array($r));
?>
