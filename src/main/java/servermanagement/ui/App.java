package main.java.servermanagement.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;

public class App {
  JFrame frmServerMgmt;
  JSplitPane splitPane;

  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          App window = new App();
          window.frmServerMgmt.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public App() {
    initialize();
  }

  private void initialize() {
    frmServerMgmt = new JFrame();
    frmServerMgmt.setResizable(false);
    frmServerMgmt.setTitle("Amazon Server Management");
    frmServerMgmt.setBounds(100, 100, 600, 600);
    frmServerMgmt.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    splitPane = new JSplitPane() {
      private static final long serialVersionUID = 1L;
      private final int location = 530;

      {
        setDividerLocation(location);
      }

      @Override
      public int getDividerLocation() {
        return location;
      }

      @Override
      public int getLastDividerLocation() {
        return location;
      }
    };

    splitPane.setResizeWeight(1.0);
    splitPane.setBorder(new LineBorder(new Color(0, 0, 0)));
    splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    frmServerMgmt.getContentPane().add(splitPane, BorderLayout.CENTER);

    JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    splitPane.setLeftComponent(tabbedPane);

    JPanel pnlSignIn = new JPanel();
    pnlSignIn.setLayout(null);
    tabbedPane.addTab("Sign In", pnlSignIn);

    JPanel pnlEC2Mgmt = new JPanel();
    pnlEC2Mgmt.setLayout(null);
    tabbedPane.addTab("EC2 Management", pnlEC2Mgmt);

    JPanel pnlRDSMgmt = new JPanel();
    pnlRDSMgmt.setLayout(null);
    tabbedPane.addTab("RDS Management", pnlRDSMgmt);

  }
}
