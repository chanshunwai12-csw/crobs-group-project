package hk.edu.polyu.comp.comp2021.crobs.view;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies GUI command formatting without requiring a graphical desktop. */
class CommandFormTest {
    /** Verifies all three feature forms encode blank input using the CLI convention. */
    @Test
    void blankFeatureInputsUseTheSharedCliEmptyListSyntax() {
        assertEquals("addRoom meetingRoom 12 \"\"", SwingView.formatCommand(
                "addRoom roomType capacity features", Arrays.asList("meetingRoom", "12", " ")));
        assertEquals("modifyRoom R001 features \"\"", SwingView.formatCommand(
                "modifyRoom roomID field newValue", Arrays.asList("R001", "features", "")));
        assertEquals("search 2027-01-01 09:00 10:00 0 \"\"", SwingView.formatCommand(
                "search date startTime endTime minCapacity features",
                Arrays.asList("2027-01-01", "09:00", "10:00", "0", "")));
    }

    /** Verifies blank required fields, extra fields, and invalid feature spacing are rejected. */
    @Test
    void requiredValuesAndMalformedFieldsAreStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> SwingView.formatCommand(
                "modifyRoom roomID field newValue", Arrays.asList("R001", "capacity", "")));
        assertThrows(IllegalArgumentException.class, () -> SwingView.formatCommand(
                "login userID", Arrays.asList("")));
        assertThrows(IllegalArgumentException.class, () -> SwingView.formatCommand(
                "addRoom roomType capacity features", Arrays.asList("meetingRoom", "12", "two words")));
        assertThrows(IllegalArgumentException.class, () -> SwingView.formatCommand(
                "login userID", Arrays.asList("U0001", "extra")));
    }

    /** Verifies formatting preserves valid names, multiword purposes, and feature lists. */
    @Test
    void namesPurposesAndNonemptyFeaturesArePreserved() {
        assertEquals("addUser Alice Chan", SwingView.formatCommand("addUser name", Arrays.asList(" Alice Chan ")));
        assertEquals("book R001 2027-01-01 09:00 10:00 Team meeting", SwingView.formatCommand(
                "book roomID date startTime endTime purpose",
                Arrays.asList("R001", "2027-01-01", "09:00", "10:00", "Team meeting")));
        assertEquals("addRoom computerLab 20 screen,computers", SwingView.formatCommand(
                "addRoom roomType capacity features", Arrays.asList("computerLab", "20", "screen,computers")));
    }
}
