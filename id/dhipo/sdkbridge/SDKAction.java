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

package id.dhipo.sdkbridge;

public class SDKAction {

    public static final String ACTION_PROGRESS = "PROGRESS";
    public static final String ACTION_INSTALLED = "INSTALLED";
    public static final String ACTION_AVAILABLE = "AVAILABLE";
    public static final String ACTION_STATUS = "STATUS";
    public static final String ACTION_ERROR = "ERROR";
    public static final String ACTION_DONE = "DONE";
    public static final String ACTION_OTHER = "OTHER";
    private String action, payload;
    private String[] extra;

    public SDKAction(String action, String payload){
        this.action = action;
        this.payload = payload;
    }

    public SDKAction(String action, String payload, String[] extra){
        this.action = action;
        this.payload = payload;
        this.extra = extra;
    }

    public String[] getExtra(){
        return this.extra;
    }

    public void setExtra(String[] extra){
        this.extra = extra;
    }

    public String getAction(){
        return this.action;
    }

    public String getPayload(){
        return this.payload;
    }
}