JUKEFOX SOURCE CODE
===================

jukefox was originally developed as part of a research project at the Swiss Federal Institute of Technology (ETH Zurich). The project investigates various ways to derive music similarity information and aims at making this information available to the end user. It was released under the GNU Public License (GPLv3) in October 2013.

The project consists of 6 parts:
--------------------------------
JukefoxAndroid: The Android client of jukefox (depends on JukefoxModel)
JukefoxModel: The platform-independent core functionality of jukefox
JukefoxModelPC: The UI independent part of the desktop version (depends on JukefoxModel)
JukefoxPlayerCLI: jukefox for desktop PCs with a command line interface (depends on JukefoxModel, JukefoxModelPC, and MiniPlayer)
MiniPlayer: Music Player for CLI version
UnitTests: Some few test for the core functionality (depends on JukefoxModel and JukefoxModelPC)

How to compile:
---------------

Import all parts as eclipse projects. The projects should compile out of the box.
For JukefoxAndroid to compile you need a working Android development environment.

License:
--------
If not mentioned differently, the files in the jukefox project are under the GNU Public Licence (GPLv3).

Have Fun!
