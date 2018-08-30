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

public class PrivateEtcGeneratorOld {

    private static final File profiles = new File("profiles");
    private static final File profilesNew = new File("profiles-new");

    public static void main(String[] args) {
        File[] allProfiles = profiles.listFiles();
        for (File profile : allProfiles) {
            fix(profile);
        }
    }

    private static void fix(File profile) {
        String[] profileSplit = profile.toString().split("/");
        String profileName = profileSplit[profileSplit.length - 1].replaceAll(".profile", "").toLowerCase();
        File profileNew = new File(profilesNew.getPath() + "/" + profileSplit[profileSplit.length - 1]);

        System.out.println("\tProcessing " + profileName);

        try {
            boolean hasNetworking = true;
            boolean hasSound = true;
            boolean hasGui = true;
            boolean has3d = true;
            boolean hasDbus = true;
            boolean hasGroups = true;
            boolean hasAllusers = false;

            boolean hasSpecialIgnore = false;
            boolean hasSpecialTor = false;
            boolean hasSpecialJava = false;
            boolean hasSpecialMono = false;
            boolean hasSpecialSword = false;
            boolean hasSpecialGimp = false;
            boolean hasSpecialSelf = false;

            boolean isGtk = true;
            boolean isQt = true;
            boolean isKde = false;

            int hadPrivateEtc = 0;

            ArrayList<String> rebuiltProfile = new ArrayList<>();
            Scanner profileReader = new Scanner(profile);
            while (profileReader.hasNext()) {
                String line = profileReader.nextLine();
                String lineLower = line.toLowerCase();
                rebuiltProfile.add(line);

                if (line.startsWith("#private-etc")) {
                    hadPrivateEtc = 1;
                }
                if (line.startsWith("private-etc")) {
                    hadPrivateEtc = 2;
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
                    hasAllusers = true;
                }
                if (line.contains("private-") && line.contains("tor") || profileName.contains("onionshare")) {
                    hasSpecialTor = true;
                }
                if (line.contains("private-") && line.contains("java") || line.equals("noblacklist ${PATH}/java") || line.equals("noblacklist ${HOME}/.java")) {
                    hasSpecialJava = true;
                }
                if (line.contains("private-") && line.contains("mono") || line.equals("noblacklist ${HOME}/.config/mono") || profileName.contains("pdfmod")) {
                    hasSpecialMono = true;
                }
                if (line.contains("private-etc") && line.contains("sword") || line.equals("noblacklist ${HOME}/.sword")) {
                    hasSpecialSword = true;
                }
                if (line.contains("private-") && lineLower.contains("gimp") || line.startsWith("noblacklist") && lineLower.contains("gimp")) {
                    hasSpecialGimp = true;
                }
                if (line.contains("private-etc") && (profileName.length() >= 3 && line.contains(profileName))) {
                    hasSpecialSelf = true;
                }
                if (lineLower.contains("redirect")) {
                    hasSpecialIgnore = true;
                }
            }
            profileReader.close();
            System.out.println("\t\tRead profile");

            String generatedEtc = generateEtc(profileName, isGtk, isQt, isKde, hasNetworking, hasSound, hasGui, has3d, hasDbus, hasGroups, hasAllusers) + getSpecificExtras(profileName);
            if (generatedEtc.length() > 0 && !hasSpecialIgnore) {
                if (hasSpecialTor) {
                    generatedEtc += ",tor";
                }
                if (hasSpecialJava) {
                    generatedEtc += ",java*";
                }
                if (hasSpecialMono) {
                    generatedEtc += ",mono";
                }
                if (hasSpecialSword) {
                    generatedEtc += ",sword*";
                }
                if (hasSpecialGimp) {
                    generatedEtc += ",gimp";
                }
                if (hasSpecialSelf) {
                    generatedEtc += "," + profileName;
                }

                //WORKAROUND NO PRIVATE-ETC GLOBBING
                generatedEtc = generatedEtc.replace("sword*", "sword,sword.conf");
                generatedEtc = generatedEtc.replace("java*", "java.conf,java-10-openjdk,java-9-openjdk,java-8-openjdk,java-7-openjdk");

                generatedEtc = shouldEnable(hadPrivateEtc, profileName) + generatedEtc;

                boolean addedNewEtc = false;
                PrintWriter profileOut = new PrintWriter(profileNew, "UTF-8");
                String lastLine = "";
                for (String newLine : rebuiltProfile) {
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

    private static String generateEtc(String program, boolean isGtk, boolean isQt, boolean isKde, boolean hasNetworking, boolean hasSound, boolean hasGui, boolean has3d, boolean hasDbus, boolean hasGroups, boolean hasAllusers) {
        String privateEtc = "private-etc ";
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
        etcContents.add("selinux");

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
        if (hasAllusers) {
            etcContents.add("passwd");
        }

        privateEtc += generateEtctSD(etcContents);

        //WORKAROUND NO PRIVATE-ETC GLOBBING
        privateEtc = privateEtc.replace("ld.so.*", "ld.so.cache,ld.so.preload,ld.so.conf,ld.so.conf.d");
        privateEtc = privateEtc.replace("locale*", "locale,locale.alias,locale.conf");
        privateEtc = privateEtc.replace("magic*", "magic,magic.mgc");
        privateEtc = privateEtc.replace("host*", "hosts,host.conf,hostname");
        privateEtc = privateEtc.replace("gtk*", "gtk-2.0,gtk-3.0");
        privateEtc = privateEtc.replace("kde*rc", "kde4rc,kde5rc");

        return privateEtc;
    }

    private static String generateEtctSD(Set<String> contents) {//Sort and delimit
        String sorted = "";

        ArrayList<String> contentSorted = new ArrayList<String>();
        contentSorted.addAll(contents);
        Collections.sort(contentSorted);

        for (String content : contentSorted) {
            sorted += "," + content;
        }
        sorted = sorted.substring(1, sorted.length()); //Remove first comma

        return sorted;
    }

    private static String getSpecificExtras(String profile) {
        String extras = "";
        switch (profile) {
            case "dnsmasq":
                extras = ",dnsmasq.conf,dnsmasq.conf.d";
                break;
            case "ardour5":
                extras = ",ardour4";
                break;
            case "ark":
                extras = ",smb.conf,samba";
                break;
            case "pitivi":
                extras = ",matplotlibrc";
                break;
            case "gnome-clocks":
            case "gnome-maps":
                extras = ",geoclue";
                break;
            case "sqlitebrowser":
                extras = ",machine-id";
                break;
            case "pybitmessage":
                extras = ",PyBitMessage,PyBitMessage.conf";
                break;
            case "steam":
            case "terasology":
                extras = ",lsb-release";
                break;
        }
        return extras;
    }

    private static final List<String> profilesTested = Arrays.asList("atril", "audacity", "bleachbit", "darktable", "eom", "gimp", "gnome-2048", "gnome-chess"
        , "gucharmap", "inkscape", "liferea", "lollypop", "mate-calc", "mate-color-select", "meld", "minetest", "onionshare", "parole", "picard", "pluma"
        , "scribus", "libreoffice", "simple-scan", "soundconverter", "torbrowser-launcher", "transmission-gtk", "xonotic", "wget", "youtube-dl", "pdfmod"
        , "pitivi", "baobab", "electrum", "epiphany", "evince", "gedit", "gitg", "gnome-calculator", "gnome-clocks", "gnome-contacts", "gnome-font-viewer"
        , "gnome-maps", "gnome-photos", "hexchat", "idea.sh", "mumble", "totem", "wireshark", "sqlitebrowser", "android-studio", "apktool", "arch-audit"
        , "arm", "nyx", "dex2jar", "dino", "jd-gui", "obs", "remmina", "pithos", "ppsspp", "shellcheck", "sdat2img", "virtualbox", "zaproxy", "steam");
    //Broken: gnome-logs

    private static String shouldEnable(int hadPrivateEtc, String profileName) {
        if (hadPrivateEtc == 2 || profilesTested.contains(profileName)) {
            return "";
        }
        return "#";
    }
}
