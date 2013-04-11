<?php
include_once("include/db.php");
opendb();

$username = htmlspecialchars($_GET["username"]);
$random = htmlspecialchars($_GET["random"]);
$pubkey = htmlspecialchars($_POST["pubkey"]);
if (isset($_POST["send_key"])) {
	$result = mysql_query("UPDATE qrpass.users SET pubkey='$pubkey' WHERE username='$username' AND random='$random'") or die("FAIL");
	if (!$result) {
		echo "FAIL";
	}
	echo "SUCCESS";
	$random = rand(0, 2147483647);
	mysql_query("UPDATE qrpass.users SET random='$random' WHERE username='$username'") or die("FAIL");
}
closedb();
?>
