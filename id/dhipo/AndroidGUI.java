// Copyright (C) 2019 Dhipo Alam <dhipo.alam@outlook.com>
// 
// This file is part of AndroidGUI.
// 
// AndroidGUI is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// AndroidGUI is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with AndroidGUI.  If not, see <http://www.gnu.org/licenses/>.

package id.dhipo;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import id.dhipo.TablePackagesModel.PackageInfo;
import id.dhipo.sdkbridge.AndroidSDK;
import id.dhipo.sdkbridge.SDKAction;

class AndroidGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTable tblPackages = new JTable();
    private TablePackagesModel tmdPackages = new TablePackagesModel();
    private JTextField txtPath = new JTextField();
    private JButton btnPath = new JButton("Browse…");
    private JButton btnApply = new JButton("Apply Changes");
    private JButton btnUpdate = new JButton("");
    private JProgressBar prgInstall = new JProgressBar();
    private JScrollPane scrollPane = new JScrollPane(tblPackages);
    private SpringLayout layout = new SpringLayout();
    private Container contentPane = getContentPane();
    private JLabel lblStatus = new JLabel("Idle");
    private String[] updates = {};
    private AndroidSDK sdk;
    public static final String CMD_PATH = "PATH";
    public static final String CMD_APPLY = "APPLY";
    public static final String CMD_UPDATE = "UPDATE";
    private Consumer<SDKAction> sdkListener = (action) -> {
        switch (action.getAction()) {
        case SDKAction.ACTION_INSTALLED:
            SwingUtilities.invokeLater(() -> {
                TablePackagesModel.PackageInfo info = this.tmdPackages.getData().getOrDefault(action.getPayload(),
                        new TablePackagesModel.PackageInfo());
                info.setName(action.getExtra()[1]);
                info.setVersionInstalled(action.getExtra()[0]);
                this.tmdPackages.updatePackage(action.getPayload(), info);
            });
            break;
        case SDKAction.ACTION_AVAILABLE:
            SwingUtilities.invokeLater(() -> {
                TablePackagesModel.PackageInfo info = this.tmdPackages.getData().getOrDefault(action.getPayload(),
                        new TablePackagesModel.PackageInfo());
                info.setName(action.getExtra()[1]);
                info.setVersionAvailable(action.getExtra()[0]);
                this.tmdPackages.updatePackage(action.getPayload(), info);
            });
            break;
        case SDKAction.ACTION_DONE:
            SwingUtilities.invokeLater(() -> {
                prgInstall.setValue(0);
                lblStatus.setText("Idle");
                scrollPane.revalidate();
                // TODO: Automatically set column width to match longest cell content.
                this.tblPackages.getColumnModel().getColumn(0).setMaxWidth(32);
                this.tblPackages.getColumnModel().getColumn(2).setPreferredWidth(160);
                this.tblPackages.getColumnModel().getColumn(2).setMaxWidth(160);
                updates = tmdPackages.getUpdates();
                this.pack();
                uiLock(false);
                btnUpdate.setText((updates.length > 0) ? "Install " + updates.length + " Update(s)" : "No Updates");
                btnUpdate.setEnabled((updates.length > 0));
            });
            break;
        case SDKAction.ACTION_PROGRESS:
            SwingUtilities.invokeLater(() -> {
                String payload = action.getPayload();
                prgInstall.setValue(Integer.parseInt(payload));
                lblStatus.setText(payload.toString() + "%");
            });
            break;
        case SDKAction.ACTION_STATUS:
        case SDKAction.ACTION_OTHER:
            SwingUtilities.invokeLater(() -> {
                String payload = action.getPayload();
                if (!(payload.contains("|") || payload.trim().isEmpty())) {
                    lblStatus.setText(payload);
                }
            });
            break;
        default:
            break;
        }
    };

    public AndroidGUI() {
        initComponents();
    }

    public static void main(String[] args) {
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
        } catch (Exception e) {

        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String osname = System.getProperty("os.name");
                    if (osname.contains("Linux")) {
                        UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                    } else {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }
                    // TODO: This is a "hacky" way to set a "proper" table height. May be revised in
                    // the next release.
                    int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
                    if (dpi > 96) {
                        UIManager.put("Table.rowHeight", 32);
                    }

                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                }
                AndroidGUI gui = new AndroidGUI();
                gui.setVisible(true);
                gui.tmdPackages.clear();
                gui.setSdkPath(System.getenv("ANDROID_HOME"));
            }
        });
    }

    public void refreshPackages() {
        if (SwingUtilities.isEventDispatchThread()) {
            tmdPackages.clear();
            Thread procSdk = new Thread(() -> {
                sdk.getPackageList();
            });
            procSdk.start();
        } else {
            System.out.println("DBG: AndroidGUI.refreshPackages called outside event dispatch thread");
        }
    }

    public void setStatus(String status) {
        if (SwingUtilities.isEventDispatchThread()) {
            lblStatus.setText(status);
        } else {
            System.out.println("DBG: AndroidGUI.setStatus called outside event dispatch thread");
        }
    }

    public void setSdkPath(String sdkPath) {
        if (SwingUtilities.isEventDispatchThread()) {
            sdk = new AndroidSDK(sdkPath, sdkListener);
            if (sdkPath != txtPath.getText()) {
                txtPath.setText(sdkPath);
            }
            uiLock(true);
            refreshPackages();
        } else {
            System.out.println("DBG: AndroidGUI.setSdkPath called outside event dispatch thread");
        }
    }

    public void setProgress(int progress) {
        if (SwingUtilities.isEventDispatchThread()) {
            prgInstall.setValue(progress);
        } else {
            System.out.println("DBG: AndroidGUI.setProgress called outside event dispatch thread");
        }

    }

    public void uiLock(boolean lock) {
        if (SwingUtilities.isEventDispatchThread()) {
            btnApply.setEnabled(!lock);
            btnUpdate.setEnabled(!lock);
            btnPath.setEnabled(!lock);
            tblPackages.setEnabled(!lock);
        } else {
            System.out.println("DBG: AndroidGUI.uiLock called outside event dispatch thread");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Thread procSdk;
        JLabel lblConfirm;
        JPanel pnlConfirm;
        JTable tblChanges;
        TableModel tmdChanges;
        JScrollPane scrChanges;
        int answer;
        switch (e.getActionCommand()) {
        case CMD_APPLY:
            String[] removes = tmdPackages.getMark(TablePackagesModel.PackageState.MARK_REMOVE);
            String[] installs = tmdPackages.getMark(TablePackagesModel.PackageState.MARK_INSTALL);
            if (removes.length + installs.length == 0) {
                JOptionPane.showMessageDialog(this, "No changes need to be applied", "No Change",
                        JOptionPane.INFORMATION_MESSAGE);
                break;
            }
            lblConfirm = new JLabel("These changes will be applied:");
            pnlConfirm = new JPanel(new BorderLayout());
            tmdChanges = new AbstractTableModel() {
                private static final long serialVersionUID = 1L;
                String[] headers = { "Action", "Package", "Version" };

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    String key = rowIndex > removes.length - 1 ? installs[rowIndex - removes.length]
                            : removes[rowIndex];
                    PackageInfo info = tmdPackages.getData().get(key);
                    String name = info.getName();
                    String version = rowIndex > removes.length - 1 ? info.getVersionAvailable()
                            : info.getVersionInstalled();
                    String val = "";
                    switch (columnIndex) {
                    case 0:
                        val = rowIndex > removes.length - 1 ? "Install" : "Remove";
                        break;
                    case 1:
                        val = name;
                        break;
                    default:
                        val = version;
                    }
                    return val;
                }

                @Override
                public int getRowCount() {
                    return removes.length + installs.length;
                }

                @Override
                public int getColumnCount() {
                    return 3;
                }

                @Override
                public String getColumnName(int column) {
                    return headers[column];
                }
            };
            tblChanges = new JTable(tmdChanges);
            tblChanges.getColumnModel().getColumn(0).setPreferredWidth(90);
            tblChanges.getColumnModel().getColumn(0).setMaxWidth(90);
            scrChanges = new JScrollPane(tblChanges);
            scrChanges.setPreferredSize(new Dimension(480, 160));
            pnlConfirm.add(scrChanges, BorderLayout.CENTER);
            pnlConfirm.add(lblConfirm, BorderLayout.NORTH);
            pnlConfirm.setVisible(true);
            answer = JOptionPane.showConfirmDialog(this, pnlConfirm, "Apply Changes", JOptionPane.OK_CANCEL_OPTION);
            if (answer == JOptionPane.OK_OPTION) {
                uiLock(true);
                procSdk = new Thread(() -> {
                    sdk.removePackages(removes);
                    sdk.installPackages(installs);
                    SwingUtilities.invokeLater(() -> {
                        refreshPackages();
                    });
                });
                procSdk.start();
            }
            break;
        case CMD_PATH:
            JFileChooser fileDialog = new JFileChooser();
            fileDialog.setCurrentDirectory(new File(txtPath.getText()));
            fileDialog.setDialogTitle("Select Android SDK location...");
            fileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileDialog.setAcceptAllFileFilterUsed(false);
            fileDialog.setFileFilter(new FileFilter() {
                @Override
                public String getDescription() {
                    return "Directory";
                }

                @Override
                public boolean accept(File f) {
                    return true;
                }
            });
            if (fileDialog.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
                setSdkPath(fileDialog.getSelectedFile().getAbsolutePath());
            }
            break;
        case CMD_UPDATE:
            lblConfirm = new JLabel("These packages will be updated:");
            pnlConfirm = new JPanel(new BorderLayout());
            tmdChanges = new AbstractTableModel() {
                private static final long serialVersionUID = 1L;
                String[] headers = { "Package", "Installed", "Available" };

                @Override
                public int getRowCount() {
                    return updates.length;
                }

                @Override
                public int getColumnCount() {
                    return 3;
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    String key = updates[rowIndex];
                    PackageInfo info = tmdPackages.getData().get(key);
                    switch (columnIndex) {
                    case 0:
                        return info.getName();
                    case 1:
                        return info.getVersionInstalled();
                    case 2:
                        return info.getVersionAvailable();
                    default:
                        return "";
                    }
                }

                @Override
                public String getColumnName(int column) {
                    return headers[column];
                }

            };
            tblChanges = new JTable(tmdChanges);
            tblChanges.getColumnModel().getColumn(1).setPreferredWidth(90);
            tblChanges.getColumnModel().getColumn(1).setMaxWidth(120);
            tblChanges.getColumnModel().getColumn(2).setPreferredWidth(90);
            tblChanges.getColumnModel().getColumn(2).setMaxWidth(120);
            scrChanges = new JScrollPane(tblChanges);
            scrChanges.setPreferredSize(new Dimension(480, 160));
            pnlConfirm.add(scrChanges, BorderLayout.CENTER);
            pnlConfirm.add(lblConfirm, BorderLayout.NORTH);
            pnlConfirm.setVisible(true);
            answer = JOptionPane.showConfirmDialog(this, pnlConfirm, "Update Packages", JOptionPane.OK_CANCEL_OPTION);
            if (answer == JOptionPane.OK_OPTION) {
                uiLock(true);
                procSdk = new Thread(() -> {
                    sdk.installPackages(updates);
                    SwingUtilities.invokeLater(() -> {
                        refreshPackages();
                    });
                });
                procSdk.start();
            }
            break;
        default:
            break;
        }
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("AndroidGUI v0.1beta");
        tblPackages.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        tblPackages.setModel(tmdPackages);
        tblPackages.setShowVerticalLines(false);
        tblPackages.setPreferredScrollableViewportSize(new Dimension(200, 70));
        tblPackages.setFillsViewportHeight(true);
        tblPackages.setAutoCreateRowSorter(true);
        JLabel lblPath = new JLabel("SDK Path: ");
        txtPath.addFocusListener(new FocusListener() {
            private String originalContent;

            @Override
            public void focusLost(FocusEvent e) {
                if (!originalContent.equals(txtPath.getText())) {
                    setSdkPath(txtPath.getText());
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                originalContent = txtPath.getText();
            }
        });
        btnPath.setActionCommand(CMD_PATH);
        btnPath.addActionListener(this);
        btnApply.setActionCommand(CMD_APPLY);
        btnApply.addActionListener(this);
        btnUpdate.setActionCommand(CMD_UPDATE);
        btnUpdate.addActionListener(this);
        prgInstall.setMinimum(0);
        prgInstall.setMaximum(100);
        contentPane.setLayout(layout);
        contentPane.add(lblPath);
        contentPane.add(txtPath);
        contentPane.add(btnPath);
        contentPane.add(scrollPane);
        contentPane.add(lblStatus);
        contentPane.add(prgInstall);
        contentPane.add(btnApply);
        contentPane.add(btnUpdate);
        // First Row (lblPath, txtPath, btnPath)
        layout.putConstraint(SpringLayout.WEST, lblPath, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, lblPath, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.SOUTH, lblPath, 0, SpringLayout.SOUTH, txtPath);
        layout.putConstraint(SpringLayout.WEST, txtPath, 2, SpringLayout.EAST, lblPath);
        layout.putConstraint(SpringLayout.EAST, txtPath, -4, SpringLayout.WEST, btnPath);
        layout.putConstraint(SpringLayout.NORTH, txtPath, 0, SpringLayout.NORTH, btnPath);
        layout.putConstraint(SpringLayout.SOUTH, txtPath, 0, SpringLayout.SOUTH, btnPath);
        layout.putConstraint(SpringLayout.EAST, btnPath, -5, SpringLayout.EAST, contentPane);
        // -- Common anchor for first row is btnPath
        layout.putConstraint(SpringLayout.NORTH, btnPath, 5, SpringLayout.NORTH, contentPane);
        // Second Row (scrollPane)
        layout.putConstraint(SpringLayout.WEST, scrollPane, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.EAST, scrollPane, -5, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, scrollPane, 5, SpringLayout.SOUTH, lblPath);
        layout.putConstraint(SpringLayout.SOUTH, scrollPane, -4, SpringLayout.NORTH, btnApply);
        // Third Row (prgInstall, btnApply)
        layout.putConstraint(SpringLayout.EAST, prgInstall, -4, SpringLayout.WEST, btnUpdate);
        layout.putConstraint(SpringLayout.WEST, prgInstall, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, prgInstall, 0, SpringLayout.NORTH, btnApply);
        layout.putConstraint(SpringLayout.SOUTH, prgInstall, 0, SpringLayout.SOUTH, btnApply);
        layout.putConstraint(SpringLayout.EAST, btnUpdate, -4, SpringLayout.WEST, btnApply);
        layout.putConstraint(SpringLayout.NORTH, btnUpdate, 0, SpringLayout.NORTH, btnApply);
        layout.putConstraint(SpringLayout.SOUTH, btnUpdate, 0, SpringLayout.SOUTH, btnApply);
        layout.putConstraint(SpringLayout.WEST, lblStatus, 4, SpringLayout.WEST, lblPath);
        layout.putConstraint(SpringLayout.EAST, lblStatus, 0, SpringLayout.EAST, prgInstall);
        layout.putConstraint(SpringLayout.NORTH, lblStatus, 0, SpringLayout.NORTH, prgInstall);
        layout.putConstraint(SpringLayout.SOUTH, lblStatus, 0, SpringLayout.SOUTH, prgInstall);
        layout.putConstraint(SpringLayout.SOUTH, btnApply, -5, SpringLayout.SOUTH, contentPane);
        // -- Common anchor for first row is btnApply
        layout.putConstraint(SpringLayout.EAST, btnApply, -5, SpringLayout.EAST, contentPane);
        setMinimumSize(new Dimension(800, 560));
        doLayout();
        pack();
        setLocationRelativeTo(null);
    }

}