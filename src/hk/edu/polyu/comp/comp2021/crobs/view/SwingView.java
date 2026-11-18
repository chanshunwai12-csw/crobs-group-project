package hk.edu.polyu.comp.comp2021.crobs.view;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsController;
import hk.edu.polyu.comp.comp2021.crobs.controller.ProjectCommands;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.CommandLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.CrobsContext;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/** Graphical command forms backed by the same controller as the CLI. */
public final class SwingView {
    /** Default gap between larger window sections. */
    private static final int SECTION_GAP = 8;
    /** Gap between labels in a command form. */
    private static final int FIELD_GAP = 5;
    /** Smaller gap between controls in the header. */
    private static final int HEADER_GAP = 4;
    /** Empty border around the window content. */
    private static final int WINDOW_PADDING = 12;
    /** Initial number of visible output rows. */
    private static final int OUTPUT_ROWS = 16;
    /** Initial number of visible output columns. */
    private static final int OUTPUT_COLUMNS = 58;
    /**
     * Commands and parameter labels presented by the graphical form.
     */
    private static final String[] SYNTAX = {
        "login userID", "logout", "addUser name", "removeUser targetUserID",
        "changeRole targetUserID newRole", "addRoom roomType capacity features",
        "modifyRoom roomID field newValue", "deleteRoom roomID", "listRoom roomID",
        "listAllRooms", "search date startTime endTime minCapacity features",
        "book roomID date startTime endTime purpose",
        "modifyBooking bookingID roomID date startTime endTime purpose",
        "deleteBooking bookingID", "listBookings", "schedule roomID weekStartDate",
        "undo", "redo", "quit"
    };

    /**
     * Top-level session window owned by this view.
     */
    private final JFrame frame = new JFrame("Crobs Room Booking");
    /**
     * Selector for command names and their parameter labels.
     */
    private final JComboBox<String> commands = new JComboBox<>(SYNTAX);
    /**
     * Container rebuilt with the selected command parameters.
     */
    private final JPanel fields = new JPanel(new GridLayout(0, 2, SECTION_GAP, FIELD_GAP));
    /**
     * Input controls in command argument order.
     */
    private final List<JTextField> inputs = new ArrayList<>();
    /**
     * Scrollable transcript of commands, results and errors.
     */
    private final JTextArea output = new JTextArea(OUTPUT_ROWS, OUTPUT_COLUMNS);
    /** Replaceable schedule display, kept separate from the command transcript. */
    private final JTextArea scheduleOutput = new JTextArea(OUTPUT_ROWS / 2, OUTPUT_COLUMNS);
    /**
     * Current login ID and role, or a signed-out message.
     */
    private final JLabel session = new JLabel();
    /**
     * Buffer receiving output from the shared command controller.
     */
    private final StringWriter response = new StringWriter();
    /**
     * Shared application model used by the graphical session.
     */
    private final CROBS crobs;
    /**
     * HTML and text command logs for this session.
     */
    private final CommandLogger logger;
    /**
     * Shared validation and command dispatch for both interfaces.
     */
    private final CrobsController controller;

    /**
     * Connects a graphical window to the shared model and command logs.
     * @param context session data and log destinations
     * @param crobs the shared model
     * @throws IOException if either command log cannot be opened
     */
    private SwingView(CrobsContext context, CROBS crobs) throws IOException {
        this.crobs = crobs;
        logger = new CommandLogger(context.getHtmlLogPath(), context.getTxtLogPath());
        CLIView resultView = new CLIView(new PrintWriter(response, true), scheduleOutput::setText);
        ProjectCommands projectCommands = new ProjectCommands(crobs, resultView);
        controller = new CrobsController(context, resultView, projectCommands);
        buildWindow();
    }

    /**
     * Opens a graphical session on the Swing event dispatch thread.
     * @param context shared booking and logging context
     * @param crobs shared application model
     */
    public static void open(CrobsContext context, CROBS crobs) {
        try {
            new SwingView(context, crobs).show();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "Session error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Displays the initialized session window. */
    private void show() {
        frame.setVisible(true);
    }

    /**
     * Builds the command form, transcript and close action on the event dispatch thread.
     */
    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                run("quit");
            }
        });

        JPanel content = new JPanel(new BorderLayout(SECTION_GAP, SECTION_GAP));
        content.setBorder(BorderFactory.createEmptyBorder(WINDOW_PADDING, WINDOW_PADDING, WINDOW_PADDING, WINDOW_PADDING));
        JPanel form = new JPanel(new BorderLayout(SECTION_GAP, SECTION_GAP));
        JPanel header = new JPanel(new GridLayout(0, 1, HEADER_GAP, HEADER_GAP));
        header.add(session);
        JLabel commandLabel = new JLabel("Command");
        commandLabel.setLabelFor(commands);
        header.add(commandLabel);
        header.add(commands);
        form.add(header, BorderLayout.NORTH);
        form.add(fields, BorderLayout.CENTER);
        JButton execute = new JButton("Run command");
        execute.addActionListener(event -> submit());
        form.add(execute, BorderLayout.SOUTH);
        frame.getRootPane().setDefaultButton(execute);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setText("Superadmin user ID: " + crobs.getSuperadminId() + "\n");
        output.getAccessibleContext().setAccessibleName("Command output");
        content.add(form, BorderLayout.NORTH);
        content.add(new JScrollPane(output), BorderLayout.CENTER);
        scheduleOutput.setEditable(false);
        scheduleOutput.getAccessibleContext().setAccessibleName("Current schedule");
        JScrollPane schedulePane = new JScrollPane(scheduleOutput);
        schedulePane.setBorder(BorderFactory.createTitledBorder("Current schedule"));
        content.add(schedulePane, BorderLayout.SOUTH);
        frame.setContentPane(content);
        commands.addActionListener(event -> updateFields());
        updateFields();
        updateSession();
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
    }

    /**
     * Rebuilds labeled input controls for the selected command.
     */
    private void updateFields() {
        fields.removeAll();
        inputs.clear();
        String selected = (String) commands.getSelectedItem();
        if (selected == null) {
            fields.revalidate();
            fields.repaint();
            return;
        }
        String[] parameters = selected.split(" ");
        for (int index = 1; index < parameters.length; index++) {
            JLabel label = new JLabel(parameters[index]);
            JTextField input = new JTextField();
            label.setLabelFor(input);
            if ("date".equals(parameters[index]) || "weekStartDate".equals(parameters[index])) {
                input.setToolTipText("YYYY-MM-DD");
            } else if (parameters[index].endsWith("Time")) {
                input.setToolTipText("HH:mm");
            }
            if ("features".equals(parameters[index]) || "newValue".equals(parameters[index])) {
                input.setToolTipText("For features, leave blank for an empty list.");
            }
            fields.add(label);
            fields.add(input);
            inputs.add(input);
        }
        fields.revalidate();
        fields.repaint();
    }

    /**
     * Validates form values and submits a command to the shared controller.
     */
    private void submit() {
        String selected = (String) commands.getSelectedItem();
        if (selected == null) {
            return;
        }
        String[] syntax = selected.split(" ");
        StringBuilder line = new StringBuilder(syntax[0]);
        for (int index = 0; index < inputs.size(); index++) {
            String value = inputs.get(index).getText().trim();
            String parameter = syntax[index + 1];
            boolean features = "features".equals(parameter)
                    || ("modifyRoom".equals(syntax[0]) && "newValue".equals(parameter)
                    && "features".equals(inputs.get(1).getText().trim()));
            if (features && value.isEmpty()) {
                value = "\"\"";
            }
            if (value.isEmpty() || (!"name".equals(parameter) && !"purpose".equals(parameter)
                    && value.matches(".*\\s.*"))) {
                JOptionPane.showMessageDialog(frame, "Enter a valid " + parameter + ".");
                inputs.get(index).requestFocusInWindow();
                return;
            }
            line.append(' ').append(value);
        }
        run(line.toString());
    }

    /**
     * Executes a graphical command and closes the window after a successful quit.
     * @param line the complete command line to log and execute
     */
    private void run(String line) {
        output.append("> " + line + "\n");
        try {
            boolean keepOpen = controller.executeLine(line, logger);
            output.append(response.toString());
            response.getBuffer().setLength(0);
            output.setCaretPosition(output.getDocument().getLength());
            updateSession();
            if (!keepOpen) {
                try {
                    logger.close();
                } finally {
                    frame.dispose();
                }
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(frame, exception.getMessage(), "Log error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Refreshes the displayed login after data or authentication changes.
     */
    private void updateSession() {
        User user = crobs.getCurrentUser();
        session.setText(user == null ? "Signed out" : "Signed in as " + user.getId() + " (" + user.getRole() + ")");
    }
}
