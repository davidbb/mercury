<?php
function pagePath() {
	$pageURL = 'http';
	if ($_SERVER["HTTPS"] == "on") {$pageURL .= "s";}
	$pageURL .= "://";
	if ($_SERVER["SERVER_PORT"] != "80") {
		$pageURL .= $_SERVER["SERVER_NAME"].":".$_SERVER["SERVER_PORT"].$_SERVER["REQUEST_URI"];
	} else {
		$pageURL .= $_SERVER["SERVER_NAME"].$_SERVER["REQUEST_URI"];
	}
	$pageURL = preg_replace('/\/[A-Za-z0-9_\-.]+\.php/','',$pageURL);
	$pageURL .= "/";
	return $pageURL;
}
function opendb() {
	$dbuser="";
	$dbpass="";
	$dbdatabase="qrpass";
	$dbserver="localhost";
	mysql_connect($dbserver,$dbuser,$dbpass) or die("couldn't connect to $dbserver");
	mysql_select_db($dbdatabase);
}

function closedb() {
	mysql_close();
}
?>
