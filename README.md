# AndroidGUI

## A simple Android SDK Graphical User Interface

Current state of features availability:

- [x] Windows & Linux compatibility
- [x] Install packages
- [x] Remove packages
- [x] Update packages
- [ ] License manager/viewer
- [ ] Coffee maker
- [ ] Bread toaster
- [ ] Fill any feature you wish here...

This project exists thanks to a certain company who loves striping features from their products.

This program is a straightly ugly and hacky replacement for Android SDK Manager GUI. This program just parse the output of sdkmanager commands and display it in the nicest way it could do. Don't expect much from it and don't depend much to it.

It would be much appreciated if somebody who have access to sdklib-\*.jar source code willing to share them with me, so I could refactor this program to access sdklib directly rather than parsing arbitrary output of a mundane program that could change any moment.

Please fork as you like. This code is licensed under GNU GPL v3.

## Running the code

To run the code, simply do the following

```bash
git clone https://github.com/devilxnux/AndroidGUI.git
cd AndroidGUI
javac id/dhipo/*.java id/dhipo/sdkbridge/*.java
java id/dhipo/AndroidGUI
```
Additionally, you can build JAR file by issuing

```bash
jar cvfme AndroidGUI.jar MANIFEST.MF id.dhipo.AndroidGUI id/dhipo/*.class id/dhipo/sdkbridge/*.class
# Run generated JAR
java -jar AndroidGUI.jar
```

Sincerely yours,
Dhipo Alam
