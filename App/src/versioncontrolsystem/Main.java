package versioncontrolsystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
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
            System.out.println(function + "\nArgs: " + Arrays.toString(arguments));
            try {
                if (function.equals("changeDir")) {
                    vcs = new VersionControlSystem(arguments[0]);
                } else if (function.equals("init")) {
                    vcs = VersionControlSystem.init(arguments[0]);
                } else if (vcs != null) {
                    switch (function) {
                        case "add" -> vcs.add(arguments[0]);
                        case "commit" ->
                                vcs.commit(arguments[0], arguments[1], readArgs(arguments[2]), readArgs(arguments[2]));
                        case "remove" -> vcs.remove(arguments[0]);
                        case "log" -> System.out.println(vcs.log());
                        case "globalLog" -> System.out.println(vcs.globalLog());
                        case "status" -> System.out.println(vcs.status());
                        case "checkout" -> {
                            if (arguments[1].startsWith("boolean")) {
                                vcs.checkout(arguments[0], arguments[1].equals("booleanTrue"));
                            } else {
                                vcs.checkout(arguments[0], arguments[1]);
                            }
                        }
                        case "branch" -> vcs.branch(arguments[0]);
                        case "removeBranch" -> vcs.removeBranch(arguments[0]);
                        case "reset" -> vcs.reset(arguments[0]);
                    }
                }
            } catch (FailCaseException e) {
                System.out.println(e.getMessage());
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
}