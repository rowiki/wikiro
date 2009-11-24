<?
  $wikipedia='ro';
  $user_space_name='Utilizator';
  $debug=false;

  require_once('wv_lib.php');

  function decho($msg)
  {
    global $debug;
    if (!$debug) {
      return false;
    }
    echo $msg."\n";
    return true;
  }

  if ($_SERVER['REMOTE_ADDR']) {
    echo "This is an internal script.";
    exit;
  }

  decho("Retrieving robot list from Wikipedia...");
  $url='http://'.$wikipedia.'.wikipedia.org/w/index.php?title=Special:Listusers&limit=5000&group=bot';
  exec(
    'wget -q -O - "'.$url.'" | '.
    'grep -Eo "[^/]'.$user_space_name.':[^\"]+" | '.
    'sed s/^\"'.$user_space_name.'://',
    $current_list
  );
  if (!$current_list) {
    echo "Failed retrieving robot list from $url\n";
    exit;
  }

  decho("Retrieving local robot list from database...");
  $r=wv_query("
    SELECT
      id,uname
    FROM
      robots
    WHERE
      disabled=0
    ORDER BY
      uname
  ");
  $db_list=array();
  while($a=mysql_fetch_array($r)) {
    $db_list[]=$a['uname'];
  }

  $new_robots=array_diff($current_list,$db_list);
  $obsolete_robots=array_diff($db_list,$current_list);

  decho("");

  if ($new_robots) {
    decho("New robots found, adding...");
    foreach($new_robots as $robot) {
      $h=wv_query("
        SELECT
          id
        FROM
          robots
        WHERE
          uname='".addslashes($robot)."' AND
          disabled=1
      ");
      $a=mysql_fetch_array($h);
      if ($a) {
        decho("Robot $robot was obsolete in the local database -- enabling.");
        wv_query("
          UPDATE
            robots
          SET
            disabled=0
          WHERE
             uname='".addslashes($robot)."'
        ");
      } else {
        decho("Importing $robot");
        wv_query("
          INSERT INTO robots
          SET
            addedon=NOW(),
            uname='".addslashes($robot)."',
            disabled=0
        ");
      }
      wv_query("
        UPDATE
          familiars
        SET
          disabled=1
        WHERE
          uname='".addslashes($robot)."'
      ");
    }
  }

  if ($obsolete_robots) {
    decho("Obsolete robots found, disabling...");
    foreach($obsolete_robots as $robot) {
      decho("Obsoleting $robot");
      wv_query("
        UPDATE
          robots
        SET
          disabled=1
        WHERE
          uname='".addslashes($robot)."'
      ");
    }
  }

  if (!$obsolete_robots && !$new_robots) {
    decho("No changes found.");
  }

  decho("Clean exit");
