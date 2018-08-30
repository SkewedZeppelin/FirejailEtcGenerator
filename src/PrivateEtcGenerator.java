/*
  This file is part of FirejailEtcGenerator.

  FirejailEtcGenerator is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 2 of the License, or
  (at your option) any later version.

  FirejailEtcGenerator is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with FirejailEtcGenerator.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

public class PrivateEtcGenerator {

    private static File profiles;
    private static File profilesNew;
    private static File gathered;

    private static final List<String> gatheredIgnore = Arrays.asList("apparmor.d", "apport", "logrotate", "skel", "apache", "init.d", "bash_completion.d"
        , "profile.d", "logrotate.d", "cron.weekly");
    private static final List<String> profilesTested = Arrays.asList("atril", "audacity", "bleachbit", "darktable", "eom", "gimp", "gnome-2048", "gnome-chess"
        , "gucharmap", "inkscape", "liferea", "lollypop", "mate-calc", "mate-color-select", "meld", "minetest", "onionshare", "parole", "picard", "pluma"
        , "scribus", "libreoffice", "simple-scan", "soundconverter", "torbrowser-launcher", "transmission-gtk", "xonotic", "wget", "youtube-dl", "pdfmod"
        , "pitivi", "baobab", "electrum", "epiphany", "evince", "gedit", "gitg", "gnome-calculator", "gnome-clocks", "gnome-contacts", "gnome-font-viewer"
        , "gnome-maps", "gnome-photos", "hexchat", "idea.sh", "mumble", "totem", "wireshark", "sqlitebrowser", "android-studio", "apktool", "arch-audit"
        , "arm", "nyx", "dex2jar", "dino", "jd-gui", "obs", "remmina", "pithos", "ppsspp", "shellcheck", "sdat2img", "virtualbox", "zaproxy", "steam");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please supply an absolute directory containing: profiles, profiles-new, gathered");
            System.exit(1);
        }
        if(!args[0].endsWith("/")) {
            args[0] += "/";
        }
        profiles = new File(args[0] + "profiles/");
        profilesNew = new File(args[0] + "profiles-new/");
        gathered = new File(args[0] + "gathered/");
        if(!profiles.exists() || !profilesNew.exists() || !gathered.exists()) {
            System.out.println("Required directories do not exist!");
            System.out.println("Please create and populate: profiles, profiles-new, gathered");
            System.exit(1);
        }

        for (File profile : profiles.listFiles()) {
            addEtc(profile);
        }
    }

    private static void addEtc(File profile) {
        String[] profileSplit = profile.toString().split("/");
        String profileName = profileSplit[profileSplit.length - 1].replaceAll(".profile", "").toLowerCase();
        File profileNew = new File(profilesNew.getPath() + "/" + profileSplit[profileSplit.length - 1]);

        System.out.println("\tProcessing " + profileName);
        try {
            int hadPrivateEtc = 0;
            boolean isRedirect = false;

            boolean hasNetworking = true;
            boolean hasSound = true;
            boolean hasGui = true;
            boolean has3d = true;
            boolean hasDbus = true;
            boolean hasGroups = true;
            boolean hasAllUsers = false;

            boolean isGtk = true;
            boolean isQt = true;
            boolean isKde = false;

            String specialExtras = "";

            ArrayList<String> profileContents = new ArrayList<String>();
            Scanner profileReader = new Scanner(profile);
            while (profileReader.hasNextLine()) {
                String line = profileReader.nextLine();
                String lineLower = line.toLowerCase();
                profileContents.add(line);

                if (line.startsWith("#private-etc")) {
                    hadPrivateEtc = 1;
                }
                if (line.startsWith("private-etc")) {
                    hadPrivateEtc = 2;
                }
                if (lineLower.contains("redirect")) {
                    isRedirect = true;
                }

                if ((line.startsWith("# Description:") && (lineLower.contains("gnome") || lineLower.contains("gtk"))) || profileName.startsWith("gnome-") || profileName.endsWith("-gtk")) {
                    isGtk = true;
                    isKde = false;
                    isQt = false;
                }

                if ((line.startsWith("# Description:") && lineLower.contains("qt")) || profileName.endsWith("-qt")) {
                    isGtk = false;
                    isQt = true;
                    isKde = false;
                }

                if ((line.startsWith("# Description:") && lineLower.contains("kde")) || (line.contains("private-") && line.contains("kde")) || (line.contains("noblacklist") && lineLower.contains("/.kde"))) {
                    isGtk = false;
                    isQt = true;
                    isKde = true;
                }

                if (line.equals("nosound")) {
                    hasSound = false;
                }
                if (line.equals("net none") || line.equals("protocol unix")) {
                    hasNetworking = false;
                }
                if (line.equals("quiet") /*|| line.equals("noblacklist /sbin")*/ || line.equals("private") || line.equals("blacklist /tmp/.X11-unix") || line.equals("x11 none")) {
                    hasGui = false;
                }
                if (profileName.equals("arm") || profileName.equals("nyx")) {
                    hasGui = false;
                }
                if (profileName.equals("gucharmap") || profileName.equals("gnome-calculator")) {
                    hasGui = true;
                }
                if (line.equals("no3d")) {
                    has3d = false;
                }
                if (line.equals("nodbus")) {
                    hasDbus = false;
                }
                if (line.equals("nogroups")) {
                    hasGroups = false;
                }
                if (line.equals("allusers")) {
                    hasAllUsers = true;
                }
                if (line.contains("private-") && (line.contains("tor")) || profileName.contains("onionshare")) { //TODO: IMPROVE ME!
                    specialExtras += "hasSpecialTor;";
                }
                if (line.contains("private-") && line.contains("java") || line.equals("noblacklist ${PATH}/java") || line.equals("noblacklist ${HOME}/.java")) {
                    specialExtras += "hasSpecialJava;";
                }
                if (line.contains("private-") && line.contains("mono") || line.equals("noblacklist ${HOME}/.config/mono") || profileName.contains("pdfmod")) {
                    specialExtras += "hasSpecialMono;";
                }
                if (line.contains("private-etc") && line.contains("sword") || line.equals("noblacklist ${HOME}/.sword")) {
                    specialExtras += "hasSpecialSword;";
                }
                if (line.contains("private-") && lineLower.contains("gimp") || line.startsWith("noblacklist") && lineLower.contains("gimp")) {
                    specialExtras += "hasSpecialGimp;";
                }
                if (line.contains("private-etc") && (profileName.length() >= 3 && line.contains(profileName))) {
                    specialExtras += "hasSpecialSelf;";
                }
            }
            profileReader.close();
            System.out.println("\t\tRead profile");

            Set<String> etcContents = new HashSet<>();
            etcContents.addAll(getEtcBase());
            etcContents.addAll(getEtcByProfileOptions(hasNetworking, hasSound, hasGui, has3d, hasDbus, hasGroups, hasAllUsers));
            if(hasGui) {
                etcContents.addAll(getEtcByProfileOptionsGui(isGtk, isQt, isKde));
            }
            etcContents.addAll(getEtcBySpecial(specialExtras, profileName));
            etcContents.addAll(getEtcBySpecific(profileName));
            etcContents.addAll(getEtcByGathered(profileName));

            String generatedEtc = shouldEnable(hadPrivateEtc, profileName) + "private-etc " + removeGlobs(delimitArray(etcContents));

            if (generatedEtc.length() > 0 && !isRedirect) {
                boolean addedNewEtc = false;
                PrintWriter profileOut = new PrintWriter(profileNew, "UTF-8");
                String lastLine = "";
                for (String newLine : profileContents) {
                    if ((newLine.contains("private-etc") || lastLine.contains("private-dev") || newLine.contains("private-tmp") || newLine.contains("noexec") || newLine.contains("read-only"))) {
                        if (!addedNewEtc) {
                            profileOut.println(generatedEtc);
                            addedNewEtc = true;
                        }
                        if (!newLine.contains("private-etc")) {
                            profileOut.println(newLine);
                        }
                    } else {
                        profileOut.println(newLine);
                    }
                    lastLine = newLine;
                }
                String prefix = "\n";
                if (lastLine.contains("disable") || lastLine.contains("private")) {
                    prefix = "";
                }
                if (!addedNewEtc) {
                    profileOut.println(prefix + generatedEtc);
                }
                profileOut.close();
                System.out.println("\t\tWrote profile");

            } else {
                System.out.println("\t\tIgnoring");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String shouldEnable(int hadPrivateEtc, String profileName) {
        if (hadPrivateEtc == 2 || profilesTested.contains(profileName)) {
            return "";
        }
        return "#";
    }

    private static String removeGlobs(String input) {
        String output = input;
        output = output.replace("gtk*", "gtk-2.0,gtk-3.0");
        output = output.replace("host*", "hosts,host.conf,hostname");
        output = output.replace("java*", "java.conf,java-10-openjdk,java-9-openjdk,java-8-openjdk,java-7-openjdk");
        output = output.replace("kde*rc", "kde4rc,kde5rc");
        output = output.replace("ld.so.*", "ld.so.cache,ld.so.preload,ld.so.conf,ld.so.conf.d");
        output = output.replace("locale*", "locale,locale.alias,locale.conf");
        output = output.replace("magic*", "magic,magic.mgc");
        output = output.replace("sword*", "sword,sword.conf");
        return output;
    }

    private static String delimitArray(Set<String> contents) {
        String result = "";

        ArrayList<String> contentSorted = new ArrayList<String>();
        contentSorted.addAll(contents);
        Collections.sort(contentSorted);

        for (String content : contentSorted) {
            result += "," + content;
        }
        result = result.substring(1, result.length()); //Remove first comma

        return result;
    }

    private static Set<String> getEtcBase() {
        Set<String> etcContents = new HashSet<>();

        etcContents.add("ld.so.*");
        etcContents.add("locale*");
        etcContents.add("localtime");
        //etcContents.add("magic*");
        etcContents.add("alternatives");
        etcContents.add("mime.types");
        etcContents.add("xdg");

        //etcContents.add("os-release");
        //etcContents.add("lsb-release");

        etcContents.add("passwd");
        //etcContents.add("security");
        //etcContents.add("system-fips");
        //etcContents.add("selinux");

        return etcContents;
    }

    private static Set<String> getEtcByProfileOptions(boolean hasNetworking, boolean hasSound, boolean hasGui, boolean has3d, boolean hasDbus, boolean hasGroups, boolean hasAllUsers) {
        Set<String> etcContents = new HashSet<>();
        //TODO Handle the following: mtab, smb.conf, samba, cups, adobe, mailcap

        if (hasNetworking) {
            etcContents.add("ca-certificates");
            etcContents.add("ssl");
            etcContents.add("pki");
            etcContents.add("crypto-policies");
            etcContents.add("nsswitch.conf");
            etcContents.add("resolv.conf");
            etcContents.add("host*");
            etcContents.add("protocols");
            etcContents.add("services");
            etcContents.add("rpc");
            //etcContents.add("gai.conf");
            //etcContents.add("proxychains.conf");
        }
        if (hasSound) {
            etcContents.add("alsa");
            etcContents.add("asound.conf");
            etcContents.add("pulse");
            etcContents.add("machine-id");
        }
        if (hasGui) {
            etcContents.add("fonts");
            etcContents.add("pango");
            etcContents.add("X11");
        }
        if (has3d) {
            etcContents.add("drirc");
            etcContents.add("glvnd");
            etcContents.add("bumblebee");
            //etcContents.add("nvidia");
        }
        if (hasDbus) {
            etcContents.add("dbus-1");
            etcContents.add("machine-id");
        }
        if (hasGroups) {
            etcContents.add("group");
        }
        if (hasAllUsers) {
            etcContents.add("passwd");
        }
        return etcContents;
    }

    private static Set<String> getEtcByProfileOptionsGui(boolean isGtk, boolean isQt, boolean isKde) {
        Set<String> etcContents = new HashSet<>();
        if (isGtk) {
            etcContents.add("dconf");
            etcContents.add("gconf");
            etcContents.add("gtk*");
        }
        if (isQt) {
            etcContents.add("Trolltech.conf");
            //etcContents.add("sni-qt.conf");
        }
        if (isKde) {
            etcContents.add("kde*rc");
        }
        return etcContents;
    }

    private static Set<String> getEtcBySpecial(String special, String profileName) {
        Set<String> etcContents = new HashSet<>();
        if (special.contains("hasSpecialTor;")) {
            etcContents.add("tor");
        }
        if (special.contains("hasSpecialJava;")) {
            etcContents.add("java*");
        }
        if (special.contains("hasSpecialMono;")) {
            etcContents.add("mono");
        }
        if (special.contains("hasSpecialSword;")) {
            etcContents.add("sword*");
        }
        if (special.contains("hasSpecialGimp;")) {
            etcContents.add("gimp");
        }
        if (special.contains("hasSpecialSelf;")) {
            etcContents.add(profileName);
        }
        return etcContents;
    }

    private static Set<String> getEtcBySpecific(String profileName) {
        Set<String> etcContents = new HashSet<>();
        switch (profileName) {
            case "dnsmasq":
                etcContents.add("dnsmasq.conf");
                etcContents.add("dnsmasq.conf.d");
                break;
            case "ardour5":
                etcContents.add("ardour4");
                break;
            case "ark":
                etcContents.add("smb.conf");
                etcContents.add("samba");
                break;
            case "pitivi":
                etcContents.add("matplotlibrc");
                break;
            case "gnome-clocks":
            case "gnome-maps":
                etcContents.add("geoclue");
                break;
            case "sqlitebrowser":
                etcContents.add("machine-id");
                break;
            case "pybitmessage":
                etcContents.add("PyBitMessage");
                etcContents.add("PyBitMessage.conf");
                break;
            case "remmina":
            case "steam":
            case "terasology":
                etcContents.add("lsb-release");
                break;
        }
        return etcContents;
    }

    private static Set<String> getEtcByGathered(String profileName) {
        Set<String> etcContents = readDirectoryIntoSet(new File(gathered + "/" + profileName));
        etcContents.removeAll(gatheredIgnore);
        return etcContents;
    }

    private static Set<String> readDirectoryIntoSet(File directory) {
        Set<String> directoryContents = new HashSet<>();
        try {
            if(directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if(files != null && files.length > 0)
                for (File file : files) {
                    if (file.isFile()) {
                        directoryContents.addAll(readFileIntoSet(file));
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return directoryContents;
    }

    private static Set<String> readFileIntoSet(File file) {
        Set<String> fileContents = new HashSet<>();
        try {
            if(file.exists() && file.isFile()) {
                Scanner reader = new Scanner(file);
                while(reader.hasNextLine()) {
                    fileContents.add(reader.nextLine());
                }
                reader.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return fileContents;
    }

}
