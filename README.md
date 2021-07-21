# MusicXML-Cleaner
A tool for [NinSheetMusic.org](https://www.NinSheetMusic.org/) arrangers who use MuseScore.

## Description
This program fixes bugs that result from exporting MuseScore sheets to the MusicXML 3.1 format and formats them according www.NinSheetMusic.org formatting guidelines.

This program does the following *(any of the following can be enabled or disabled in the program)*:
* Adds and formats page numbers and mini-titles to pages 2+
* Adds system measure numbers
* Removes duplicate copyright information and corrects the font size of the primary copyright information
* Corrects metronome and tempo markings
* Centers titles and other centered credits
* Offsets systems so that unindented systems align with the left margin
* Makes ending texts like D.C. al Coda bold
* Adds periods to the ends of numbers in ending texts (AKA voltas)
* Adds swing 8th notes wherever there is a direction that says "Swing"
* Replaces uses of the fonts Edwin or FreeSerif with Times New Roman

## Installation
Go to https://github.com/cacabish/MusicXML-Cleaner/releases and download the latest .jar file corresponding to the desired version. Do NOT download the source code. There is no installation required, simply double click to execute.

**Requires Java 8 or higher. Recommended: Java 11 or higher.**
You can download Java [here](https://www.java.com/en/download/).

## Usage
1. Launch the MusicXML Cleaner .jar file. Check or uncheck the boxes corresponding to the desired operations. Then, either drag-and-drop an uncompressed MusicXML document from MuseScore onto the window or click the button and select the file to load (or do File -> Open).

![Load File 1](https://www.dropbox.com/s/4bb8wevcc5ain5j/open_v120.png?raw=1)

![Load File 2](https://www.dropbox.com/s/h9p59if9hj1e1jq/open2.png?raw=1)

2. The program now works its magic! After a few seconds, the program will be prompt you to save. The default is to overwrite the old file, but you can save it as a new file.

![Save File](https://www.dropbox.com/s/nqle29xuh7spprh/save.png?raw=1)

3. That's it! If the save is successful, you should see a window confirming the save.

## License
MIT License

Copyright (c) 2020-21 cacabish

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### A Word from the Developer
This program is not intended to be a strictly formal piece of software, nor follow standard industry protocols. This is intended as a fun, personal project for aiding those who use MuseScore when arranging for [www.NinSheetMusic.org](https://www.NinSheetMusic.org/), which includes myself, whilst keeping it open source for other contributors to further its development as new bugs are discovered and as new versions of MuseScore are released. Thanks! :D