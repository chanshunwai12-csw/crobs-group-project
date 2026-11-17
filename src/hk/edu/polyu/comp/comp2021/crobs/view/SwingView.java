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

    private final JFrame frame = new JFrame("Crobs Room Booking");
    private final JComboBox<String> commands = new JComboBox<>(SYNTAX);
    private final JPanel fields = new JPanel(new GridLayout(0, 2, 8, 5));
    private final List<JTextField> inputs = new ArrayList<>();
    private final JTextArea output = new JTextArea(16, 58);
    private final JLabel session = new JLabel();
    private final StringWriter response = new StringWriter();
    private final CROBS crobs;
    private final CommandLogger logger;
    private final CrobsController controller;

    private SwingView(CrobsContext context, CROBS crobs) throws IOException {
        this.crobs = crobs;
        logger = new CommandLogger(context.getHtmlLogPath(), context.getTxtLogPath());
        CLIView resultView = new CLIView(new PrintWriter(response, true));
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
            new SwingView(context, crobs).frame.setVisible(true);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "Session error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                run("quit");
            }
        });

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel header = new JPanel(new GridLayout(0, 1, 4, 4));
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
        frame.setContentPane(content);
        commands.addActionListener(event -> updateFields());
        updateFields();
        updateSession();
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
    }

    private void updateFields() {
        fields.removeAll();
        inputs.clear();
        String[] parameters = ((String) commands.getSelectedItem()).split(" ");
        for (int index = 1; index < parameters.length; index++) {
            JLabel label = new JLabel(parameters[index]);
            JTextField input = new JTextField();
            label.setLabelFor(input);
            if ("date".equals(parameters[index]) || "weekStartDate".equals(parameters[index])) {
                input.setToolTipText("YYYY-MM-DD");
            } else if (parameters[index].endsWith("Time")) {
                input.setToolTipText("HH:mm");
            }
            fields.add(label);
            fields.add(input);
            inputs.add(input);
        }
        fields.revalidate();
        fields.repaint();
    }

    private void submit() {
        String[] syntax = ((String) commands.getSelectedItem()).split(" ");
        StringBuilder line = new StringBuilder(syntax[0]);
        for (int index = 0; index < inputs.size(); index++) {
            String value = inputs.get(index).getText().trim();
            String parameter = syntax[index + 1];
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

    private void updateSession() {
        User user = crobs.getCurrentUser();
        session.setText(user == null ? "Signed out" : "Signed in as " + user.getId() + " (" + user.getRole() + ")");
    }
}
