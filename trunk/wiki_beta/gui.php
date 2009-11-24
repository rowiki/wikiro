<?
  require_once("wv_lib.php");
  $u=new wv_user();
  require('login.php');
?>
<a href='index.php'>WikiVerifier Home</a>
&mdash;
<a href='http://ro.wikipedia.org/wiki/Special:Recentchanges'>Wikipedia Recent Changes</a>
&mdash;
<a href='logout.php'>Log out</a>
<hr>
<a href="resetownpwd.php"
  onClick='return confirm("Do you really want to change your password?");'
>Change my password</a>
&mdash;
<a href="wv_graph.php">Usage graphs</a>
&mdash;
<a href='legend.php'>Legend</a>
<?
  if ($u->hasRight('B')) {
?>
&mdash;
<a href="useradmin.php">User Administration</a>
<?
  }
?>
&mdash;
<a href="familiars.php">Trusted users</a>
