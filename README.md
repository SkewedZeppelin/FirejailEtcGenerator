General
========
This is a set of two utilities created in order to automatically generate 'private-etc' lines for Firejail profiles.

EtcGatherer
-----------
This utility gathers what files in /etc a program may need using various methods.
The programs output is kept simple to allow for easy merging, deduplication is handled elsewhere.

Currently supported methods are:
- strings $program
- apt-file list $program
- cat /var/lib/dpkg/info/$program.conffiles
- pacman -Ql $program
- rpm -ql $program


PrivateEtcGenerator
-------------------
This utility actually "analyzes" profiles and combines various sources to generate a 'private-etc' line.
It then rewrites the program with the new line in place.

TODO:
- Improve GNOME/GTK/Qt/KDE handling
- Improve GUI determination
- Improve placement in certain cases (missing private-* lines)


PrivateEtcGeneratorOld
----------------------
This is the original program.
It is very messy and doesn't support parsing data from EtcGatherer.
