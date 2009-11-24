<?
/*
0 - ff0000 (unverified)
1 - 00d10a (verified)
2 - afafee (user)
3 - 0b00a9 (familiar)
4 - 000000 (robot)
*/
  require_once("wv_lib.php");
  if ($argv[1]!=="cli") {
    echo "This tool can only run from the command line with parameter 'cli'. Exiting.\n";
    exit;
  }

  if (!`which rrdtool 2>/dev/null`) {
    echo "rrdtool could not be found, exiting!\n";
    exit;
  }

  $cDir=dirname(__FILE__);
  $rra=$cDir."/wv_data.rra";
  $rra2=$cDir."/wv_stat.rra";


  $r=wv_query("
    SELECT MAX(hdate) AS crondate, NOW() as ldate FROM hits WHERE htype='cron'
  ");
  $a=mysql_fetch_array($r);
  if (!$a['crondate']) {
    // We have no previous execution of the cron script, all must die!
    wv_query("
      DELETE FROM hits
    ");
    cleanexit();
  }

  $cDate=trimdate($a['crondate']);
  $lDate=trimdate($a['ldate']);
  // exec("rm -f $rra");
  if (!is_file($rra)) {
    exec(
      "rrdtool create $rra -s 3600 ".
      "--start ".($cDate-1)." ".
      "DS:assignments:GAUGE:18000:0:U ".
      "DS:verifications:GAUGE:18000:0:U ".
      "DS:ucount:GAUGE:18000:0:U ".
      "RRA:AVERAGE:0.5:1:336 ".
      "RRA:AVERAGE:0.5:24:365"
    );
  }

  if (!is_file($rra2)) {
    exec(
      "rrdtool create $rra2 -s 3600 ".
      "--start ".($cDate-1)." ".
      "DS:verified_percent:GAUGE:18000:0:100 ".
      "DS:user_percent:GAUGE:18000:0:100 ".
      "DS:familiar_percent:GAUGE:18000:0:100 ".
      "DS:robot_percent:GAUGE:18000:0:100 ".
      "DS:old_percent:GAUGE:18000:0:100 ".
      "RRA:AVERAGE:0.5:1:336 ".
      "RRA:AVERAGE:0.5:24:365"
    );
  }

  $pDate=0;
  while($cDate<$lDate) {
    $where="
        hdate>=FROM_UNIXTIME($cDate) AND
        hdate<FROM_UNIXTIME($cDate+3600)
    ";
    $r=wv_query("
      SELECT htype AS type, COUNT(*) AS count
      FROM hits
      WHERE $where
      GROUP BY htype
    ");
    $assignment=$verification=0;
    while($a=mysql_fetch_array($r)) {
      if (!in_array($a['type'],array('assignment','verification'))) {
        continue;
      }
      $$a['type']=$a['count'];
    }
    $r=wv_query("
      SELECT DISTINCT(user_id)
      FROM hits
      WHERE $where AND user_id!=0
    ");
    $users=mysql_num_rows($r);
    $assignment=(int) $assignment;
    $verification=(int) $verification;
    //echo("Time: ".date('r',$cDate)." -- assignments: $assignment; Verifications: $verification; Users: $users\n");
    // Uncomment after testing
    $rrdtool_update=
      "rrdtool update $rra ".
      "-t assignments:verifications:ucount ".
      "$cDate:$assignment:$verification:$users"
    ;
    //echo "Executing rrdtool update:\n$rrdtool_update\n";
    exec($rrdtool_update);
    wv_query("
      DELETE FROM hits
      WHERE $where AND htype!='cron'
    ");

    $utc_cDate=$cDate-date('Z');

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1
    ");
    list($total_diffs)=mysql_fetch_row($r);

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1 AND
        verified=1
    ");
    list($verified_diffs)=mysql_fetch_row($r);
    if ($total_diffs) {
      $verified_percent=round($verified_diffs/$total_diffs*100);
    } else {
      $verified_percent=0;
    }

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1 AND
        verified=2
    ");
    list($user_diffs)=mysql_fetch_row($r);
    if ($total_diffs) {
      $user_percent=round($user_diffs/$total_diffs*100);
    } else {
      $user_percent=0;
    }

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1 AND
        verified=3
    ");
    list($familiar_diffs)=mysql_fetch_row($r);
    if ($total_diffs) {
      $familiar_percent=round($familiar_diffs/$total_diffs*100);
    } else {
      $familiar_percent=0;
    }

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -25 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -1 HOUR) AND
        successive=1 AND
        verified=4
    ");
    list($robot_diffs)=mysql_fetch_row($r);
    if ($total_diffs) {
      $robot_percent=round($robot_diffs/$total_diffs*100);
    } else {
      $robot_percent=0;
    }

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -264 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -240 HOUR) AND
        successive=1
    ");
    list($total_old_diffs)=mysql_fetch_row($r);

    $r=wv_query("
      SELECT
        COUNT(*)
      FROM
        diffs
      WHERE
        modifiedon_utc>DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -264 HOUR) AND
        modifiedon_utc<DATE_ADD(FROM_UNIXTIME($utc_cDate), INTERVAL -240 HOUR) AND
        successive=1 AND
        verified>0
    ");
    list($verified_old_diffs)=mysql_fetch_row($r);
    $verified_old_percent=round($verified_old_diffs/$total_old_diffs*100);

    $rrdtool_update=
      "rrdtool update $rra2 ".
      "-t verified_percent:user_percent:familiar_percent:robot_percent:old_percent ".
      "$cDate:$verified_percent:$user_percent:$familiar_percent:$robot_percent:$verified_old_percent"
    ;
    exec($rrdtool_update);
    $pDate=$cDate;
    $cDate+=3600;
  }
  if (!$pDate) {
    exit;
  }
  

  // Generate graphs ----------------------------------------------------------

  $width1y=365;
  $width14d=336;
  $width24h=96;
  $height1y=$height14d=75;

  $width30d=600;
  $height24h=$height30d=50;

  // Assignments/24h
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_ass_24h.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-24h ".
    "--width $width24h ".
    "--height $height24h ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:assignments:AVERAGE ".
    "LINE2:ds0a#0000FF:\"per 24h\" ".
    "COMMENT:'Curr\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\" ".
    "> /dev/null");
    
  // Assignments/2w
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_ass_2wk.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-14d ".
    "--width $width14d ".
    "--height $height14d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:assignments:AVERAGE ".
    "AREA:ds0a#0000FF:\"Assignments per 2w\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:ds0a:MIN:%2.0lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.0lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");
    
  // Assignments/30d
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_ass_30d.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-30d ".
    "--width $width30d ".
    "--height $height30d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:assignments:AVERAGE ".
    "AREA:ds0a#0000FF:\"Total assignments per 30d\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\" ".
    "COMMENT:'|   Min\: ' ".
    "GPRINT:ds0a:MIN:%2.0lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.0lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");

  // Assignments/1y
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_ass_1y.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-1y ".
    "--width $width1y ".
    "--height $height1y ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:assignments:AVERAGE ".
    "AREA:ds0a#0000AA:\"Assignments per 1y\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.2lf\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:ds0a:MIN:%2.2lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.2lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");
    
  // V%/24h
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vpc_24h.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-24h ".
    "--width $width24h ".
    "--height $height24h ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "--upper-limit 100 ".
    "--lower-limit 0 ".
    "--rigid ".
    "DEF:ds0a=$rra2:verified_percent:AVERAGE ".
    "DEF:ds0b=$rra2:user_percent:AVERAGE ".
    "DEF:ds0c=$rra2:familiar_percent:AVERAGE ".
    "DEF:ds0d=$rra2:robot_percent:AVERAGE ".
    "CDEF:total_verified=ds0a,ds0b,ds0c,ds0d,+,+,+ ".
    "CDEF:full=100,total_verified,- ".
    "AREA:ds0a#00d10a:\"Verified\" ".
    "STACK:full#FF0000:\"Not verified\" ".
    "STACK:ds0d#000000:\"Robots\" ".
    "STACK:ds0b#afafee:\"WV users\" ".
    "STACK:ds0c#0b00a9:\"Trusted users\" ".
    "COMMENT:'Curr\: ' ".
    "GPRINT:total_verified:LAST:\"%2.0lf%%\" ".
    "> /dev/null");
    
  /*
    We SHIFT 10 days data because we log it as being current,
    although it isn't really (in fact it's 10 days in the past, duh).

    And we subtract fluff from old data because said fluff is
    being visually graphed above, and we need to show it in its
    proper context.
  */
  // V%/2w
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vpc_2wk.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-14d ".
    "--width $width14d ".
    "--height $height14d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "--upper-limit 100 ".
    "--lower-limit 0 ".
    "--rigid ".
    "DEF:ds0a=$rra2:verified_percent:AVERAGE ".
    "DEF:ds0b=$rra2:user_percent:AVERAGE ".
    "DEF:ds0c=$rra2:familiar_percent:AVERAGE ".
    "DEF:ds0d=$rra2:robot_percent:AVERAGE ".
    "DEF:ds0e=$rra2:old_percent:AVERAGE ".
    "SHIFT:ds0e:-864000 ".
    "CDEF:total_verified=ds0a,ds0b,ds0c,ds0d,+,+,+ ".
    "CDEF:full=100,total_verified,- ".
    "CDEF:old=ds0e,ds0b,-,ds0c,-,ds0d,- ".
    "AREA:ds0a#00d10a:\"Verified\" ".
    "STACK:full#FF0000:\"Not verified\" ".
    "STACK:ds0d#000000:\"Robots\" ".
    "STACK:ds0b#afafee:\"WV users\" ".
    "STACK:ds0c#0b00a9:\"Trusted users\" ".
    "LINE1:old#ffcc00:\"Verified 10 days later\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:total_verified:LAST:\"%2.0lf%%\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:total_verified:MIN:%2.0lf%% ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:total_verified:MAX:%2.0lf%% ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:total_verified:AVERAGE:%2.2lf%% ".
    "> /dev/null");
    
  // V%/30d
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vpc_30d.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-30d ".
    "--width $width30d ".
    "--height ".($height30d+42)." ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "--upper-limit 100 ".
    "--lower-limit 0 ".
    "--rigid ".
    "DEF:ds0a=$rra2:verified_percent:AVERAGE ".
    "DEF:ds0b=$rra2:user_percent:AVERAGE ".
    "DEF:ds0c=$rra2:familiar_percent:AVERAGE ".
    "DEF:ds0d=$rra2:robot_percent:AVERAGE ".
    "DEF:ds0e=$rra2:old_percent:AVERAGE ".
    "SHIFT:ds0e:-864000 ".
    "CDEF:total_verified=ds0a,ds0b,ds0c,ds0d,+,+,+ ".
    "CDEF:full=100,total_verified,- ".
    "CDEF:old=ds0e,ds0b,-,ds0c,-,ds0d,- ".
    "AREA:ds0a#00d10a:\"Verified\" ".
    "STACK:full#FF0000:\"Not verified\" ".
    "STACK:ds0d#000000:\"Robots\" ".
    "STACK:ds0b#afafee:\"WV users\" ".
    "STACK:ds0c#0b00a9:\"Trusted users\" ".
    "LINE1:old#ffcc00:\"Verified 10 days later\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:total_verified:LAST:\"%2.0lf%%\" ".
    "COMMENT:'|   Min\: ' ".
    "GPRINT:total_verified:MIN:%2.0lf%% ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:total_verified:MAX:%2.0lf%% ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:total_verified:AVERAGE:%2.2lf%% ".
    "> /dev/null");

  // V%/1y
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vpc_1y.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-1y ".
    "--width $width1y ".
    "--height $height1y ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "--upper-limit 100 ".
    "--lower-limit 0 ".
    "--rigid ".
    "DEF:ds0a=$rra2:verified_percent:AVERAGE ".
    "DEF:ds0b=$rra2:user_percent:AVERAGE ".
    "DEF:ds0c=$rra2:familiar_percent:AVERAGE ".
    "DEF:ds0d=$rra2:robot_percent:AVERAGE ".
    "DEF:ds0e=$rra2:old_percent:AVERAGE ".
    "SHIFT:ds0e:-864000 ".
    "CDEF:total_verified=ds0a,ds0b,ds0c,ds0d,+,+,+ ".
    "CDEF:full=100,total_verified,- ".
    "CDEF:old=ds0e,ds0b,-,ds0c,-,ds0d,- ".
    "AREA:ds0a#00d10a:\"Verified\" ".
    "STACK:full#FF0000:\"Not verified\" ".
    "STACK:ds0d#000000:\"Robots\" ".
    "STACK:ds0b#afafee:\"WV users\" ".
    "STACK:ds0c#0b00a9:\"Trusted users\" ".
    "LINE1:old#ffcc00:\"Verified 10 days later\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:total_verified:LAST:\"%2.2lf%%\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:total_verified:MIN:%2.2lf%% ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:total_verified:MAX:%2.2lf%% ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:total_verified:AVERAGE:%2.2lf%% ".
    "> /dev/null");

  // Verifications/24h
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vrf_24h.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-24h ".
    "--width $width24h ".
    "--height $height24h ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:verifications:AVERAGE ".
    "LINE2:ds0a#00DD00:\"per 24h\" ".
    "COMMENT:'Curr\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\" ".
    "> /dev/null");    
    
  // Verifications/2w
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vrf_2wk.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-14d ".
    "--width $width14d ".
    "--height $height14d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:verifications:AVERAGE ".
    "AREA:ds0a#00DD00:\"Verifications per 2w\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:ds0a:MIN:%2.0lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.0lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");
    
  // Verifications/30d
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vrf_30d.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-30d ".
    "--width $width30d ".
    "--height $height30d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:verifications:AVERAGE ".
    "AREA:ds0a#00DD00:\"Verifications per 30d\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\" ".
    "COMMENT:'|   Min\: ' ".
    "GPRINT:ds0a:MIN:%2.0lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.0lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");

  // Verifications/1y
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_vrf_1y.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-1y ".
    "--width $width1y ".
    "--height $height1y ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:verifications:AVERAGE ".
    "AREA:ds0a#009900:\"Verifications per 1y\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.2lf\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:ds0a:MIN:%2.2lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.2lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");

  // Users/24h
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_usr_24h.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-24h ".
    "--width $width24h ".
    "--height $height24h ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:ucount:AVERAGE ".
    "DEF:ds0b=$rra:ucount:AVERAGE:end=now-24h:start=end-24h ".
    "SHIFT:ds0b:86400 ".
    "AREA:ds0b#FFCCCC:\"prev 24h\" ".
    "LINE2:ds0a#DD0000:\"last 24h\" ".
    "> /dev/null");    
    
  // Users/2w
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_usr_2wk.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-14d ".
    "--width $width14d ".
    "--height $height14d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:ucount:AVERAGE ".
    "AREA:ds0a#DD0000:\"Users per 2w\" ".
    "COMMENT:'| Current\: '  ".
    "GPRINT:ds0a:LAST:\"%2.0lf\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:ds0a:MIN:%2.0lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.0lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");
    
  // Users/30d
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_usr_30d.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-30d ".
    "--width $width30d ".
    "--height $height30d ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:ucount:AVERAGE ".
    "AREA:ds0a#DD0000:\"Users per 30d\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf\" ".
    "COMMENT:'|   Min\: ' ".
    "GPRINT:ds0a:MIN:%2.0lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.0lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");

  // Users/1y
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_usr_1y.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-1y ".
    "--width $width1y ".
    "--height $height1y ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "DEF:ds0a=$rra:ucount:AVERAGE ".
    "LINE1:ds0a#990000:\"Users per 1y\" ".
    "COMMENT:'| Current\: ' ".
    "GPRINT:ds0a:LAST:\"%2.2lf\\n\" ".
    "COMMENT:'    Min\: ' ".
    "GPRINT:ds0a:MIN:%2.2lf ".
    "COMMENT:'| Max\: ' ".
    "GPRINT:ds0a:MAX:%2.2lf ".
    "COMMENT:'| Avg\: ' ".
    "GPRINT:ds0a:AVERAGE:%2.2lf ".
    "> /dev/null");

  // Users 24h average over 1w
  /*
    We need to shift both the data source and the graph
    data for a sensible reason: shifting only the data source
    would result bringing past data to TODAY; shifting only
    the graph would bring TODAY's data to the past; since
    we want to bring past data to the past, we need to do
    both simultaneously.
  */
  exec("rrdtool graph ".
    "$cDir/images/wv_graph_usr_1w24h.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-24h ".
    "--width ".($width30d+$width24h+5)." ".
    "--height $height1y ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "--lower-limit 0 ".
    "DEF:dsd7=$rra:ucount:AVERAGE ".
    "DEF:dsd6=$rra:ucount:AVERAGE:end=now-24h:start=end-24h ".
    "DEF:dsd5=$rra:ucount:AVERAGE:end=now-48h:start=end-24h ".
    "DEF:dsd4=$rra:ucount:AVERAGE:end=now-72h:start=end-24h ".
    "DEF:dsd3=$rra:ucount:AVERAGE:end=now-96h:start=end-24h ".
    "DEF:dsd2=$rra:ucount:AVERAGE:end=now-120h:start=end-24h ".
    "DEF:dsd1=$rra:ucount:AVERAGE:end=now-144h:start=end-24h ".
    "SHIFT:dsd6:86400 ".
    "SHIFT:dsd5:172800 ".
    "SHIFT:dsd4:259200 ".
    "SHIFT:dsd3:345600 ".
    "SHIFT:dsd2:432000 ".
    "SHIFT:dsd1:518400 ".
    "CDEF:ds1w=dsd1,dsd2,+,dsd3,+,dsd4,+,dsd5,+,dsd6,+,dsd7,+,7,/ ".
    "AREA:dsd7#FFEEEE:\"last 24h\" ".
    "LINE2:ds1w#DD0000:\"avg over 1w\" ".
    "> /dev/null");


  cleanexit();

  function trimdate($date)
  {
    return strtotime(substr($date,0,-5).'00:00');
  }

  function cleanexit()
  {
    wv_query("
      INSERT INTO hits
      SET
        hdate=NOW(),
        htype='cron'
    ");
    if (rand(0,100)>0) {
      wv_query("
        DELETE FROM hits
        WHERE
          htype='cron' AND
          date_add(hdate, interval 31 day)<NOW()
      ");
      $r=wv_query("
        SELECT MIN(id) FROM hits
      ");
      $a=mysql_fetch_array($r);
      if ($a['id']>1) {
        $delta=$a['id']-1;
        wv_query("
          UPDATE hits SET id=id-$delta
        ");
        wv_query("
          ALTER TABLE hits AUTO_INCREMENT = 1
        ");
      }
    }
  }

?>
