package versioncontrolsystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Main {
    private static void updateStatus(VersionControlSystem vcs) throws Exception {
        Set<String>[] status = vcs.statusHelper();
        if (!status[0].isEmpty()) {
            System.out.println("Branches" + sendList(status[0]));
        }
        if (!status[1].isEmpty()) {
            System.out.println("Staged" + sendList(status[1]));
        }
        if (!status[2].isEmpty()) {
            System.out.println("Modified" + sendList(status[2]));
        }
        if (!status[3].isEmpty()) {
            System.out.println("Untracked" + sendList(status[3]));
        }
        if (!status[4].isEmpty()) {
            System.out.println("Removed" + sendList(status[4]));
        }
    }
    public static void main(String[] args) throws Exception {
        // Read input from stdin
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        String[] arguments;
        String function;
        int first;
        VersionControlSystem vcs = null;
        while ((input = reader.readLine()) != null) {  // input is assigned to the message from electron
            // Separates the input by reading the number, and separating that amount of following characters
            first = input.charAt(0);
            function = input.substring(1, first + 1);
            arguments = readArgs(input.substring(first + 1));
            // Send response to Electron
            try {
                if (function.equals("changeDir")) {
                    vcs = new VersionControlSystem(arguments[0]);
                } else if (function.equals("init")) {
                    vcs = VersionControlSystem.init(arguments[0]);
                } else if (vcs != null) {
                    switch (function) {
                        case "add" -> {
                            vcs.add(arguments[0]);
                            updateStatus(vcs);
                        }
                        case "commit" -> {
                            vcs.commit(arguments[0], arguments[1], readArgs(arguments[2]), readArgs(arguments[2]));
                            updateStatus(vcs);
                        }
                        case "remove" -> {
                            vcs.remove(arguments[0]);
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
                        case "update" -> {
                            updateStatus(vcs);
                        }
                    }
                }
            } catch (FailCaseException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
    private static String[] readArgs(String input) {
        List<String> output = new ArrayList<>();
        int spot = 0;
        int num;
        while (spot < input.length()) {
            num = input.charAt(spot);
            output.add(input.substring(spot + 1, spot + num + 1));
            spot += num + 1;
        }
        return output.toArray(new String[0]);
    }
    private static String sendList(Set<String> input) {
        StringBuilder output = new StringBuilder();
        char c;
        for (String s : input) {
            c = (char) s.length();
            output.append(c).append(s);
        }
        return output.toString();
    }
}