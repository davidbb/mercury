<?php
$PageTitle = "QRPass Forgot Password";
include_once("include/db.php");
include_once("include/header.inc");
opendb();
function print_form($message="Please enter user name to recover. Note that this system is merely the counterpart to the proof of concept Android application for <i>How to Live with 'I Forgot My Password'</i>, a HotSec submission.") {
	$name = $_POST["username"];
	print "<form method=\"post\" action=\"forgot.php\">
	<p class=\"message\">$message</p>
	<label>User Name<span class=\"small\">[A-Za-z0-9] no spaces</span></label>
	<input type=\"text\" name=\"username\" id=\"username\" value=\"$name\" />
	<button type=\"submit\" value=\"Submit\" name=\"submit\">Submit</button>
	</form>
	<div class=\"spacer\"></div>";
}
print "<h1>$PageTitle</h1>";

$username = htmlspecialchars($_POST["username"]);
if (isset($_POST["username"]) && $username != "") {
	
	//generate the encrypted text from the pubkey we have in the database
	$result = mysql_query("SELECT password, pubkey FROM qrpass.users WHERE username='$username'");

	$row = mysql_fetch_array($result);
	$pubkey = $row['pubkey'];
	$password = $row['password'];
	if ($pubkey == NULL) {
		//warn user to send their public key
		print_form("Invalid user name, or no key found to generate your QRPass, please login and submit your public key.");
	} else {
		//generate the qrcode
		//use $pubkey and $password
		//echo "<p>$ciphertext</p>";
		print "<p class=\"message\">The following is your encrypted password, scan it with the QRPass Android application, and your password will be revealed on your device.</p>";
		$pubkey = escapeshellarg($pubkey);
		$password = escapeshellarg($password);
		$ciphertext = exec("/usr/bin/java -jar encrypter.jar $pubkey $password");
		print "<img src=\"http://chart.apis.google.com/chart?cht=qr&chs=350x350&chl=QRPASS%3A$ciphertext\" />";
	}
} elseif ($username == "") {
	print_form("No user name specified.");
} else {
	print_form();
}
include_once("include/footer.inc");
closedb();
?>
