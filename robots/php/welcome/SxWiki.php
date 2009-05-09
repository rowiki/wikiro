<?php
 
function setEPM($epm) {
        $epm = 60 / $epm;
        return $epm;
}
 
function sxLogin($username, $password) {
        global $url;
        if (!isset($url)) { die("\r\nConfiguration variables not set\r\n"); }
        $request = $url . 'api.php?action=login&lgname=' . urlencode($username) . '&lgpassword=' . urlencode($password) . '&format=php';
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $request);
        curl_setopt($ch, CURLOPT_HEADER, false);
        curl_setopt($ch, CURLOPT_COOKIEJAR, "cookiejar.txt");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);

      curl_setopt($ch, CURLOPT_POST, 1);
      curl_setopt($ch, CURLOPT_POSTFIELDS, "wpName=".$username."&wpPassword=".$password."&wpLoginattempt=true");

        $sxLgInput = curl_exec($ch);
        curl_close($ch);
        $sxLgI = unserialize($sxLgInput);
        $result = $sxLgI[login][result];
        if ($result != "Success") {
                $failed = "Login failed: $result\n";
                die($failed);
        } else {
//                echo "Logged in as $username \r\n";
        }
        $sxLgIA = array("token" => $sxLgI[login][lgtoken], "uid" => $sxLgI[login][lguserid]);
        return($sxLgIA);
}
 
function sxGetPage($article) {
        global $url;
        if (!isset($url)) { die("\r\nConfiguration variables not set\r\n"); }
        $article = urlencode($article);
        $request = $url . 'api.php?action=query&prop=revisions&titles=' . $article . '&rvprop=content&format=php';
        $sxGetArticle = file_get_contents($request);
        $sxGetA = unserialize($sxGetArticle);
        $sxGetAID = $sxGetA;
        $sxGetAID = array_shift($sxGetAID);
        $sxGetAID = array_shift($sxGetAID);
        $sxGetAID = array_shift($sxGetAID);
        $sxGetAID = array_shift($sxGetAID);
        $sxAText = $sxGetA[query][pages][$sxGetAID][revisions][0]["*"];
        return($sxAText);
}
 
function sxGetUrl($url) {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt ($ch, CURLOPT_COOKIEJAR, 'cookiejar.txt');
        curl_setopt ($ch, CURLOPT_COOKIEFILE, 'cookiejar.txt');
        curl_setopt($ch, CURLOPT_URL, $url);
        $response = curl_exec($ch);
	return $response;
}

function sxPutPage($article, $editsum, $newtext, $minor) {
        global $maxlag;
        global $url;
        global $epm;
        if (!isset($epm) || !isset($url) || !isset($maxlag)) { die("\r\nConfiguration variables not set\r\n"); }
        $article = urlencode($article);
        $postrequest = $url . 'index.php?action=edit&title=' . $article;
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt ($ch, CURLOPT_COOKIEJAR, 'cookiejar.txt');
        curl_setopt ($ch, CURLOPT_COOKIEFILE, 'cookiejar.txt');
        curl_setopt($ch, CURLOPT_URL, $postrequest);
        $response = curl_exec($ch);
        preg_match('/\<input type\=\\\'hidden\\\' value\=\"(.*)\" name\=\"wpStarttime\" \/\>/i', $response, $starttime);
        preg_match('/\<input type\=\\\'hidden\\\' value\=\"(.*)\" name\=\"wpEdittime\" \/\>/i', $response, $edittime);
        preg_match('/\<input name\=\"wpAutoSummary\" type\=\"hidden\" value\=\"(.*)\" \/\>/i', $response, $autosum);
        preg_match('/\<input type\=\'hidden\' value\=\"(.*)\" name\=\"wpEditToken\" \/\>/i', $response, $token);

//echo "starttime: {$starttime[1]}; edittime: {$edittime[1]}\n";
        
	curl_close($ch);
        $postrequest = $url . 'index.php?title=' . $article . '&action=submit&maxlag=' . $maxlag;
        $postData['wpScrolltop'] = '';
        $postData['wpSection'] = '';
        $postData['wpEditToken'] = $token[1];
        $postData['wpAutoSummary'] = $autosum[1];
        $postData['wpSummary'] = $editsum;
        $postData['wpTextbox1'] = $newtext;
        $postData['wpSave'] = "Save page";
        $postData['wpStarttime'] = $starttime[1];
        $postData['wpEdittime'] = $edittime[1];
        if ($minor != null) { $postData['wpMinoredit'] = $minor; }
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $postrequest);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt ($ch, CURLOPT_COOKIEJAR, 'cookiejar.txt');
        curl_setopt ($ch, CURLOPT_COOKIEFILE, 'cookiejar.txt');
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        $response = curl_exec($ch);
        if (preg_match('/^Waiting for (.*) seconds lagged/', $response)) {
                echo "Maxlag hit, not posted\r\n";
                $returnval = false;
        } else {
                $returnval = true;
        }
        if(curl_errno($ch))
          {
            print curl_error($ch);
          }
        curl_close($ch);
	sleep($epm);
        return($returnval);
}
 
function sxGetCat($categoryname, $ns) {
        global $url;
	$fcat = array();
        if (!isset($url)) { die("\r\nConfiguration variables not set\r\n"); }
        $categoryname = urlencode($categoryname);
        $frequest = $url . 'api.php?action=query&list=categorymembers&cmcategory=' . $categoryname . '&format=php&cmlimit=500';
        $finalcat = getit($frequest);
        if(isset($finalcat['query-continue']['categorymembers']['cmcontinue'])) {
                $firstrun = 1;
		$catfrom = $finalcat['query-continue']['categorymembers']['cmcontinue'];
		$catfrom = urlencode($catfrom);
        } else {
                $firstrun = 0;
        }
        foreach($finalcat[query][categorymembers] as $fcat_l) {
	        array_push($fcat, $fcat_l);
        }

        while($done != 1) {
                if(isset($cat['query-continue']['categorymembers']['cmcontinue']) || $firstrun == 1) {
                        $request = $url . 'api.php?action=query&list=categorymembers&cmcategory=' . $categoryname.'&format=php&cmlimit=500&cmcontinue=' . $catfrom;
                        $cat = getit($request);
			$catfrom = $cat['query-continue']['categorymembers']['cmcontinue'];
			$catfrom = urlencode($catfrom);
			foreach($cat[query][categorymembers] as $fcat_l) {
				array_push($fcat, $fcat_l);
			}
                        echo ".";
                        $firstrun = 0;
                } else {
                        $done = 1;
                }
        }
        $result = array();
        foreach($fcat as $catmemb) {
		if ($catmemb[ns] == $ns || $ns == "all") {
	                array_push($result, $catmemb[title]);
		}
        }
        return($result);
}

function sxGetPrefix($prefixname) {
	$o_prefixname = $prefixname;
        $result = array();
	$searchpf = '/^' . $o_prefixname . '/';
        global $url;
        if (!isset($url)) { die("\r\nConfiguration variables not set\r\n"); }
        $prefixname = urlencode($prefixname);
        $frequest = $url . 'api.php?action=query&list=allpages&apfrom=' . $prefixname . '&format=php&aplimit=500';
        $finalpre = getit($frequest);
        if(isset($finalpre['query-continue']['allpages']['apfrom'])) {
                $firstrun = "1";
		$prefart = urlencode($finalpre['query-continue']['allpages']['apfrom']);
        } else {
                $firstrun = "0";
        }
	foreach($finalpre[query][allpages] as $finalpre_l) {
		if(!preg_match($searchpf, $finalpre_l[title])) {
			$done = 1;
		} else {
			array_push($result, $finalpre_l[title]);
		}
	}

        while($done != 1) {
                if(isset($pref['query-continue']['allpages']['apfrom']) || $firstrun == "1") {
                        $request = $url . 'api.php?action=query&list=allpages&apfrom=' . $prefixname.'&format=php&aplimit=500&apfrom=' . $prefart;
                        $pref = getit($request);
	                $prefart = urlencode($pref['query-continue']['allpages']['apfrom']);
			foreach($pref[query][allpages] as $pref_l) {
				if(!preg_match($searchpf, $pref_l[title])) {
					$done = 1;
				} else {
					array_push($result, $pref_l[title]);
				}
			}
                        echo ".";
                        $firstrun = 0;
                } else {
                        $done = 1;
                }
        }
        return($result);
}

function sxLastEdited($article) {
        global $url;
        $article = urlencode($article);
        $request = $url . 'api.php?action=query&prop=revisions&titles=' . $article . '&rvprop=user|comment&rvlimit=1&format=php';
        $sxGetArticle = file_get_contents($request);
        $sxGetA = unserialize($sxGetArticle);
        $sxGetAID = $sxGetA;
        $sxGetAID = array_shift($sxGetAID);
        $sxGetAID = array_shift($sxGetAID);
        $sxGetAID = array_shift($sxGetAID);
        $sxGetAID = array_shift($sxGetAID);
	$sxAText = array();
        $sxAText[user] = $sxGetA[query][pages][$sxGetAID][revisions][0][user];
	$sxAText[editsum] = $sxGetA[query][pages][$sxGetAID][revisions][0][comment];
        return($sxAText);
}
 
function getit ($query) {
        $result = file($query);
        $result = unserialize($result[0]);
        return($result);
}
 
?>
