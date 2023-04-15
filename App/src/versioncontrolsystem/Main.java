package versioncontrolsystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Jar created");
            VersionControlSystem vcs = VersionControlSystem.init("C:\\Users\\malic\\Downloads\\Test");
            // Read input from stdin
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input;
            while ((input = reader.readLine()) != null) {
                // Process input from Electron
                System.out.println("Received input from Electron: " + input);

                // Send response to Electron
                String response = vcs.getLastCommit().toString();
                System.out.println("Sending response to Electron: " + response);
                System.out.println(response);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}