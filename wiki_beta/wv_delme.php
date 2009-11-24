<?

  $cDir=dirname(__FILE__);
  $width1y=365;
  $width14d=336;
  $width24h=96;
  $height1y=$height14d=75;

  $width30d=600;
  $height24h=$height30d=50;
  $pDate=strtotime(date('H').':00 -1 hour');
  $rra2=$cDir."/wv_stat.rra";

  exec("rrdtool graph ".
    "$cDir/wv_graph_vpc_24h.png ".
    "--imgformat PNG ".
    "--end $pDate ".
    "--start end-24h ".
    "--width $width24h ".
    "--height $height24h ".
    "--color MGRID#A0A0A0 ".
    "--color GRID#DADADA ".
    "--upper-limit 100 ".
    "--lower-limit 0 ".
    "DEF:ds0a=$rra2:verified_percent:AVERAGE ".
    "CDEF:full=100,ds0a,- ".
    "AREA:ds0a#00DD00:\"validated per 24h\" ".
    "STACK:full#DD0000:\"not validated\" ".
    "COMMENT:'Curr\: ' ".
    "GPRINT:ds0a:LAST:\"%2.0lf%%\" ".
    "> /dev/null");

?>
