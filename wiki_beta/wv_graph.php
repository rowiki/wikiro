<html>
<head>
  <title>WikiVerifier Graphs</title>
  <?
    // June 2007
    $cDir=dirname(__FILE__);
    $rra=$cDir."/wv_data.rra";
    $r_safe=10; // a "safe" delay for the next refresh
    $last_refresh=filemtime($rra);
    $current_delta=time()-$last_refresh;
    $nominal_delta=3600;
    $refresh=$nominal_delta-$current_delta;
    $refresh=($refresh>0)?$refresh:0;
    echo("<meta http-equiv='refresh' content='".($refresh+$r_safe)."'>");
  ?>
</head>
<body>
  <h1 style='text-align:center; font-size:12pt'>
    WikiVerifier Graphs
  </h1>
  <div align=center style='margin-bottom:8px; margin-top:-5px'><small>[All data in items/hour]</small></div>

  <table align=center bgcolor='#c0ffc0'>
  <tr>
    <td colspan=2 align=center><small><b>Verifications</b> &mdash; how many small squares were shown in Recent Pages, etc (measures overall activity)</small></td>
  </tr>
  <tr>
    <td>
      <img src='images/wv_graph_vrf_2wk.png'>
    </td><td>
      <img src='images/wv_graph_vrf_1y.png'>
    </td>
  </tr>
  <tr>
    <td colspan=2>
      <img src='images/wv_graph_vrf_24h.png'>
      <img src='images/wv_graph_vrf_30d.png'>
    </td>
  </tr>
  </table>

  <table align=center bgcolor='#ddddff' style="margin-top:3px">
  <tr>
    <td colspan=2 align=center><small><b>Assignments</b> &mdash; how many large squares were shown in the differences page (measures validations)</small></td>
  </tr>
  <tr>
    <td>
      <img src='images/wv_graph_ass_2wk.png'>
    </td><td>
      <img src='images/wv_graph_ass_1y.png'>
    </td>
  </tr>
  <tr>
    <td colspan=2>
      <img src='images/wv_graph_ass_24h.png'>
      <img src='images/wv_graph_ass_30d.png'>
    </td>
  </tr>
  </table>
  
  <table align=center bgcolor='#99dd99' style="margin-top:3px">
  <tr>
    <td colspan=2 align=center><small><b>Verification percent</b> &mdash; what proportion of the known modifications over the past 24 hours have been verified by now (measures efficiency)</small></td>
  </tr>
  <tr>
    <td>
      <img src='images/wv_graph_vpc_2wk.png'>
    </td><td>
      <img src='images/wv_graph_vpc_1y.png'>
    </td>
  </tr>
  <tr>
    <td colspan=2>
      <img src='images/wv_graph_vpc_24h.png'>
      <img src='images/wv_graph_vpc_30d.png'>
    </td>
  </tr>
  </table>
  
  <table align=center bgcolor='#ffdddd' style="margin-top:3px">
  <tr>
    <td colspan=2 align=center><small><b>Users</b> &mdash; how many users were active (measures user coverage over the clock)</small></td>
  </tr>
  <tr>
    <td>
      <img src='images/wv_graph_usr_2wk.png'>
    </td><td>
      <img src='images/wv_graph_usr_1y.png'>
    </td>
  </tr>
  <tr>
    <td colspan=2>
      <img src='images/wv_graph_usr_24h.png'>
      <img src='images/wv_graph_usr_30d.png'>
    </td>
  </tr>
  <tr>
    <td colspan=2>
      <img src='images/wv_graph_usr_1w24h.png'>
    </td>
  </tr>
  </table>
  
  <p align='center'><small>
    This page shows the assignment/verification graphs for
    <a href='./'>WikiVerifier</a>.
  </small></p>
  <hr>
  <p align='center'><small>
    Data last updated <?= date('r',filemtime($rra)) ?>;
    Next refresh: <?= date('r',time()+$refresh+$r_safe) ?>
  </small></p>
</body>
