<?php
$PageTitle = "QRPass Registration";
include_once("include/db.php");
include_once("include/header.inc");
opendb();
function print_form($message="Please register below. Note that this system is merely the counterpart to the proof of concept Android application for <i>How to Live with 'I Forgot My Password'</i>, a HotSec submission.") {
	$name = $_POST["username"];
	print "<form method=\"post\" action=\"register.php\">
	<p class=\"message\">$message</p>
	<label>User Name<span class=\"small\">[A-Za-z0-9] no spaces</span></label>
	<input type=\"text\" name=\"username\" id=\"username\" value=\"$name\" />
	<label>Password<span class=\"small\">Anything you like ;-)</span></label>
	<input type=\"password\" name=\"password\" id=\"password\" />
	<label>Verify Password<span class=\"small\">Needs to match</span></label>
	<input type=\"password\" name=\"password2\" id=\"password2\" />
	<button type=\"submit\" value=\"Submit\" name=\"submit\">Submit</button>
	</form>
	<div class=\"spacer\"></div>";
}

$username = htmlspecialchars($_POST["username"]);
$password = htmlspecialchars($_POST["password"]);
$password2 = htmlspecialchars($_POST["password2"]);
$bad_username = false;
if (!preg_match('/^[A-Za-z0-9]+$/', $_POST["username"])) {
	$bad_username = true;
}
echo "<h1>$PageTitle</h1>";
if (!isset($_POST["submit"])) {
	print_form();
echo $PHP_SELF;
} else {
	if ($password == $password2 && $username != ""  && $password != "" && !$bad_username) {
		$random = rand(0, 2147483647);
		$result = mysql_query("INSERT INTO qrpass.users (username, password, random) VALUES ('$username', '$password', '$random')");
		if ($result) {
			echo "<p>Registration successful, ".$username."!</p>";
			//pagePath() gives only the path of this PHP file, and does NOT include "filename.php"
			$send_key_url = urlencode(pagePath()."send_key.php");
?>
			<p>Next, let's submit your public key using the QRPass application on your 
			phone. This is necessary to recover a forgotten password. Open it up and select <b>Send public key</b> from the menu. Scan the
			following barcode:</p>
			<img src="http://chart.apis.google.com/chart?cht=qr&chs=350x350&chl=<?php echo $send_key_url;?>%3Fusername%3D<?php echo $username;?>%26random%3D<?php echo $random;?>" />
			<p>Then, you can <a href="login.php">log in</a>.</p>
<?php
		} else {
			//mysql query was unsuccessful
			print_form("Registration failure. Try again with a different username.");
		}
	} else {
		//passwords didn't match, or username was blank
		if ($bad_username || $username == "") {
			print_form("Registration failure, username was invalid. Try again.");
		} elseif ($password == "") {
			print_form("Registration failure, passwords cannot be blank. Try again.");
		}else {
			print_form("Registration failure, passwords did not match. Try again.");
		}
	}
}

include_once("include/footer.inc");
closedb();
?>
