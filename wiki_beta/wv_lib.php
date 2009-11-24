<?

  /**
  * WikiVerifier library, Bogdan Stancescu <gutza@moongate.ro>, June 2007
  * See LICENSE file for license
  */

  require_once("wv_config.php");

  register_shutdown_function("wv_log",NULL,true);

  $vw_connected=false;

  function wv_connect()
  {
    return mysql_connect(WV_DB_SERVER,WV_DB_USER,WV_DB_PWD,0,0);
  }

  function wv_select_db()
  {
    return mysql_select_db(WV_DB_NAME);
  }

  function wv_query($sql)
  {
    global $wv_connected;
    if (!$wv_connected) {
      if (!wv_connect()) {
        wv_log("Failed to connect to MySQL!",true);
      }
      if (!wv_select_db()) {
        wv_log("Failed to select database!",true);
      }
      $wv_connected=true;
    }
    $h=mysql_query($sql);
    if ($h) {
      return $h;
    }

    wv_log("SQL Error: ".mysql_error()."\n(query=".$sql.")");
  }

  // We assume $uname and $pwd are unescaped
  function wv_adduser($uname,$pwd)
  {
    if (!$uname || !$pwd) {
      return false;
    }
    $r1=wv_query('
      INSERT INTO people
      SET
        uname="'.mysql_real_escape_string($uname).'"
    ');
    if ($r1) {
      $id=mysql_insert_id();
      $r2=wv_query('
        UPDATE people
        SET
          pwd="'.md5($pwd.md5($id)).'
      ');
    }
    return $r1 && $r2;
  }

  class wv_user {

    var $id=0;
    var $uname='';

    function wv_user()
    {
      $this->auth();
    }

    /**
    * This checks if a user is already logged in, or if a proper
    * login just took place.
    * @result mixed (int) user ID if proper login or (bool) false otherwise
    */
    function auth()
    {
      session_set_cookie_params(3600*24*365);
      ini_set('session.save_path','/tmp/wv');
      ini_set('session.gc_maxlifetime',3600*24*365);
      ini_set('session.cache_expire',60*24*365);
      session_start();
      if ($this->id) {
        return $this->id;
      }
      if ($_SESSION['user_id']) {
        $this->id=$_SESSION['user_id'];
        // It should be safe to assume the uname has been
        // populated as well in the session
        $this->uname=$_SESSION['user_name'];
        $this->rights=$_SESSION['rights'];
        return $_SESSION['user_id'];
      }
      if ($this->_login()) {
        return $this->id;
      }
      return false;
    }

    /**
    * Private -- checks whether a login just took place.
    * You should use {@link wv_auth} instead.
    */
    function _login()
    {
      if (!$_REQUEST['uname'] || !$_REQUEST['pwd']) {
        return false;
      }
      $r=wv_query('
        SELECT
          id,pwd,uname,rights
        FROM people
        WHERE
          uname="'.wv_slash($_REQUEST['uname']).'" AND
          disabled=0
        '
      );
      $a=mysql_fetch_array($r);
      if (!$a['id']) {
        return false;
      }
      if (md5(wv_unslash($_REQUEST['pwd']).md5($a['id']))==$a['pwd']) {
        $_SESSION['user_id']=$this->id=$a['id'];
        $_SESSION['user_name']=$this->uname=$a['uname'];
        $_SESSION['rights']=$this->rights=$a['rights'];
        wv_query('
          UPDATE people
          SET
           prev_login=last_login, last_login=NOW() 
        ');
        return $this->id;
      }
      return false;
    }

    function hasRight($right)
    {
      return (bool) strstr($this->rights,$right);
    }
  }

  function wv_unslash($str)
  {
    if (!get_magic_quotes_gpc()) {
      return $str;
    }
    return stripslashes($str);
  }

  function wv_slash($str)
  {
    if (get_magic_quotes_gpc()) {
      return $str;
    }
    return addslashes($str);
  }

/*
  function wv_log($msg,$exit=false)
  {
    echo $msg."\n";
    if ($exit) {
      exit;
    }
    return "boobies"; // what?
  }
*/

  function register_hit($type)
  {
    global $u;
    if (is_object($u)) {
      $uid=abs($u->id);
    } else {
      $uid=0;
    }
    return (bool) wv_query("
      INSERT INTO hits
      SET
        hdate=now(),
        htype=\"".addslashes($type)."\",
        user_id=$uid;
    ");
  }

  function wv_infer($old_id,$new_id)
  {
    if ($new_id=='next') {
      $which_id='old_id';
    } else {
      // assuming $new_id='prev'
      $which_id='new_id';
    }
    $query="
      SELECT old_id,new_id
      FROM diffs
      WHERE
        $which_id=\"".addslashes($old_id)."\" AND
        successive=1
    ";
    $r=wv_query($query);
    $a=mysql_fetch_array($r);
    return array($a['old_id'],$a['new_id']);
  }

  function wv_log($msg,$close=false)
  {
    static $fp,$fname;
    if (!$fname) {
      $fname="/tmp/wv_log_".getmypid().".txt";
    }
    $logfile="/tmp/wv_log.txt";
    if ($close) {
      if (!$fp) {
        return NULL;
      }
      fclose($fp);
      exec("cat \"$fname\" >> \"$logfile\"; rm -f \"$fname\"");
      return true;
    }
    if ($fp===NULL) {
      $fp=@fopen($fname,'a');
      if ($fp) {
        fputs($fp,"-------- ".date('r')." ----------\n");
      }
    }
    if (!$fp) {
      return false;
    }
    fputs($fp,$msg."\n");
    return true;
  }
?>
