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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class EtcGatherer {

    private static String outputDirectory = "/tmp/EtcGatherer/";

    public static void main(String[] args) {
        outputDirectory = System.getProperty("user.home") + "/EtcGatherer/";
        System.out.println("Outputting results to " + outputDirectory);

        System.out.println("Using the following methods:");
        boolean hasStrings = new File("/usr/bin/strings").exists();
        System.out.println("\tstrings: " + hasStrings);
        boolean hasRPM = new File("/usr/bin/rpm").exists();
        System.out.println("\trpm: " + hasRPM);
        boolean hasPacman = new File("/usr/bin/pacman").exists();
        System.out.println("\tpacman: " + hasPacman);
        boolean hasAptFile = new File("/usr/bin/apt-file").exists();
        System.out.println("\tapt-file: " + hasAptFile);
        boolean hasDpkg = new File("/var/lib/dpkg/info").exists();
        System.out.println("\tdpkg: " + hasDpkg);

        Set<String> programs = new HashSet<>();

        File programsList = new File(outputDirectory + "programs.list");
        try {
            if (programsList.exists()) {
                Scanner programReader = new Scanner(programsList);
                while (programReader.hasNextLine()) {
                    programs.add(programReader.nextLine());
                }
                programReader.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        File firejailProfiles = new File("/etc/firejail/");
        try {
            if(firejailProfiles.exists()) {
                for(File contents : firejailProfiles.listFiles()) {
                    if(contents.getName().endsWith(".profile")) {
                        programs.add(contents.getName().replace(".profile", ""));
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("Processing " + programs.size() + " programs");
        Long time = System.currentTimeMillis();

        for(String program : programs) {
            if(hasStrings) {
                File programPath = new File("/usr/bin/" + program);
                if(programPath.exists()) {
                    ArrayList<String> commandOutput = getOutputFromCommand(new String[] {"/usr/bin/strings", programPath.getAbsolutePath()});
                    Set<String> processedOutput = processOutputToSet(commandOutput, null);
                    writeSetToFile(processedOutput, new File(outputDirectory + program + "/" + "strings." + time + ".list"));
                }
            }
            if(hasAptFile) {
                ArrayList<String> commandOutput = getOutputFromCommand(new String[] {"/usr/bin/apt-file", "list", program});
                Set<String> processedOutput = processOutputToSet(commandOutput, program + ": ");
                writeSetToFile(processedOutput, new File(outputDirectory + program + "/" + "apt." + time + ".list"));
            }
            if(hasRPM) {
                ArrayList<String> commandOutput = getOutputFromCommand(new String[] {"/usr/bin/rpm", "-ql", program});
                Set<String> processedOutput = processOutputToSet(commandOutput, null);
                writeSetToFile(processedOutput, new File(outputDirectory + program + "/" + "rpm." + time + ".list"));
            }
            if(hasPacman) {
                ArrayList<String> commandOutput = getOutputFromCommand(new String[] {"/usr/bin/pacman", "-Ql", program});
                Set<String> processedOutput = processOutputToSet(commandOutput, program + " ");
                writeSetToFile(processedOutput, new File(outputDirectory + program + "/" + "pacman." + time + ".list"));
            }
            if(hasDpkg) {
                File conffiles = new File("/var/lib/dpkg/info/" + program + ".conffiles");
                if(conffiles.exists()) {
                    ArrayList<String> commandOutput = getOutputFromCommand(new String[] {"/usr/bin/cat", conffiles.getAbsolutePath()});
                    Set<String> processedOutput = processOutputToSet(commandOutput, null);
                    writeSetToFile(processedOutput, new File(outputDirectory + program + "/" + "dpkg." + time + ".list"));
                }
            }
        }
    }

    private static ArrayList<String> getOutputFromCommand(String[] command) {
        ArrayList<String> output = new ArrayList<String>();
        try {
            Process process = Runtime.getRuntime().exec(command);
            Scanner processOutput = new Scanner(process.getInputStream());
            while(processOutput.hasNextLine()) {
                output.add(processOutput.nextLine());
            }
            processOutput.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    private static Set<String> processOutputToSet(ArrayList<String> output, String filter) {
        Set<String> results = new HashSet<>();
        for(String line : output) {
            if(filter != null && filter.length() > 0) {
                line = line.replace(filter, "");
            }
            if(line.startsWith("/etc/")) {
                line = line.replace("/etc/", ""); //Strip prefix
                if(line.contains("/")) {
                    line = line.split("/")[0]; //Get basename
                }
                results.add(line);
            }
        }
        return results;
    }

    private static void writeSetToFile(Set<String> contents, File output) {
        try {
            if(contents.size() > 0) {
                output.getParentFile().mkdirs();
                PrintWriter writer = new PrintWriter(output, "UTF-8");
                for (String line : contents) {
                    if(line.length() > 0) {
                        writer.println(line);
                    }
                }
                writer.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
