package versioncontrolsystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Main {
    private static void updateStatus(VersionControlSystem vcs) throws Exception {
        Set<String>[] status = vcs.updateStatus();
        System.out.println("Staged" + sendList(status[0]));
        Thread.sleep(7);
        System.out.println("Unstaged" + sendList(status[1]));
        Thread.sleep(7);
    }
    public static void main(String[] args) throws Exception {
        // Read input from stdin
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        String[] arguments;
        String function;
        String[] firstPass;
        VersionControlSystem vcs = null;
        while ((input = reader.readLine()) != null) {  // input is assigned to the message from electron
            // Decodes input
            firstPass = decode(input);
            function = firstPass[0];
            arguments = decode(firstPass[1]);
            // Send response to Electron
            try {
                if (function.equals("changeDir")) {
                    vcs = new VersionControlSystem(arguments[0]);
                } else if (function.equals("init")) {
                    System.out.println(arguments[0]);
                    vcs = VersionControlSystem.init(arguments[0]);
                } else if (vcs != null) {
                    switch (function) {
                        case "add" -> {
                            vcs.otherAdd(arguments[0]);
                            updateStatus(vcs);
                        }
                        case "commit" -> {
                            vcs.commit(arguments[0], arguments[1], decode(arguments[2]), decode(arguments[3]));
                            updateStatus(vcs);
                        }
                        case "remove" -> {
                            vcs.unstage(arguments[0]);
                            updateStatus(vcs);
                        }
                        case "log" -> System.out.println(vcs.log());
                        case "globalLog" -> System.out.println(vcs.globalLog());
                        case "checkout" -> {
                            if (arguments[1].startsWith("boolean")) {
                                vcs.checkout(arguments[0], arguments[1].equals("booleanTrue"));
                            } else {
                                vcs.checkout(arguments[0], arguments[1]);
                            }
                            updateStatus(vcs);
                        }
                        case "branch" -> vcs.branch(arguments[0]);
                        case "removeBranch" -> vcs.removeBranch(arguments[0]);
                        case "reset" -> {
                            vcs.reset(arguments[0]);
                            updateStatus(vcs);
                        }
                        case "updateStatus" -> {
                            try {
                                updateStatus(vcs);
                            }
                            catch (Exception e) {
                                continue;
                            }
                        }
                        default -> {
                            continue;
                        }
                    }
                }
            } catch (FailCaseException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    /**
     * Decodes input
     * @param input: string
     * @return: string
     */
    public static String[] decode(String input) {
        List<String> list = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            int j = i;
            while (input.charAt(j) != '#') j++;

            int length = Integer.valueOf(input.substring(i, j));
            i = j + 1 + length;
            list.add(input.substring(j + 1, i).trim());
        }
        return list.toArray(new String[0]);
    }

    /**
     * Encodes input
     * @param input: string
     * @return string
     */
    private static String sendList(Iterable<String> input) {
        StringBuilder encodedString = new StringBuilder();
        for (String str : input) {
            encodedString.append(str.length()).append("#").append(str);
        }
        return encodedString.toString();
    }
}