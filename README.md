Mercury
=======

Mercury is a client/server tool for secure password recovery. It has 2 main components:

Android app
-----------
The Android app generates a private/public key pair and transmits the public key to a server. When password recovery is required, the server displays an encrypted version of the password, encoded as a QR-code. The Android app scans the code, decrypts the password, and displays it on screen.

Server
------
The server stores the users credentials as usual, along with the user's public key. When password recovery is requested, the password is encrypted with the user's public key, and encoded as a QR-code. 

Related publications
--------------------
Mohammad Mannan, David Barrera, Carson Brown, David Lie, P.C. van Oorschot. [Mercury: Recovering Forgotten Passwords Using Personal Devices](https://www.ccsl.carleton.ca/~dbarrera/files/fc11-mannan.pdf), In Proceedings of the 15th International Conference on Financial Cryptography and Data Security (FC), Springer, 2011.
