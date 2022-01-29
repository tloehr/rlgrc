/*
 * Created by JFormDesigner on Sat Jan 29 13:35:07 CET 2022
 */

package de.flashheart.rlgrc;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Torsten Löhr
 */
public class FrameMain extends JFrame {
    public FrameMain() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        label1 = new JLabel();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "default, $lcgap, default",
            "default:grow"));

        //---- label1 ----
        label1.setText("text");
        contentPane.add(label1, CC.xy(3, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel label1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
