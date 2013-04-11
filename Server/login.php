<?php
$PageTitle = "QRPass Log In";
include_once("include/db.php");
include_once("include/header.inc");
opendb();
function print_form($message="Please enter login information below. Note that this system is merely the counterpart to the proof of concept Android application for <i>How to Live with 'I Forgot My Password'</i>, a HotSec submission.") {
	$name = $_POST["username"];
	print "<form method=\"post\" action=\"login.php\">
	<p class=\"message\">$message<br /><br />New user? <a href=\"register.php\">Register an account</a>.</p>
	<label>User Name<span class=\"small\">[A-Za-z0-9] no spaces</span></label>
	<input type=\"text\" name=\"username\" id=\"username\" value=\"$name\" />
	<label>Password<span class=\"small\">Anything you like ;-)</span></label>
	<input type=\"password\" name=\"password\" id=\"password\" />
	<div class=\"forgot\"><a href=\"forgot.php\">Forgot password?</a></div>
	<button type=\"submit\" value=\"Submit\" name=\"submit\">Submit</button>
	</form>
	<div class=\"spacer\"></div>";
}


print "<h1>$PageTitle</h1>";

$username = htmlspecialchars($_POST["username"]);
$password = htmlspecialchars($_POST["password"]);
if (!isset($_POST["submit"])) {
	print_form();
} else {
	$result = mysql_query("SELECT COUNT(*) FROM qrpass.users WHERE username='$username' AND password='$password'");
	$count = mysql_result($result,0,0);
	if ($count > 0) {
		echo "<p class=\"message\">Log in successful, ".$username."!</p>";
		$result = mysql_query("SELECT random, pubkey FROM qrpass.users WHERE username='$username'");
		$row = mysql_fetch_array($result);
		$pubkey = $row['pubkey'];
		$random = $row['random'];
		if ($pubkey == NULL) {
			//pagePath() gives only the path of this PHP file, and does NOT include "filename.php"
			$send_key_url = urlencode(pagePath()."send_key.php");
?>
			<p>Next, let's submit your public key using the QRPass application on your 
			phone. This is necessary to recover a forgotten password. Open it up and select <b>Send public key</b> from the menu. Scan the
			following barcode:</p>
			<img src="http://chart.apis.google.com/chart?cht=qr&chs=350x350&chl=<?php echo $send_key_url;?>%3Fusername%3D<?php echo $username;?>%26random%3D<?php echo $random;?>" />
<?php
		}
	} else {
		print_form("Log in failed, did you <a href=\"forgot.php\">forget your password</a>?");
	}
}

include_once("include/footer.inc");
closedb();
?>
