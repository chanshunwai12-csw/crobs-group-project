package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CommandResult;

import java.util.Scanner;

/** Command-line entry point for the Crobs application. */
public final class Application {
    private Application() {
        // Utility class: do not instantiate.
    }

    public static void main(String[] args) {
        CROBS crobs = new CROBS();
        System.out.println("Crobs started.");
        System.out.println("Superadmin user ID: " + crobs.getSuperadminId());
        System.out.println("Supported commands: addUser, removeUser, changeRole, login, logout, quit");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String command = scanner.nextLine().trim();
                if ("quit".equals(command)) {
                    System.out.println("Crobs terminated.");
                    break;
                }

                CommandResult result = crobs.execute(command);
                System.out.println(result.getMessage());
            }
        }
    }
}
