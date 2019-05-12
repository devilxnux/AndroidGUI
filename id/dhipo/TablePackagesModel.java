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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

public class TablePackagesModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private String[] headers = { "", "Name", "Version", "Status", "ID" };
    private LinkedHashMap<String, PackageInfo> data = new LinkedHashMap<String, PackageInfo>();

    public enum PackageState {
        UNMARKED, MARK_INSTALL, MARK_REMOVE
    } // Package State

    public static class PackageInfo {
        String name = "";
        String versionAvailable = "";
        String versionInstalled = "";
        String location = "";
        PackageState state;

        PackageInfo() {
        }

        // #region Getters and Setters
        /**
         * @param location the location to set
         */
        public void setLocation(String location) {
            this.location = location;
        }

        /**
         * @return the location
         */
        public String getLocation() {
            return location;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param versionAvailable the versionAvailable to set
         */
        public void setVersionAvailable(String versionAvailable) {
            this.versionAvailable = versionAvailable;
        }

        /**
         * @return the versionAvailable
         */
        public String getVersionAvailable() {
            return versionAvailable;
        }

        /**
         * @param versionInstalled the versionInstalled to set
         */
        public void setVersionInstalled(String versionInstalled) {
            this.versionInstalled = versionInstalled;
        }

        /**
         * @return the versionInstalled
         */
        public String getVersionInstalled() {
            return versionInstalled;
        }

        /**
         * @param state the state to set
         */
        public void setState(PackageState state) {
            this.state = state;
        }

        /**
         * @return the state
         */
        public PackageState getState() {
            return state;
        }
        // #endregion

        public boolean isInstalled() {
            return !versionInstalled.isEmpty();
        }

        public boolean isLatest() {
            return isInstalled() ? versionInstalled.equals(versionAvailable) : true;
        }
    } // Package Info

    public int findIdRow(String id) {
        int i = 0;
        Set<String> keySet = data.keySet();
        for (String key : keySet) {
            if (key.equals(id)) {
                break;
            }
            if (i == keySet.size() - 1) {
                i = -1;
                break;
            }
        }
        return i;
    }

    public PackageInfo updatePackage(String id, PackageInfo info) {
        PackageInfo oldInfo = data.get(id);
        data.put(id, info);
        fireTableRowsUpdated(findIdRow(id) - 1, findIdRow(id) - 1);
        return oldInfo;
    }

    public void clear() {
        data.clear();
        fireTableDataChanged();
    }

    public void setData(LinkedHashMap<String, PackageInfo> data) {
        this.data = data;
        fireTableDataChanged();
    }

    public LinkedHashMap<String, PackageInfo> getData() {
        return this.data;
    }

    public String[] getUpdates() {
        ArrayList<String> updates = new ArrayList<String>();
        for (String key : this.data.keySet()) {
            if (!this.data.get(key).isLatest()) {
                updates.add(key);
            }
        }
        return updates.toArray(new String[0]);
    }

    public String[] getMark(PackageState mark) {
        ArrayList<String> packages = new ArrayList<String>();
        for (String key : this.data.keySet()) {
            if (this.data.get(key).getState() == mark) {
                packages.add(key);
            }
        }
        return packages.toArray(new String[0]);
    }

    // #region Overrides
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return Boolean.class;
        default:
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String key = data.keySet().toArray(new String[0])[rowIndex];
        PackageInfo info = data.get(key);
        switch (columnIndex) {
        case 0:
            return (info.getState() == PackageState.MARK_INSTALL
                    || (info.isInstalled() && info.getState() != PackageState.MARK_REMOVE));
        case 1:
            return info.getName();
        case 2:
            return info.isInstalled() ? info.getVersionInstalled() : info.getVersionAvailable();
        case 3:
            if (info.isInstalled() && info.isLatest()) {
                return "Installed";
            } else if (info.isInstalled() && !info.isLatest()) {
                return "Version " + info.getVersionAvailable() + " available";
            } else {
                return "Not installed";
            }
        case 4:
            return key;
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object val, int row, int column) {
        switch (column) {
        case 0:
            String key = data.keySet().toArray(new String[0])[row];
            PackageInfo info = data.get(key);
            Boolean value = (Boolean) val;
            Boolean installed = info.isInstalled();
            if (value == false && installed) {
                info.setState(PackageState.MARK_REMOVE);
            } else if (value == true && !installed) {
                info.setState(PackageState.MARK_INSTALL);
            } else {
                info.setState(PackageState.UNMARKED);
            }
            break;
        default:
        }
        fireTableCellUpdated(row, column);
    }
    // #endregion Overrides
}