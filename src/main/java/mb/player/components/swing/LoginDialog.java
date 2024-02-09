package mb.player.components.swing;

import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

public class LoginDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private JTextField path, user;
    private JPasswordField pass;
    private String[] input;
    
    public LoginDialog(Frame frame) {
        super(frame, true);
        setLocationRelativeTo(frame);
        setSize(400, 180);
        setTitle("Login");
        createAndLayoutComponents();
    }
    
    private void createAndLayoutComponents() {
        setLayout(new MigLayout("wrap", "[][grow]", "[][][][]"));
        
        add(new JLabel("Remote path"));
        add(path = new JTextField(), "grow x");
        add(new JLabel("Username (optional)"));
        add(user = new JTextField(), "grow x");
        add(new JLabel("Password (optional)"));
        add(pass = new JPasswordField(), "grow x");
        
        JPanel buttonsPanel = new JPanel(
                new MigLayout("insets 0, align right", "[][]", "[]"));
        add(buttonsPanel, "grow x, spanx 2");
        
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            if(!path.getText().isBlank()) {
                input = new String[] {path.getText(), user.getText(), String.valueOf(pass.getPassword())};
                closeDialog();
            }
        });
        buttonsPanel.add(ok);
        
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> closeDialog());
        buttonsPanel.add(cancel);
    }
    
    private void closeDialog() {
        setVisible(false);
        dispose();
    }
    
    public String[] getInput() {
        return input;
    }
    
    public static String[] showDialog(Frame frame) {
        LoginDialog dialog = new LoginDialog(frame);
        dialog.setVisible(true);
        dialog.dispose();
        return dialog.getInput();
    }

}
